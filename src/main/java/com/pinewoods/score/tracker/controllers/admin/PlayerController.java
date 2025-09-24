package com.pinewoods.score.tracker.controllers.admin;

import com.pinewoods.score.tracker.controllers.admin.utilities.ControllerUtilities;
import com.pinewoods.score.tracker.dto.admin.PlayerDTO;
import com.pinewoods.score.tracker.services.admin.PlayerService;
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
 * Controller for managing players.
 */
@RestController
@RequestMapping("/players")
public class PlayerController {

    PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    // -------- Create Methods --------

    @PostMapping
    public ResponseEntity<PlayerDTO> createPlayer(@RequestBody PlayerDTO playerDTO) {
        PlayerDTO result = playerService.createPlayer(playerDTO);
        URI resultUri = ControllerUtilities.createResourceURI("name", result.name());

        return ResponseEntity.created(resultUri).body(result);
    }

    // -------- Read Methods --------

    @GetMapping("/{name}")
    public ResponseEntity<PlayerDTO> getPlayer(@PathVariable String name) {
        PlayerDTO result = playerService.getPlayerByName(name);
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<List<PlayerDTO>> getAllPlayers() {
        List<PlayerDTO> result = playerService.getAllPlayers();
        return ResponseEntity.ok(result);
    }

    // -------- Update Methods --------

    @PutMapping("/{name}/team")
    public ResponseEntity<PlayerDTO> updatePlayerTeam(
        @PathVariable String name,
        @RequestBody String teamName) {

        PlayerDTO result = playerService.updatePlayerTeam(name, teamName);

        return ResponseEntity.ok(result);
    }

    @PutMapping("/{name}/handicap")
    public ResponseEntity<PlayerDTO> updatePlayerHandicap(
        @PathVariable String name,
        @RequestBody Integer handicap) {

        PlayerDTO result = playerService.updatePlayerHandicap(name, handicap);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{name}/password")
    public ResponseEntity<PlayerDTO> updatePlayerPassword(
        @PathVariable String name,
        @RequestBody String rawPassword) {

        PlayerDTO result = playerService.updatePlayerPassword(name, rawPassword);
        return ResponseEntity.ok(result);
    }


    // -------- Delete Methods --------

    @DeleteMapping("/{name}")
    public ResponseEntity<Void> deletePlayer(@PathVariable String name) {
        playerService.deletePlayer(name);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
