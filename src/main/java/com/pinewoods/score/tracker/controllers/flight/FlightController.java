package com.pinewoods.score.tracker.controllers.flight;

import com.pinewoods.score.tracker.controllers.admin.utilities.ControllerUtilities;
import com.pinewoods.score.tracker.dto.flight.FlightDTO;
import com.pinewoods.score.tracker.dto.flight.FlightScoreDTO;
import com.pinewoods.score.tracker.entities.flight.Flight;
import com.pinewoods.score.tracker.services.flight.FlightService;
import com.pinewoods.score.tracker.services.tournament.TournamentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/flights")
@Tag(name = "Flight Management", description = "Operations related to golf flights and scores")
public class FlightController {

    private final TournamentService tournamentService;

    FlightService flightService;

    public FlightController(FlightService flightService, TournamentService tournamentService) {
        this.flightService = flightService;
        this.tournamentService = tournamentService;
    }

    // --------- Create Flight ------------

    @Operation(
            summary = "Create a new flight",
            description = "Creates a new flight with the provided scores.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Flight created successfully",
                    content = @Content(schema = @Schema(implementation = FlightDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    @PostMapping()
    public ResponseEntity<FlightDTO> createFlight(@RequestBody List<FlightScoreDTO> scores) {
        Flight createdFlight = flightService.createFlight(scores);
        URI resourceUri = ControllerUtilities.createResourceURI("id", createdFlight.getId());
        return ResponseEntity.created(resourceUri)
                .body(createdFlight.toDTO());
    }

    // --------- Push Flight ------------
    @Operation(
        summary = "Push a flight to a tournament",
        description = "Pushes a flight to a tournament, associating it with the specified tournament ID.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @PatchMapping("/{id}/{tournamentId}/link")
    public ResponseEntity<Void> pushFlightToTournament(@PathVariable long id, @PathVariable long tournamentId) {
        tournamentService.addFlightToTournament(id, tournamentId);

        return ResponseEntity.ok().build();
    }

    // ---------- Read Flight --------------
    @Operation(
            summary = "Get a flight by ID",
            description = "Retrieves details of a specific flight by its ID.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Flight found",
                    content = @Content(schema = @Schema(implementation = FlightDTO.class))),
            @ApiResponse(responseCode = "404", description = "Flight not found", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<FlightDTO> getFlight(@PathVariable long id) {
        return ResponseEntity.ok(flightService.getFlight(id).toDTO());
    }

    @GetMapping("/all")
    public ResponseEntity<List<FlightDTO>> getAllFlights() {
        return ResponseEntity.ok(flightService.getAllFlights().stream().map(Flight::toDTO).toList());
    }

    @GetMapping("/{courseId}/{playerId}")
    @Operation(summary = "Get the effective net par score for the player in that particular tournament")
    public ResponseEntity<List<Integer>> getExpectedScore(@PathVariable("courseId") Long courseId,
                                                                    @PathVariable("playerId") Long handicap) {
        return ResponseEntity.ok(flightService.getDefaultScores(courseId, handicap));
    }
}
