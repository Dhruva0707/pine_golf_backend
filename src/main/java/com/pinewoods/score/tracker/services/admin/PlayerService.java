package com.pinewoods.score.tracker.services.admin;

import com.pinewoods.score.tracker.dao.admin.PlayerRepository;
import com.pinewoods.score.tracker.dao.admin.TeamRepository;
import com.pinewoods.score.tracker.dao.flight.FlightScoreRepository;
import com.pinewoods.score.tracker.dto.admin.PlayerDTO;
import com.pinewoods.score.tracker.dto.admin.UpdatePlayerRequest;
import com.pinewoods.score.tracker.dto.flight.FlightDTO;
import com.pinewoods.score.tracker.entities.admin.Player;
import com.pinewoods.score.tracker.entities.admin.Role;
import com.pinewoods.score.tracker.entities.admin.Team;
import com.pinewoods.score.tracker.entities.flight.Flight;
import com.pinewoods.score.tracker.entities.flight.FlightScore;
import com.pinewoods.score.tracker.exceptions.ResourceConflictException;
import com.pinewoods.score.tracker.exceptions.ResourceNotFoundException;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.pinewoods.score.tracker.Utilities.isUserAdmin;

/**
 * Service class for managing Player entities.
 */
@Service
@Transactional
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final FlightScoreRepository flightScoreRepository;

    public PlayerService(PlayerRepository playerRepository, TeamRepository teamRepository,
                         FlightScoreRepository flightScoreRepository,
                         BCryptPasswordEncoder passwordEncoder) {
        this.playerRepository = playerRepository;
        this.teamRepository = teamRepository;
        this.passwordEncoder = passwordEncoder;
        this.flightScoreRepository = flightScoreRepository;
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
        Player player = Player.builder()
                .name(name)
                .password(encodedPassword)
                .role(Role.PLAYER)
                .handicap(handicap)
                .team(team)
                .build();

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

        if (player.getRole() == Role.ADMIN && !isUserAdmin()) {
            throw new ResourceNotFoundException("Player with name " + name + " does not exist.");
        }

        return player.toDTO();
    }

    /**
     * Retrieves all the players
     *
     * @return List of PlayerDTO representing all players
     */
    public List<PlayerDTO> getAllPlayers() {
        boolean isAdmin = isUserAdmin();
        return playerRepository.findAll().stream()
                .filter(player -> isAdmin || player.getRole() != Role.ADMIN)
                .map(PlayerService::createPlayerDTO)
                .toList();
    }

    public PlayerDTO getPlayerById(Long id) {
        return playerRepository.findById(id)
                .map(PlayerService::createPlayerDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Player with id " + id + " does not exist."));
    }

    /**
     * Retrieves all the flights for a player.
     * Along with the scores of the other players in the flight.
     *
     * @param playerName name of the player to retrieve flights for
     * @return List of FlightDTO representing all flights for the player
     */
    public List<FlightDTO> getPlayerFlights(String playerName) {
        Player player = playerRepository.findByName(playerName)
                .orElseThrow(() -> new ResourceNotFoundException("Player with name " + playerName + " does not exist."));

        List<Flight> flights = player.getFlightScores().stream()
                .map(FlightScore::getFlight)
                .toList();

        return flights.stream()
                .map(Flight::toDTO)
                .toList();
    }

    //----------- Update Player -----------

    @Transactional
    @PreAuthorize("hasRole('ADMIN')") // Strict Admin Only
    public PlayerDTO updatePlayerProfile(String currentName, UpdatePlayerRequest request) {
        Player player = playerRepository.findByName(currentName)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found"));

        if (request.newName() != null) {
            if (!request.newName().equals(player.getName()) && playerRepository.existsByName(request.newName())) {
                throw new ResourceConflictException("Name taken");
            }
            player.setName(request.newName());
        }

        if (request.handicap() != null) player.setHandicap(request.handicap());

        if (request.teamName() != null) {
            Team team = teamRepository.findByName(request.teamName())
                    .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
            player.setTeam(team);
        }

        return createPlayerDTO(playerRepository.save(player));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN') or authentication.name == #currentName")
    public PlayerDTO updatePlayerPassword(String currentName, String newPassword) {
        Player player = playerRepository.findByName(currentName)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found"));

        player.setPassword(passwordEncoder.encode(newPassword));
        return createPlayerDTO(playerRepository.save(player));
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
