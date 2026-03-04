package com.pinewoods.score.tracker.controllers.admin;

import com.pinewoods.score.tracker.controllers.admin.utilities.ControllerUtilities;
import com.pinewoods.score.tracker.dto.admin.TeamDTO;
import com.pinewoods.score.tracker.services.admin.TeamService;
import java.net.URI;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
 * Controller for managing team entities
 */
@RestController
@RequestMapping("/teams")
@Tag(name = "Team Management", description = "Operations related to golf teams and rosters")
public class TeamController {

    TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    // -------- Create Methods --------

    @Operation(
        summary = "Create a new team",
        description = "Creates a new golf team with the given name.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Team created successfully",
                content = @Content(schema = @Schema(implementation = TeamDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    @PostMapping
    public ResponseEntity<TeamDTO> createTeam(@RequestBody String name) {
        TeamDTO result = teamService.createTeam(name);
        URI uri = ControllerUtilities.createResourceURI("name", name);
        return ResponseEntity.created(uri).body(result);
    }

    // -------- Read Methods --------

    @Operation(
        summary = "Get a team by name",
        description = "Retrieves details of a specific team by its name.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Team found",
                content = @Content(schema = @Schema(implementation = TeamDTO.class))),
        @ApiResponse(responseCode = "404", description = "Team not found", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    @GetMapping("/{name}")
    public ResponseEntity<TeamDTO> getTeam(@PathVariable String name) {
        TeamDTO result = teamService.getTeamByName(name);
        return ResponseEntity.ok(result);
    }
    @Operation(
            summary = "Get all teams",
            description = "Retrieves a list of all golf teams.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of teams retrieved",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = TeamDTO.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    @GetMapping
    public ResponseEntity<List<TeamDTO>> getAllTeams() {
        List<TeamDTO> result = teamService.getAllTeams();
        return ResponseEntity.ok(result);
    }

    // -------- Update Methods --------

    @Operation(
            summary = "Rename a team",
            description = "Updates the name of an existing team.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Team renamed successfully",
                    content = @Content(schema = @Schema(implementation = TeamDTO.class))),
            @ApiResponse(responseCode = "404", description = "Team not found", content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid new name", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    @PutMapping("/{name}/rename")
    public ResponseEntity<TeamDTO> renameTeam(@PathVariable String name, @RequestBody String newName) {
        TeamDTO result = teamService.updateTeamName(name, newName);
        return ResponseEntity.ok(result);
    }

    // -------- Delete Methods --------

    @Operation(
            summary = "Delete a team",
            description = "Deletes a team by its name.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Team deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Team not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @DeleteMapping("/{name}")
    public ResponseEntity<Void> deleteTeam(@PathVariable String name) {
        teamService.deleteTeam(name);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
