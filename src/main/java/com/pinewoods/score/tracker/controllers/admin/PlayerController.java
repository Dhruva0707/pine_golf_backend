package com.pinewoods.score.tracker.controllers.admin;

import com.pinewoods.score.tracker.controllers.admin.utilities.ControllerUtilities;
import com.pinewoods.score.tracker.dto.admin.PlayerDTO;
import com.pinewoods.score.tracker.dto.flight.FlightDTO;
import com.pinewoods.score.tracker.services.admin.PlayerService;
import java.net.URI;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for managing players.
 */
@RestController
@RequestMapping("/players")
@Tag(name = "Player Management", description = "Endpoints for managing players, teams, and handicaps")
public class PlayerController {

    PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    // -------- Create Methods --------

    @Operation(
        summary = "Create a new player",
        description = "Registers a new player in the system. **Role required: ADMIN**",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Player created successfully"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required"),
        @ApiResponse(responseCode = "400", description = "Invalid player data provided")
    })
    @PostMapping
    public ResponseEntity<PlayerDTO> createPlayer(@RequestBody PlayerDTO playerDTO) {
        PlayerDTO result = playerService.createPlayer(playerDTO);
        URI resultUri = ControllerUtilities.createResourceURI("name", result.name());

        return ResponseEntity.created(resultUri).body(result);
    }

    // -------- Read Methods --------

    @Operation(summary = "Get player by name", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{name}")
    public ResponseEntity<PlayerDTO> getPlayer(@PathVariable String name) {
        PlayerDTO result = playerService.getPlayerByName(name);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Get all players", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    public ResponseEntity<List<PlayerDTO>> getAllPlayers() {
        List<PlayerDTO> result = playerService.getAllPlayers();
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Get player's flights",
        description = "Retrieves all flights associated with a specific player.",
        security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{name}/flights")
    public ResponseEntity<List<FlightDTO>> getAllFlights(@PathVariable String name) {
        List<FlightDTO> result = playerService.getPlayerFlights(name);
        return ResponseEntity.ok(result);
    }

    // -------- Update Methods --------

    @Operation(
        summary = "Update player team",
        description = "Assigns a player to a different team. **Role required: ADMIN**",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping("/{name}/team")
    public ResponseEntity<PlayerDTO> updatePlayerTeam(
        @PathVariable String name,
        @RequestBody String teamName) {

        PlayerDTO result = playerService.updatePlayerTeam(name, teamName);

        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Update player handicap",
            description = "Updates the numerical handicap for a player. **Role required: ADMIN**",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping("/{name}/handicap")
    public ResponseEntity<PlayerDTO> updatePlayerHandicap(
        @PathVariable String name,
        @RequestBody Integer handicap) {

        PlayerDTO result = playerService.updatePlayerHandicap(name, handicap);
        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Change player password",
            description = "Updates the password for a player. Can be performed by the player themselves or an Admin.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping("/{name}/password")
    public ResponseEntity<PlayerDTO> updatePlayerPassword(
        @PathVariable String name,
        @RequestBody String rawPassword) {

        PlayerDTO result = playerService.updatePlayerPassword(name, rawPassword);
        return ResponseEntity.ok(result);
    }


    // -------- Delete Methods --------

    @Operation(
            summary = "Delete a player",
            description = "Permanently removes a player from the system. **Role required: ADMIN**",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Player deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Player not found")
    })
    @DeleteMapping("/{name}")
    public ResponseEntity<Void> deletePlayer(@PathVariable String name) {
        playerService.deletePlayer(name);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
