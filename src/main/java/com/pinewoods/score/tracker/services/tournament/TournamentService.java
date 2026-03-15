package com.pinewoods.score.tracker.services.tournament;

import com.pinewoods.score.tracker.dao.admin.PlayerRepository;
import com.pinewoods.score.tracker.dao.season.SeasonRepository;
import com.pinewoods.score.tracker.dao.season.TeamStandingRepository;
import com.pinewoods.score.tracker.dao.tournament.TournamentRepository;
import com.pinewoods.score.tracker.dto.flight.FlightDTO;
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
import com.pinewoods.score.tracker.services.flight.FlightService;
import com.pinewoods.score.tracker.services.scoring.IScoringStrategy;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TournamentService {
    private final TournamentRepository tournamentRepo;
    private final SeasonRepository seasonRepo;
    private final PlayerRepository playerRepo;
    private final TeamStandingRepository standingRepo;
    private final Map<Long, IScoringStrategy> activeStrategies = new ConcurrentHashMap<>();

    // ==================== Create Tournament ====================
     /**
     * @param name name of the tournament
     * @param seasonName name of the season
     * @param strategy scoring strategy
     * @return Tournament object created
     */
    @Transactional
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
    @Transactional
    @PreAuthorize( "hasRole('ADMIN')")
    public Flight addScorecards(Long tournamentId, List<ScoreCardDTO> cards) {
        Tournament tournament = tournamentRepo.findById(tournamentId).orElseThrow();
        IScoringStrategy strategy = activeStrategies.get(tournamentId);

        if (strategy == null || tournament.isFinished()) {
            throw new ResourceConflictException("Tournament session expired or not initialized");
        }

        // Use our Strategy to transform ScoreCards into a Flight entity
        Flight flight = strategy.calculateScores(cards);
        tournament.getFlights().add(flight);

        // update birdies in standing for each team
        for (FlightScore fs : flight.getFlightScores()) {
            Team team = fs.getPlayer().getTeam();
            standingRepo.findBySeasonNameAndTeamName(tournament.getSeason().getName(), team.getName())
                    .ifPresent(standing -> standing.setBirdies(standing.getBirdies() + fs.getBirdies()));
        }

        tournamentRepo.save(tournament); // Cascades to Flight and FlightScores

        // Convert to DTO for the frontend
        return flight;
    }

    /**
     * Completes a tournament and updates values
     *
     * @param tournamentId tournament id
     */
    @Transactional
    @PreAuthorize( "hasRole('ADMIN')")
    public void endTournament(Long tournamentId) {
        Tournament tournament = tournamentRepo.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament " + tournamentId + " not found"));

        // Calculate the 100, 66, 33 points and specialty awards
        calculateFinalAwards(tournament);

        // Update Team Standings in the Season
        updateTeamStandings(tournament);

        tournament.setFinished(true);
        tournamentRepo.save(tournament);

        // Cleanup: Memory is freed, Strategy is garbage collected
        activeStrategies.remove(tournamentId);
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
                .map(t -> new TournamentDTO(t.getName(), t.getAwards(),
                        t.getId(), t.getSeason().getId()))
                .toList();
    }

    public List<TournamentDTO> getTournamentsByName(String name) {
        return tournamentRepo.findAllByName(name)
                .stream()
                .map(t -> new TournamentDTO(t.getName(), t.getAwards(),
                        t.getId(), t.getSeason().getId()))
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
        tournamentRepo.deleteById(tournamentId);
    }

    // ============= Utilities =============
    private void calculateFinalAwards(Tournament tournament) {
        List<FlightScore> leaderboard = tournament.getFlights().stream()
                .flatMap(f -> f.getFlightScores().stream())
                .sorted(Comparator.comparingInt(FlightScore::getScore).reversed())
                .toList();

        allocateRanks(leaderboard, tournament);
        allocateBirdies(tournament, leaderboard);
    }

    private void allocateRanks(List<FlightScore> leaderboard, Tournament t) {
        Map<Integer, List<Player>> groups = leaderboard.stream()
                .collect(Collectors.groupingBy(
                        FlightScore::getScore,
                        LinkedHashMap::new, // Keep the sorted order
                        Collectors.mapping(FlightScore::getPlayer, Collectors.toList())
                ));

        List<List<Player>> rankedGroups = new ArrayList<>(groups.values());

        List<Player> firstPlacePlayers = rankedGroups.get(0);
        // If 3 people tie for 1st, they all get 100.
        int firstPoints = 100 / firstPlacePlayers.size();
        firstPlacePlayers.forEach(p -> t.getAwards().put(p.getId(), firstPoints));

        if (rankedGroups.size() > 1 && firstPlacePlayers.size() == 1) {
            // Only award 2nd place if there wasn't a tie for 1st that "consumed" the rank
            List<Player> secondPlacePlayers = rankedGroups.get(1);
            int secondPoints = 66 / secondPlacePlayers.size();
            secondPlacePlayers.forEach(p -> t.getAwards().put(p.getId(), secondPoints));

            if (rankedGroups.size() > 2 && secondPlacePlayers.size() == 1) {
                List<Player> thirdPlacePlayers = rankedGroups.get(2);
                int thirdPoints = 33 / thirdPlacePlayers.size();
                thirdPlacePlayers.forEach(p -> t.getAwards().put(p.getId(), thirdPoints));
            }
        } else if (rankedGroups.size() > 1 && firstPlacePlayers.size() > 1) {
            List<Player> thirdPlacePlayers = rankedGroups.get(1);
            int thirdPoints = 33 / thirdPlacePlayers.size();
            thirdPlacePlayers.forEach(p -> t.getAwards().put(p.getId(), thirdPoints));
        }
    }

    private void allocateBirdies(Tournament t, List<FlightScore> leaderboard) {
        leaderboard.stream()
                .max(Comparator.comparingInt(FlightScore::getBirdies))
                .ifPresent(fs -> t.getAwards()
                        .merge(fs.getPlayer().getId(), 50, Integer::sum));
    }

    private void updateTeamStandings(Tournament tournament) {
        Season season = tournament.getSeason();
        tournament.getAwards().forEach((playerId, points) -> {
            Player player = playerRepo.findById(playerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Player with id " + playerId + " not found"));
            Team team = player.getTeam();

            // Find existing standing for this team in this season, or create new
            TeamStanding standing = standingRepo.findBySeasonNameAndTeamName(
                    season.getName(), team.getName())
                    .orElse(TeamStanding.builder().season(season).team(team).points(0).build());

            standing.setPoints(standing.getPoints() + points);

            // update wins
            standing.setWins(standing.getWins() + 1);

            standingRepo.save(standing);
        });
    }
}
