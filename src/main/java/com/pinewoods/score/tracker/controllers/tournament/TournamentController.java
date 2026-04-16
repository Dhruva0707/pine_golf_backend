package com.pinewoods.score.tracker.controllers.tournament;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pinewoods.score.tracker.controllers.admin.utilities.ControllerUtilities;
import com.pinewoods.score.tracker.dao.admin.PlayerRepository;
import com.pinewoods.score.tracker.dao.course.CourseRepository;
import com.pinewoods.score.tracker.dto.flight.FlightScoreDTO;
import com.pinewoods.score.tracker.dto.scoring.ScoreCardDTO;
import com.pinewoods.score.tracker.dto.tournament.TournamentDTO;
import com.pinewoods.score.tracker.entities.course.Course;
import com.pinewoods.score.tracker.entities.tournament.Tournament;
import com.pinewoods.score.tracker.exceptions.ResourceNotFoundException;
import com.pinewoods.score.tracker.services.scoring.IScoringStrategy;
import com.pinewoods.score.tracker.services.scoring.ScoringStrategyFactory;
import com.pinewoods.score.tracker.services.tournament.TournamentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tournaments")
@RequiredArgsConstructor
@Tag(name = "Tournament Management", description = "Endpoints for managing the golf tournament lifecycle")
public class TournamentController {

    private final TournamentService tournamentService;
    private final ScoringStrategyFactory strategyFactory;
    private final PlayerRepository playerRepository;
    private final CourseRepository courseRepository;

    @PostMapping("/start")
    @Operation(summary = "Create and start a new tournament session",
            description = "Initializes the scoring strategy and parks it in memory until the tournament is finalized.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Tournament successfully started"),
            @ApiResponse(responseCode = "404", description = "Season not found")
    })
    public ResponseEntity<TournamentDTO> startTournament(@Valid @RequestBody TournamentCreateRequest request) {
        Course course = courseRepository.findByName(request.courseName)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        // Build the strategy instance from request data
        IScoringStrategy strategy = strategyFactory.getStrategy(
                request.getStrategyType(),
                course.getPars(),
                course.getIndexes(),
                request.getPointsMap(),
                request.getHandicapMultiplier(),
                request.getCourseName(),
                playerRepository
        );

        Tournament tournament = tournamentService.createTournament(
                request.getName(),
                request.getSeasonName(),
                strategy
        );

        TournamentDTO tournamentDTO = tournament.toDTO();
        URI resultUri = ControllerUtilities.createResourceURI("name", tournamentDTO.name());
        return ResponseEntity.created(resultUri).body(tournamentDTO);
    }

    @PostMapping("/{id}/flights")
    @Operation(summary = "Add a scorecard",
            description = """
                    Accepts a list of player scorecards for a single flight.
                    Calculates scores immediately using the active strategy.
                    """)
    public ResponseEntity<Void> addFlight(
            @Parameter(description = "ID of the active tournament") @PathVariable Long id,
            @Valid @RequestBody List<ScoreCardDTO> cards) {
        tournamentService.addScorecards(id, cards);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/end")
    @Operation(summary = "End and finalize the tournament",
            description = "Calculates awards (100/66/33), updates team standings, and clears the session from memory.")
    public ResponseEntity<Void> endTournament(@PathVariable Long id) {
        tournamentService.endTournament(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{tournamentName}")
    @Operation(summary = "Get tournament by the name",
            description = "Returns a list of tournaments with the given name")
    public ResponseEntity<List<TournamentDTO>> getTournamentByName(@PathVariable String tournamentName) {
        List<TournamentDTO> tournaments = tournamentService.getTournamentsByName(tournamentName);
        return ResponseEntity.ok(tournaments);
    }

    @GetMapping("/{seasonName}/{tournamentName}")
    @Operation(summary = "Get a tournament by name and season name")
    public ResponseEntity<TournamentDTO> getTournament(@PathVariable("tournamentName") String tournamentName,
                                                       @PathVariable("seasonName") String seasonName) {
        return ResponseEntity.ok(tournamentService.getTournamentBySeasonAndName(seasonName, tournamentName).toDTO());
    }

    @GetMapping("/{seasonName}/{tournamentName}/leaderBoard")
    @Operation(summary = "Get the current tournament's leaderboard")
    public ResponseEntity<List<FlightScoreDTO>> getLeaderBoard(@PathVariable("tournamentName") String tournamentName,
                                                               @PathVariable("seasonName") String seasonName) {
        return ResponseEntity.ok(tournamentService.getTournamentLeaderBoard(seasonName, tournamentName));
    }

    @GetMapping("/{tournamentId}/{playerId}")
    @Operation(summary = "Get the effective net par score for the player in that particular tournament")
    public ResponseEntity<List<Integer>> getTournamentExpectedScore(@PathVariable("tournamentId") Long tournamentId,
                                                                    @PathVariable("playerId") Long playerId) {
        return ResponseEntity.ok(tournamentService.getDefaultScores(tournamentId, playerId));
    }

    @GetMapping("/{tournamentId}/{handicap}/score")
    @Operation(summary = "Get the effective net par score for a given handicap in a tournament")
    public ResponseEntity<List<Integer>> getTournamentExpectedScoreByHandicap(@PathVariable("tournamentId") Long tournamentId,
                                                                              @PathVariable("handicap") double handicap) {
        return ResponseEntity.ok(tournamentService.getDefaultScoresByHandicap(tournamentId, handicap));
    }

    // ------------ Delete Tournament -----------
    @DeleteMapping("/{tournamentId}")
    @Operation(summary = "Delete a tournament by name")
    public ResponseEntity<Void> deleteTournament(@PathVariable("tournamentId") Long tournamentId) {

        tournamentService.deleteTournament(tournamentId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }


    // ------------ DTO's ----------
    @Data
    @AllArgsConstructor
    public static class TournamentCreateRequest {
        @NotBlank
        @JsonProperty("name")
        private String name;
        @JsonProperty("season_name")
        @NotBlank
        private String seasonName;
        @JsonProperty("strategy_type")
        @NotBlank
        private String strategyType; // "STABLEFORD", "STROKEPLAY", etc.
        @JsonProperty("courseName")
        private String courseName;
        @JsonProperty("pointsMap")
        @NotEmpty
        private Map<Integer, Integer> pointsMap;
        @JsonProperty("handicapMultiplier")
        private double handicapMultiplier;
    }

    // ============= Import and export ================
    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> exportTournament(@PathVariable Long id) {
        try {
            byte[] jsonData = tournamentService.exportTournament(id);
            String filename = "tournament_export_" + id + ".json";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonData);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Uploads a previously exported JSON file to import it into a season.
     */
    @PostMapping("/import")
    public ResponseEntity<String> importTournament(
            @RequestParam("file") MultipartFile file,
            @RequestParam("seasonName") String seasonName) {
        try {
            tournamentService.importTournament(file.getBytes(), seasonName);
            return ResponseEntity.ok("Tournament imported successfully into " + seasonName);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Failed to process the import file.");
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
