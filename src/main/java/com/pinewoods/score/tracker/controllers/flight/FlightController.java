package com.pinewoods.score.tracker.controllers.flight;

import com.pinewoods.score.tracker.controllers.admin.utilities.ControllerUtilities;
import com.pinewoods.score.tracker.dto.flight.FlightDTO;
import com.pinewoods.score.tracker.dto.flight.FlightScoreDTO;
import com.pinewoods.score.tracker.entities.flight.Flight;
import com.pinewoods.score.tracker.services.flight.FlightService;
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

    FlightService flightService;

    public FlightController(FlightService flightService) {
        this.flightService = flightService;
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
}
