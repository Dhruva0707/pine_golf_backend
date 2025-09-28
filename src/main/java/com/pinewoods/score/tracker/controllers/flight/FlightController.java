package com.pinewoods.score.tracker.controllers.flight;

import com.pinewoods.score.tracker.controllers.admin.utilities.ControllerUtilities;
import com.pinewoods.score.tracker.dto.admin.PlayerDTO;
import com.pinewoods.score.tracker.dto.flight.FlightDTO;
import com.pinewoods.score.tracker.dto.flight.FlightScoreDTO;
import com.pinewoods.score.tracker.entities.flight.Flight;
import com.pinewoods.score.tracker.services.flight.FlightService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/flights")
public class FlightController {

    FlightService flightService;

    public FlightController(FlightService flightService) {
        this.flightService = flightService;
    }

    // --------- Create Flight ------------

    @PostMapping()
    public ResponseEntity<FlightDTO> createFlight(@RequestBody List<FlightScoreDTO> scores) {
        Flight createdFlight = flightService.createFlight(scores);
        URI resourceUri = ControllerUtilities.createResourceURI("id", createdFlight.getId());
        return ResponseEntity.created(resourceUri)
                .body(FlightService.createDTO(createdFlight));
    }

    // ---------- Read Flight --------------
    @GetMapping("/{id}")
    public ResponseEntity<FlightDTO> getFlight(@PathVariable long id) {
        return ResponseEntity.ok(flightService.getFlightDTO(id));
    }
}
