package com.pinewoods.score.tracker.services.tournament;

import com.pinewoods.score.tracker.dao.admin.PlayerRepository;
import com.pinewoods.score.tracker.dao.season.SeasonRepository;
import com.pinewoods.score.tracker.dao.season.TeamStandingRepository;
import com.pinewoods.score.tracker.dao.tournament.TournamentRepository;
import com.pinewoods.score.tracker.dto.admin.PlayerDTO;
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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
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
    private final Map<Long, IScoringStrategy> activeStrategies = new ConcurrentHashMap<>();
    private final Map<Long, List<Flight>> calculatedFlightCache = new ConcurrentHashMap<>();

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
        IScoringStrategy strategy = activeStrategies.get(tournamentId);

        if (strategy == null || tournament.isFinished()) {
            throw new ResourceConflictException("Tournament session expired or not initialized");
        }

        // Use our Strategy to transform ScoreCards into a Flight entity
        Flight calculatedFlight = strategy.calculateScores(cards);
        Flight flight = createFlight(cards, strategy);
        tournament.getFlights().add(flight);

        // update birdies in standing for each team
        for (FlightScore fs : calculatedFlight.getFlightScores()) {
            Team team = fs.getPlayer().getTeam();
            standingRepo.findBySeasonNameAndTeamName(tournament.getSeason().getName(), team.getName())
                    .ifPresent(standing -> standing.setBirdies(standing.getBirdies() + fs.getBirdies()));
        }

        tournamentRepo.saveAndFlush(tournament); // Cascades to Flight and FlightScores

        calculatedFlightCache.computeIfAbsent(tournamentId, k -> new ArrayList<>());
        calculatedFlightCache.get(tournamentId).add(calculatedFlight);

        // Convert to DTO for the frontend
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

    // ================ Delete Tournament ======================
    @PreAuthorize( "hasRole('ADMIN')")
    public void deleteTournament(Long tournamentId) {
        Tournament tournament = tournamentRepo.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found"));
        Season season = tournament.getSeason();
        season.getTournaments().remove(tournament);
        tournamentRepo.delete(tournament);
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
}
