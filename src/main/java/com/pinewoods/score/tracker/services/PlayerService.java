package com.pinewoods.score.tracker.services;

import com.pinewoods.score.tracker.dao.PlayerRepository;
import com.pinewoods.score.tracker.dao.TeamRepository;
import com.pinewoods.score.tracker.entities.Player;
import com.pinewoods.score.tracker.entities.Team;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.List;
import java.util.Optional;

@Service
public class PlayerService {
    private final PlayerRepository playerRepo;
    private final TeamRepository teamRepo;
    private final PasswordEncoder pwdEncoder;

    public PlayerService(PlayerRepository playerRepo, TeamRepository teamRepo, PasswordEncoder pwdEncoder) {
        this.playerRepo = playerRepo;
        this.teamRepo = teamRepo;
        this.pwdEncoder = pwdEncoder;
    }

    /**
     * Creates new players
     */
    @PreAuthorize("hasRole('ADMIN')")
    public Player createPlayer(Player player) {
        player.setPassword(pwdEncoder.encode(player.getPassword()));

        if (playerRepo.findByName(player.getName()).isPresent()) {
            throw new IllegalArgumentException("Player name already exists");
        }

        return playerRepo.save(player);
    }

    /**
     * Fetches the player by name
     * @param name of the player
     * @return Optional with the player
     */
    public Optional<Player> getPlayerByName(String name) {
        return playerRepo.findByName(name);
    }

    /**
     * Fetched the player by the id
     * @param id player id
     * @return Optional of the player
     */
    public Optional<Player> getPlayerById(Long id) {
        return playerRepo.findById(id);
    }

    /**
     * Fetches all players. Could be restricted if needed.
     */
    public List<Player> getAllPlayers() {
        return playerRepo.findAll();
    }

    /**
     * Allows the player and the admin to update their own password.
     *
     * @param username username of the player
     * @param newPassword updated password
     */
    @PreAuthorize("#username == authentication.name or hasRole('ADMIN')")
    public void changePassword(String username, String newPassword) {
        Player player = playerRepo.findByName(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        player.setPassword(pwdEncoder.encode(newPassword));
        playerRepo.save(player);
    }

    /**
     * Deletes a player by ID. Only ADMINs can do this.
     * @param id id to be deleted
     */
    @PreAuthorize("hasRole('ADMIN')")
    public void deletePlayer(Long id) {
        playerRepo.deleteById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Player changePlayerTeam(Long playerId, Long newTeamId) {
        Player player = playerRepo.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));

        Team newTeam = teamRepo.findById(newTeamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found"));

        player.setTeam(newTeam);
        return playerRepo.save(player);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Player changePlayerTeam(String playerName, String teamName) {
        Player player = playerRepo.findByName(playerName)
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));

        Team newTeam = teamRepo.findByName(teamName)
                .orElseThrow(() -> new IllegalArgumentException("Team not found"));

        return changePlayerTeam(player.getId(), newTeam.getId());
    }

    // Exception handler for database level difficulties
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> handleConstraintViolation(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body("Name must be unique");
    }

}
