package com.pinewoods.score.tracker.services.tournament;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pinewoods.score.tracker.dao.admin.PlayerRepository;
import com.pinewoods.score.tracker.dao.flight.FlightRepository;
import com.pinewoods.score.tracker.dao.season.SeasonRepository;
import com.pinewoods.score.tracker.dao.season.TeamStandingRepository;
import com.pinewoods.score.tracker.dao.tournament.TournamentRepository;
import com.pinewoods.score.tracker.dto.admin.PlayerDTO;
import com.pinewoods.score.tracker.dto.flight.FlightDTO;
import com.pinewoods.score.tracker.dto.flight.FlightScoreDTO;
import com.pinewoods.score.tracker.dto.scoring.ScoreCardDTO;
import com.pinewoods.score.tracker.dto.tournament.TournamentDTO;
import com.pinewoods.score.tracker.entities.admin.Player;
import com.pinewoods.score.tracker.entities.admin.Team;
import com.pinewoods.score.tracker.entities.flight.Flight;
import com.pinewoods.score.tracker.entities.flight.FlightScore;
import com.pinewoods.score.tracker.entities.season.Season;
import com.pinewoods.score.tracker.entities.season.TeamStanding;
import com.pinewoods.score.tracker.entities.tournament.Tournament;
import com.pinewoods.score.tracker.exceptions.ResourceConflictException;
import com.pinewoods.score.tracker.exceptions.ResourceNotFoundException;
import com.pinewoods.score.tracker.services.scoring.IScoringStrategy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class TournamentService {
    private final TournamentRepository tournamentRepo;
    private final SeasonRepository seasonRepo;
    private final PlayerRepository playerRepo;
    private final FlightRepository flightRepo;
    private final TeamStandingRepository standingRepo;
    private final Map<Long, IScoringStrategy> activeStrategies = new ConcurrentHashMap<>();
    private final Map<Long, List<Flight>> calculatedFlightCache = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    // ==================== Create Tournament ====================
     /**
     * @param name name of the tournament
     * @param seasonName name of the season
     * @param strategy scoring strategy
     * @return Tournament object created
     */
    @PreAuthorize( "hasRole('ADMIN')")
    public Tournament createTournament(String name, String seasonName, IScoringStrategy strategy) {
        Season season = seasonRepo.findByName(seasonName)
                .orElseThrow(() -> new ResourceNotFoundException("Season not found"));

        if (season.isFinished()) {
            throw new ResourceConflictException("Cannot create tournament for finished season");
        }

        if (season.getTournaments().stream().anyMatch(t -> t.getName().equals(name))) {
            throw new ResourceConflictException("Tournament with this name already exists within the season");
        }

        Tournament tournament = Tournament.builder()
                .name(name)
                .season(season)
                .strategyName(strategy.getName()) // Save name for history
                .isFinished(false)
                .build();

        Tournament saved = tournamentRepo.save(tournament);

        // Put the specific strategy instance (with its pars/indexes) into memory
        activeStrategies.put(saved.getId(), strategy);
        return saved;
    }

    /**
     * Adds scorecards to a tournament.
     *
     * @param tournamentId tournament id
     * @param cards scorecards for the flight
     * @return FlightDTO representing the flight
     */
    @PreAuthorize( "hasRole('ADMIN')")
    public Flight addScorecards(Long tournamentId, List<ScoreCardDTO> cards) {
        Tournament tournament = tournamentRepo.findById(tournamentId).orElseThrow();

        IScoringStrategy strategy = getValidStrategy(tournament);

        Flight flight = createFlight(cards, strategy);

        return processAndLinkFlight(tournament, flight, cards, strategy);
    }

    /**
     * Adds an existing flight to a tournament, and updates everything accordingly
     *
     * @param tournamentId tournament id
     * @param flightId flight id
     * @return FlightDTO representing the flight
     */
    @PreAuthorize( "hasRole('ADMIN')")
    public Flight addExistingFlight(Long tournamentId, Long flightId) {
        Tournament tournament = tournamentRepo.findById(tournamentId).orElseThrow();
        Flight flight = flightRepo.findById(flightId).orElseThrow();
        IScoringStrategy strategy = getValidStrategy(tournament);

        // Convert existing entity back to DTOs so the strategy can calculate scores
        List<ScoreCardDTO> scoreCards = flight.getFlightScores().stream()
                .map(fs -> new ScoreCardDTO(fs.getPlayer().toDTO(), fs.getHoleScores()))
                .toList();

        return processAndLinkFlight(tournament, flight, scoreCards, strategy);
    }

    private IScoringStrategy getValidStrategy(Tournament tournament) {
        IScoringStrategy strategy = activeStrategies.get(tournament.getId());
        if (strategy == null) {
            throw new ResourceNotFoundException("Scoring strategy not found for tournament " + tournament.getId());
        }
        return strategy;
    }

    private Flight processAndLinkFlight(Tournament tournament, Flight flight,
                                        List<ScoreCardDTO> cards, IScoringStrategy strategy) {

        // 1. Calculate scores (for the cache/return object)
        Flight calculatedFlight = strategy.calculateScores(cards);

        // 2. Link the Flight to the Tournament
        // JPA will handle the tournament_flights table update automatically
        tournament.getFlights().add(flight);

        // 3. Update Birdies in Standings
        for (FlightScore fs : calculatedFlight.getFlightScores()) {
            Team team = fs.getPlayer().getTeam();
            standingRepo.findBySeasonNameAndTeamName(tournament.getSeason().getName(), team.getName())
                    .ifPresent(standing -> standing.setBirdies(standing.getBirdies() + fs.getBirdies()));
        }

        // 4. Update Cache
        calculatedFlightCache.computeIfAbsent(tournament.getId(), k -> new ArrayList<>())
                .add(calculatedFlight);

        // 5. Persist everything
        tournamentRepo.saveAndFlush(tournament);

        return calculatedFlight;
    }

    /**
     * Completes a tournament and updates values
     *
     * @param tournamentId tournament id
     */
    @PreAuthorize( "hasRole('ADMIN')")
    public void endTournament(Long tournamentId) {
        Tournament tournament = tournamentRepo.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament " + tournamentId + " not found"));

        Map<Long, Integer> pointsMap = new HashMap<>();

        // Calculate the 100, 66, 33 points and specialty awards
        calculateFinalAwards(tournament, pointsMap);

        // Update Team Standings in the Season
        updateTeamStandings(tournament, pointsMap);

        tournament.setFinished(true);
        tournamentRepo.save(tournament);

        // Cleanup: Memory is freed, Strategy is garbage collected
        activeStrategies.remove(tournamentId);
        calculatedFlightCache.remove(tournamentId);
    }

    // ================= Get Tournament ==================
     /**
     * Fetches the tournaments for a given season.
     * @param seasonName name of the season
     * @return List of TournamentDTO representing the tournaments for the season
     */
    public List<TournamentDTO> getTournamentsBySeason(String seasonName) {
        Season season = seasonRepo.findByName(seasonName)
                .orElseThrow(() -> new ResourceNotFoundException("Season " + seasonName + " not found"));

        return tournamentRepo.findBySeasonId(season.getId())
                .stream()
                .map(Tournament::toDTO)
                .toList();
    }

    public List<TournamentDTO> getTournamentsByName(String name) {
        return tournamentRepo.findAllByName(name)
                .stream()
                .map(Tournament::toDTO)
                .toList();
    }

    public Tournament getTournamentBySeasonAndName(String seasonName, String name) {
        Season season = seasonRepo.findByName(seasonName)
                .orElseThrow(() -> new ResourceNotFoundException("Season " + seasonName + " not found"));

        return season.getTournaments().stream()
                .filter(t -> t.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Tournament " + name +
                        " not found in season " + seasonName));
    }

    public List<TournamentDTO> getAllActiveTournaments() {
        // Active means: not finished AND currently has a scoring strategy parked in memory
        return tournamentRepo.findAll().stream()
                .filter(t -> !t.isFinished() && activeStrategies.containsKey(t.getId()))
                .map(Tournament::toDTO)
                .toList();
    }

    // ================ Delete Tournament ======================
    @PreAuthorize( "hasRole('ADMIN')")
    public void deleteTournament(Long tournamentId) {
        Tournament tournament = tournamentRepo.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found"));
        Season season = tournament.getSeason();
        season.getTournaments().removeIf(t -> t.getId() == tournamentId);
        tournamentRepo.deleteById(tournamentId);
        activeStrategies.remove(tournamentId);
    }

    // ============= Utilities =============

    private Flight createFlight(List<ScoreCardDTO> cards, IScoringStrategy strategy) {
        Flight flight = Flight.builder()
                .date(new Date())
                .build();

        for (ScoreCardDTO card : cards) {
            PlayerDTO player = card.player();

            // Perform the handicap/par/index math we discussed
            int totalPoints = card.holeScores().stream().mapToInt(Integer::intValue).sum();
            int birdies = strategy.countBirdies(card.holeScores());

            FlightScore fs = FlightScore.builder()
                    .player(playerRepo.findByName(player.name()).orElseThrow())
                    .score(totalPoints)
                    .holeScores(card.holeScores())
                    .courseName(strategy.getCourseName())
                    .birdies(birdies)
                    .flight(flight) // Set back-reference
                    .build();

            flight.getFlightScores().add(fs);
        }
        return flight;
    }

    private void calculateFinalAwards(Tournament tournament, Map<Long, Integer> pointsMap) {
        List<FlightScore> scoreboard = calculatedFlightCache.get(tournament.getId()).stream()
                .flatMap(f -> f.getFlightScores().stream())
                .sorted(Comparator.comparingInt(FlightScore::getScore).reversed())
                .toList();

        allocatePoints(scoreboard, tournament, pointsMap);
        allocateBirdies(scoreboard, tournament, pointsMap);
    }

    private void allocatePoints(List<FlightScore> scoreboard, Tournament t, Map<Long, Integer> pointsMap) {
        // 1. Group players by score and maintain the sorted order (Highest score first)
        Map<Integer, List<Player>> groups = scoreboard.stream()
                .collect(Collectors.groupingBy(
                        FlightScore::getScore,
                        LinkedHashMap::new,
                        Collectors.mapping(FlightScore::getPlayer, Collectors.toList())
                ));

        List<List<Player>> rankedGroups = new ArrayList<>(groups.values());

        // Define our prize pools
        int[] prizePools = {100, 66, 33};

        // 2. Iterate through the top 3 score groups (Rank 1, 2, and 3)
        for (int i = 0; i < Math.min(rankedGroups.size(), 3); i++) {
            List<Player> playersInRank = rankedGroups.get(i);
            int rankLabel = i + 1; // 1, 2, or 3
            int pointsToDistribute = prizePools[i] / playersInRank.size();

            for (Player p : playersInRank) {
                // Assign the award rank (1, 2, or 3)
                t.getAwards().put(p.getId(), rankLabel);

                // Add the points to the map
                pointsMap.put(p.getId(), pointsMap.getOrDefault(p.getId(), 0) + pointsToDistribute);
            }
        }
    }

    private void allocateBirdies(List<FlightScore> scoreboard, Tournament t, Map<Long, Integer> pointsMap) {
        scoreboard.stream().filter(fs -> fs.getBirdies() > 0)
                .max(Comparator.comparingInt(FlightScore::getBirdies))
                .ifPresent(fs -> pointsMap.put(
                        fs.getPlayer().getId(), pointsMap.getOrDefault(fs.getPlayer().getId(), 0) + 50));
    }

    private void updateTeamStandings(Tournament tournament, Map<Long, Integer> pointsMap) {
        Season season = tournament.getSeason();
        pointsMap.forEach((playerId, points) -> {
            Player player = playerRepo.findById(playerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Player with id " + playerId + " not found"));
            Team team = player.getTeam();
            if (!team.getName().equalsIgnoreCase("UNASSIGNED")) {
                // Find existing standing for this team in this season, or create new
                TeamStanding standing = standingRepo.findBySeasonNameAndTeamName(
                        season.getName(), team.getName())
                        .orElse(TeamStanding.builder().season(season).team(team).points(0).build());

                standing.setPoints(standing.getPoints() + points);

                // update wins
                standing.setWins(standing.getWins() + 1);

                standingRepo.save(standing);
            }
        });
    }

    public List<FlightScoreDTO> getTournamentLeaderBoard(String seasonName, String tournamentName) {
        Tournament tournament = getTournamentBySeasonAndName(seasonName, tournamentName);
        if (tournament.isFinished()) {
            throw new ResourceConflictException("Tournament is already finished");
        }
        List<Flight> calculateFlights = calculatedFlightCache.get(tournament.getId());
        List<FlightScoreDTO> leaderBoard = new ArrayList<>();
        calculateFlights.forEach(f -> {
            f.getFlightScores().forEach(fs -> leaderBoard.add(fs.toDto()));
        });

        return leaderBoard;
    }

    // ============== Import and export ===============

    public record TournamentExport(
            @Schema(description = "Tournament name", example = "02_02_2026_PineWoodsMMR")
            String name,
            @Schema(description = "Awards for each player")
            Map<String, Integer> awards,
            String seasonName,
            String strategyName,
            List<FlightDTO> flights){}

    @PreAuthorize("hasRole('ADMIN')")
    public byte[] exportTournament(Long tournamentId) throws IOException {
        Tournament tournament = tournamentRepo.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found"));

        if (!tournament.isFinished()) {
            throw new ResourceConflictException("Only finished tournaments can be exported.");
        }

        // Convert to DTO
        Map<String, Integer> portableAwards = new HashMap<>();
        tournament.getAwards().forEach((playerId, rank) -> {
            playerRepo.findById(playerId).ifPresent(p -> portableAwards.put(p.getName(), rank));
        });

        TournamentDTO tournamentDto = tournament.toDTO();

        TournamentExport dto = new TournamentExport(tournamentDto.name(),
                portableAwards,
                tournament.getSeason().getName(),
                tournamentDto.strategyName(),
                tournamentDto.flights());

        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(dto);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void importTournament(byte[] jsonData, String targetSeasonName) throws IOException {
        // 1. Use the new Export record to read the data
        TournamentExport exportDto = objectMapper.readValue(jsonData, TournamentExport.class);

        Season season = seasonRepo.findByName(targetSeasonName)
                .orElseThrow(() -> new ResourceNotFoundException("Season not found"));

        Tournament tournament = Tournament.builder()
                .name(exportDto.name())
                .season(season)
                .strategyName(exportDto.strategyName())
                .flights(new ArrayList<>())
                .isFinished(true)
                .build();

        // This will hold our final Awards: NewPlayerID (Long) -> Rank (Integer)
        Map<Long, Integer> newAwardsMap = new HashMap<>();

        // 2. Create the flights
        for (FlightDTO flightDto : exportDto.flights()) {
            Flight flight = Flight.builder()
                    .date(flightDto.date())
                    .build();

            for (FlightScoreDTO scoreDto : flightDto.flights()) {
                // Find player in the CURRENT database by name
                Player player = playerRepo.findByName(scoreDto.playerName())
                        .orElseThrow(() -> new ResourceNotFoundException("Player not found: " + scoreDto.playerName()));

                FlightScore score = FlightScore.builder()
                        .player(player)
                        .score(scoreDto.score())
                        .holeScores(scoreDto.holeScores())
                        .courseName(scoreDto.courseName())
                        .birdies(scoreDto.birdies())
                        .flight(flight)
                        .build();
                flight.getFlightScores().add(score);

                // 3. Map the Award if it exists for this player name
                if (exportDto.awards() != null && exportDto.awards().containsKey(player.getName())) {
                    Integer rank = exportDto.awards().get(player.getName());
                    newAwardsMap.put(player.getId(), rank);
                }
            }
            tournament.getFlights().add(flight);
        }

        // Set the translated awards map
        tournament.setAwards(newAwardsMap);

        // Save cascades to all flights and scores
        tournamentRepo.saveAndFlush(tournament);
    }
}
