package com.pinewoods.score.tracker.controllers;

import com.pinewoods.score.tracker.entities.Player;
import com.pinewoods.score.tracker.entities.Team;
import com.pinewoods.score.tracker.services.PlayerService;
import com.pinewoods.score.tracker.services.TeamService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/players")
public class PlayerController {
    private final PlayerService playerService;
    private final TeamService teamService;

    public PlayerController(PlayerService playerService, TeamService teamService) {
        this.playerService = playerService;
        this.teamService = teamService;
    }

    @PostMapping
    public ResponseEntity<Player> createPlayer(@RequestBody Player player) {
        if (player.getTeam() == null) {
            var defaultTeam = teamService.getTeamByName("UNASSIGNED");
            defaultTeam.ifPresent(player::setTeam);
        }
        Player saved = playerService.createPlayer(player);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{name}")
    public ResponseEntity<Player> getPlayer(@PathVariable String name) {
        return  playerService.getPlayerByName(name)
                .map(ResponseEntity::ok)
                .orElse((ResponseEntity.notFound().build()));
    }

    @GetMapping
    public List<Player> getAllPlayers() {
        return playerService.getAllPlayers();
    }

    @PutMapping("/{username}/password")
    public ResponseEntity<Void> changePassword(@PathVariable String username,
                                               @RequestBody String newPassword) {
        playerService.changePassword(username, newPassword);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<Void> deletePlayer(@PathVariable String userName) {
        var player = playerService.getPlayerByName(userName);
        if (player.isPresent()) {
            Long id = player.get().getId();
            playerService.deletePlayer(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}