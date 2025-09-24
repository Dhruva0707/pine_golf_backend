package com.pinewoods.score.tracker.controllers.admin;

import com.pinewoods.score.tracker.controllers.admin.utilities.ControllerUtilities;
import com.pinewoods.score.tracker.dto.admin.TeamDTO;
import com.pinewoods.score.tracker.services.admin.TeamService;
import java.net.URI;
import java.util.List;
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
public class TeamController {

    TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    // -------- Create Methods --------

    @PostMapping
    public ResponseEntity<TeamDTO> createTeam(@RequestBody String name) {
        TeamDTO result = teamService.createTeam(name);
        URI uri = ControllerUtilities.createResourceURI("name", name);
        return ResponseEntity.created(uri).body(result);
    }

    // -------- Read Methods --------

    @GetMapping("/{name}")
    public ResponseEntity<TeamDTO> getTeam(@PathVariable String name) {
        TeamDTO result = teamService.getTeamByName(name);
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<List<TeamDTO>> getAllTeams() {
        List<TeamDTO> result = teamService.getAllTeams();
        return ResponseEntity.ok(result);
    }

    // -------- Update Methods --------

    @PutMapping("/{name}/rename")
    public ResponseEntity<TeamDTO> renameTeam(@PathVariable String name, @RequestBody String newName) {
        TeamDTO result = teamService.updateTeamName(name, newName);
        return ResponseEntity.ok(result);
    }

    // -------- Delete Methods --------

    @DeleteMapping("/{name}")
    public ResponseEntity<Void> deleteTeam(@PathVariable String name) {
        teamService.deleteTeam(name);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
