package com.pinewoods.score.tracker.controllers.tournament;

import com.pinewoods.score.tracker.dto.flight.FlightDTO;
import com.pinewoods.score.tracker.dto.scoring.ScoreCardDTO;
import com.pinewoods.score.tracker.dto.tournament.TournamentDTO;
import com.pinewoods.score.tracker.entities.tournament.Tournament;
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
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tournaments")
@RequiredArgsConstructor
@Tag(name = "Tournament Management", description = "Endpoints for managing the golf tournament lifecycle")
public class TournamentController {

    private final TournamentService tournamentService;
    private final ScoringStrategyFactory strategyFactory;

    @PostMapping("/start")
    @Operation(summary = "Create and start a new tournament session",
            description = "Initializes the scoring strategy and parks it in memory until the tournament is finalized.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Tournament successfully started"),
            @ApiResponse(responseCode = "404", description = "Season not found")
    })
    public ResponseEntity<Tournament> startTournament(@Valid @RequestBody TournamentCreateRequest request) {
        // Build the strategy instance from request data
        IScoringStrategy strategy = strategyFactory.getStrategy(
                request.getStrategyType(),
                request.getPars(),
                request.getIndexes(),
                request.getPointsMap()
        );

        Tournament tournament = tournamentService.createTournament(
                request.getName(),
                request.getSeasonName(),
                strategy
        );
        return new ResponseEntity<>(tournament, HttpStatus.CREATED);
    }

    @PostMapping("/{id}/flights")
    @Operation(summary = "Add a scorecard",
            description = "Accepts a list of player scorecards for a single flight. Calculates scores immediately using the active strategy.")
    public ResponseEntity<FlightDTO> addFlight(
            @Parameter(description = "ID of the active tournament") @PathVariable Long id,
            @Valid @RequestBody List<ScoreCardDTO> cards) {
        return ResponseEntity.ok(tournamentService.addScorecards(id, cards));
    }

    @PostMapping("/{id}/end")
    @Operation(summary = "End and finalize the tournament",
            description = "Calculates awards (100/66/33), updates team standings, and clears the session from memory.")
    public ResponseEntity<Void> endTournament(@PathVariable Long id) {
        tournamentService.endTournament(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/season/{seasonName}")
    @Operation(summary = "Get all tournaments in a season")
    public ResponseEntity<List<TournamentDTO>> getBySeason(@PathVariable String seasonName) {
        return ResponseEntity.ok(tournamentService.getTournamentsBySeason(seasonName));
    }

    // ------------ DTO's ----------
    @Data
    public static class TournamentCreateRequest {
        @NotBlank
        private String name;
        @NotBlank private String seasonName;
        @NotBlank private String strategyType; // "STABLEFORD", "STROKEPLAY", etc.
        @Size(min = 18, max = 18) private List<Integer> pars;
        @Size(min = 18, max = 18) private List<Integer> indexes;
        @NotEmpty
        private Map<Integer, Integer> pointsMap;
    }
}
