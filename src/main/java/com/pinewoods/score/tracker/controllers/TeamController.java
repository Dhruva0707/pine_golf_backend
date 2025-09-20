package com.pinewoods.score.tracker.controllers;

import com.pinewoods.score.tracker.entities.Team;
import com.pinewoods.score.tracker.services.TeamService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Team> createTeam(@RequestBody Team team) {
        Team saved = teamService.createTeam(team);
        return ResponseEntity.ok(saved);
    }

    @GetMapping
    public List<Team> getAllTeams() {
        return teamService.getAllTeams();
    }

    @GetMapping("/{name}")
    public ResponseEntity<Team> getTeam(@PathVariable String name) {
        return teamService.getTeamByName(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}