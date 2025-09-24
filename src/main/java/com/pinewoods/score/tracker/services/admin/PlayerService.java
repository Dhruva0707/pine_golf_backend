package com.pinewoods.score.tracker.services.admin;

import com.pinewoods.score.tracker.dao.admin.PlayerRepository;
import com.pinewoods.score.tracker.dao.admin.TeamRepository;
import com.pinewoods.score.tracker.dto.admin.PlayerDTO;
import com.pinewoods.score.tracker.entities.admin.Player;
import com.pinewoods.score.tracker.entities.admin.Role;
import com.pinewoods.score.tracker.entities.admin.Team;
import com.pinewoods.score.tracker.exceptions.ResourceConflictException;
import com.pinewoods.score.tracker.exceptions.ResourceNotFoundException;
import java.util.List;
import java.util.stream.StreamSupport;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class for managing Player entities.
 */
@Service
@Transactional
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public PlayerService(PlayerRepository playerRepository, TeamRepository teamRepository, BCryptPasswordEncoder passwordEncoder) {
        this.playerRepository = playerRepository;
        this.teamRepository = teamRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ----------- Create Player -----------

    /**
     * Creates a new player with the given details.
     * Only users with the ADMIN role can perform this operation.
     *
     * @param name name of the player
     * @param rawPassword raw password of the player
     * @param teamName name of the team the player belongs to
     * @param handicap handicap of the player
     * @return PlayerDTO representing the created player
     * @throws ResourceConflictException if a player with the same name already exists
     * @throws ResourceNotFoundException if the specified team does not exist
     */
    @PreAuthorize("hasRole('ADMIN')")
    public PlayerDTO createPlayer(String name, String rawPassword, String teamName, double handicap) {
        if (playerRepository.existsByName(name)) {
            throw new ResourceConflictException("Player with name " + name + " already exists.");
        }

        String teamNameInternal = teamName == null || teamName.isBlank() ? "UNASSIGNED" : teamName;

        Team team = teamRepository.findByName(teamNameInternal)
                .orElseThrow(() -> new ResourceNotFoundException("Team with name " + teamNameInternal + " does not exist."));

        String encodedPassword = passwordEncoder.encode(rawPassword);
        Player player = new Player(name, encodedPassword, handicap, Role.PLAYER, team);
        playerRepository.save(player);

        return createPlayerDTO(player);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public PlayerDTO createPlayer(PlayerDTO playerDTO) {
        return createPlayer(playerDTO.name(), "P!neWo0d$", playerDTO.team(), playerDTO.handicap());
    }

    // ----------- Get Player -----------
    /**
     * Retrieves a player by their name.
     *
     * @param name name of the player
     * @return PlayerDTO representing the player
     * @throws ResourceNotFoundException if the player does not exist
     */
    public PlayerDTO getPlayerByName(String name) {
        Player player = playerRepository.findByName(name)
            .orElseThrow(() -> new ResourceNotFoundException("Player with name " + name + " does not exist."));

        return createPlayerDTO(player);
    }

    /**
     * Retrieves all the players
     *
     * @return List of PlayerDTO representing all players
     */
    public List<PlayerDTO> getAllPlayers() {
        return playerRepository.findAll().stream()
                .map(PlayerService::createPlayerDTO)
                .toList();
    }

    //----------- Update Player -----------

    /**
     * Updates the team of a player.
     * Only users with the ADMIN role can perform this operation.
     *
     * @param playerName name of the player to update
     * @param teamName name of the new team
     * @return PlayerDTO representing the updated player
     * @throws ResourceNotFoundException if the player or team does not exist
     */
    @PreAuthorize("hasRole('ADMIN')")
    public PlayerDTO updatePlayerTeam(String playerName, String teamName) {
        Player player = playerRepository.findByName(playerName)
            .orElseThrow(() -> new ResourceNotFoundException("Player with name " + playerName + " does not exist."));

        Team team = teamRepository.findByName(teamName)
                .orElseThrow(() -> new ResourceNotFoundException("Team with name " + teamName + " does not exist."));

        player.setTeam(team);
        playerRepository.save(player);

        return createPlayerDTO(player);
    }

    /**
     * Updates the handicap of a player.
     * Only users with the ADMIN role can perform this operation.
     *
     * @param playerName name of the player to update
     * @param handicap new handicap value
     * @return PlayerDTO representing the updated player
     * @throws ResourceNotFoundException if the player does not exist
     */
    @PreAuthorize("hasRole('ADMIN')")
    public PlayerDTO updatePlayerHandicap(String playerName, int handicap) {
        Player player = playerRepository.findByName(playerName)
            .orElseThrow(() -> new ResourceNotFoundException("Player with name " + playerName + " does not exist."));

        player.setHandicap(handicap);
        playerRepository.save(player);

        return createPlayerDTO(player);
    }

    @PreAuthorize("hasRole('ADMIN') or authentication.name == #playerName")
    public PlayerDTO updatePlayerPassword(String playerName, String rawPassword) {
        Player player = playerRepository.findByName(playerName)
            .orElseThrow(() -> new ResourceNotFoundException("Player with name " + playerName + " does not exist."));

        String encodedPassword = passwordEncoder.encode(rawPassword);
        player.setPassword(encodedPassword);
        playerRepository.save(player);
        return createPlayerDTO(player);
    }

    // ----------- Delete Player -----------

    /**
     * Deletes a player by their name.
     * Only users with the ADMIN role can perform this operation.
     *
     * @param playerName name of the player to delete
     * @throws ResourceNotFoundException if the player does not exist
     */
    @PreAuthorize("hasRole('ADMIN')")
    public void deletePlayer(String playerName) {
        Player player = playerRepository.findByName(playerName)
            .orElseThrow(() -> new ResourceNotFoundException("Player with name " + playerName + " does not exist."));

        playerRepository.delete(player);
    }

    // ----------- Helper Methods -----------

    /**
     * A helper method that converts a Player entity to a PlayerDTO.
     *
     * @param player the Player entity to convert
     * @return PlayerDTO representing the player
     */
    public static PlayerDTO createPlayerDTO(Player player) {
        return new PlayerDTO(player.getName(), player.getTeam().getName(), player.getHandicap());
    }
}
