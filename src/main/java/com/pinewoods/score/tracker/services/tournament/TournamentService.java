package com.pinewoods.score.tracker.services.tournament;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pinewoods.score.tracker.dao.admin.PlayerRepository;
import com.pinewoods.score.tracker.dao.course.CourseRepository;
import com.pinewoods.score.tracker.dao.season.SeasonRepository;
import com.pinewoods.score.tracker.dao.season.TeamStandingRepository;
import com.pinewoods.score.tracker.dao.tournament.TournamentRepository;
import com.pinewoods.score.tracker.dto.flight.FlightDTO;
import com.pinewoods.score.tracker.dto.flight.FlightScoreDTO;
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
import com.pinewoods.score.tracker.services.course.CourseService;
import com.pinewoods.score.tracker.services.flight.FlightService;
import com.pinewoods.score.tracker.services.scoring.IScoringStrategy;
import io.micrometer.common.KeyValues;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    private final TeamStandingRepository standingRepo;
    private final CourseRepository courseRepo;
    private final CourseService courseService;
    private final Map<Long, IScoringStrategy> activeStrategies = new ConcurrentHashMap<>();
    private final Map<Long, List<Flight>> calculatedFlightCache = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final FlightService flightService;

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

    public List<Integer> getDefaultScores(Long tournamentId, Long playerId) {
        Tournament tournament = tournamentRepo.findById(tournamentId).orElseThrow();
        IScoringStrategy strategy = activeStrategies.get(tournamentId);

        if (strategy == null || tournament.isFinished()) {
            throw new ResourceConflictException("Tournament session expired or not initialized");
        }

        Long courseId = courseRepo.findByName(strategy.getCourseName()).orElseThrow().getId();

        return flightService.getDefaultScores(courseId, playerId, strategy.getHandicapMultiplier());
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

    public Tournament getTournament(Long tournamentId) {
        return tournamentRepo.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament " + tournamentId + " not found"));
    }

    // ================ Delete Tournament ======================
    @PreAuthorize( "hasRole('ADMIN')")
    public void deleteTournament(Long tournamentId) {
        Tournament tournament = tournamentRepo.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found"));

        // Remove association from the Season so the bidirectional collection stays consistent
        Season season = tournament.getSeason();
        if (season != null) {
            season.getTournaments().remove(tournament);
            seasonRepo.save(season);
        }

        // Clear the flights collection on the tournament so the join-table entries (tournament_flights)
        // are removed by JPA when we save the tournament. This avoids manual SQL on the join table.
        if (tournament.getFlights() != null && !tournament.getFlights().isEmpty()) {
            tournament.getFlights().clear();
            // Persist the cleared association to ensure join-table rows are deleted before deleting the tournament row
            tournamentRepo.saveAndFlush(tournament);
        }

        // Finally delete the tournament record itself
        tournamentRepo.deleteById(tournamentId);

        // Cleanup in-memory caches
        activeStrategies.remove(tournamentId);
        calculatedFlightCache.remove(tournamentId);
    }

    /**
     * Deletes the tournament and also deletes all Flights and FlightScores associated with it.
     * This relies on JPA cascading (Tournament -> Flight cascade = ALL, Flight -> FlightScore cascade = ALL
     * with orphanRemoval on Flight->FlightScore) so Spring will remove dependent rows automatically.
     */
    @PreAuthorize( "hasRole('ADMIN')")
    public void deleteTournamentWhole(Long tournamentId) {
        Tournament tournament = tournamentRepo.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found"));

        // Remove from season collection to keep bidirectional state consistent
        Season season = tournament.getSeason();
        if (season != null) {
            season.getTournaments().removeIf(t -> t.getId() == tournamentId);
            seasonRepo.save(season);
        }

        // Delete the tournament entity. Because Tournament.flights is cascade = ALL,
        // and Flight.flightScores is cascade = ALL with orphanRemoval, deleting the Tournament
        // will cascade deletes to Flight and FlightScore rows (handled by JPA).
        tournamentRepo.delete(tournament);

        // Cleanup in-memory caches
        activeStrategies.remove(tournamentId);
        calculatedFlightCache.remove(tournamentId);
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

        List<FlightScoreDTO> leaderBoard = new ArrayList<>();

        calculatedFlightCache.getOrDefault(tournament.getId(), Collections.emptyList())
            .forEach(f -> {
                f.getFlightScores().forEach(fs -> leaderBoard.add(fs.toDto()));
            });

        return leaderBoard;
    }

    public void addFlightToTournament(long flightId, long tournamentId) {
        Flight flight = flightService.getFlight(flightId);
        Tournament tournament = getTournament(tournamentId);

        IScoringStrategy strategy = activeStrategies.get(tournamentId);

        if (strategy == null || tournament.isFinished()) {
            throw new ResourceConflictException("Tournament session expired");
        }

        Flight calculatedFlight = strategy.calculateScores(flight);
        tournament.getFlights().add(flight);

        for (FlightScore fs : calculatedFlight.getFlightScores()) {
            Team team = fs.getPlayer().getTeam();
            standingRepo.findBySeasonNameAndTeamName(tournament.getSeason().getName(), team.getName())
                .ifPresent(standing -> standing.setBirdies(standing.getBirdies() + fs.getBirdies()));
        }

        tournamentRepo.save(tournament); // Cascades to Flight and FlightScores

        calculatedFlightCache.computeIfAbsent(tournamentId, k -> new ArrayList<>());
        calculatedFlightCache.get(tournamentId).add(calculatedFlight);
    }

    public List<TournamentDTO> getAllActiveTournaments() {
        return tournamentRepo.findAll().stream()
            .filter(t -> !t.isFinished())
            .map(Tournament::toDTO)
            .toList();
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
        tournamentRepo.save(tournament);
    }
}
