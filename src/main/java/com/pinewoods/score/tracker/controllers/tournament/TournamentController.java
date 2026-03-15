package com.pinewoods.score.tracker.controllers.tournament;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pinewoods.score.tracker.controllers.admin.utilities.ControllerUtilities;
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
import jakarta.websocket.server.PathParam;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/start")
    @Operation(summary = "Create and start a new tournament session",
            description = "Initializes the scoring strategy and parks it in memory until the tournament is finalized.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Tournament successfully started"),
            @ApiResponse(responseCode = "404", description = "Season not found")
    })
    public ResponseEntity<TournamentDTO> startTournament(@Valid @RequestBody TournamentCreateRequest request) {
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
    public ResponseEntity<FlightDTO> addFlight(
            @Parameter(description = "ID of the active tournament") @PathVariable Long id,
            @Valid @RequestBody List<ScoreCardDTO> cards) {
        return ResponseEntity.ok(tournamentService.addScorecards(id, cards).toDTO());
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
        @JsonProperty("pars")
        @Size(min = 18, max = 18) private List<Integer> pars;
        @JsonProperty("indexes")
        @Size(min = 18, max = 18) private List<Integer> indexes;
        @JsonProperty("points_map")
        @NotEmpty
        private Map<Integer, Integer> pointsMap;
    }
}
