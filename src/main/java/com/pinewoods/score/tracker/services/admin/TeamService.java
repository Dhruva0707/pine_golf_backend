package com.pinewoods.score.tracker.services.admin;

import com.pinewoods.score.tracker.dao.admin.TeamRepository;
import com.pinewoods.score.tracker.dto.admin.PlayerDTO;
import com.pinewoods.score.tracker.dto.admin.TeamDTO;
import com.pinewoods.score.tracker.entities.admin.Team;
import com.pinewoods.score.tracker.exceptions.ResourceConflictException;
import com.pinewoods.score.tracker.exceptions.ResourceNotFoundException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class for managing Team entities.
 */
@Service
@Transactional
public class TeamService {
    private final TeamRepository teamRepository;

    public TeamService(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    // ----------- Create Team -----------
    /**
     * Creates a new team with the given name.
     * Only users with the ADMIN role can perform this operation.
     *
     * @param name name of the team
     * @throws ResourceConflictException if a team with the same name already exists
     */
    @PreAuthorize("hasRole('ADMIN')")
    public TeamDTO createTeam(String name) {
        if (teamRepository.existsByName(name)) {
            throw new ResourceConflictException("Team with name " + name + " already exists.");
        }
        var team = new Team(name);
        teamRepository.save(team);

        return createTeamDTO(team);
    }

    // ----------- Read Team -----------
    /**
     * Retrieves a team by its name.
     *
     * @param name name of the team
     * @return Team entity
     * @throws ResourceNotFoundException if the team does not exist
     */
    public TeamDTO getTeamByName(String name) {
        var team = teamRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Team with name " + name + " does not exist."));

        return createTeamDTO(team);
    }

    /**
     * Retrieves all teams.
     *
     * @return List of TeamDTOs
     */
    public List<TeamDTO> getAllTeams() {
        var teams = teamRepository.findAll();

        return teams.stream()
                .map(TeamService::createTeamDTO)
                .toList();
    }

    // ----------- Update Team -----------

    /**
     * Updates the name of an existing team.
     * Only users with the ADMIN role can perform this operation.
     *
     * @param currentName current name of the team
     * @param newName new name for the team
     * @return Updated TeamDTO
     * @throws ResourceNotFoundException if the team does not exist or if a team with the new name already exists
     */
    @PreAuthorize("hasRole('ADMIN')")
    public TeamDTO updateTeamName(String currentName, String newName) {
        var team = teamRepository.findByName(currentName)
            .orElseThrow(() -> new ResourceNotFoundException("Team with name " + currentName + " does not exist."));

        team.setName(newName);

        teamRepository.save(team);

        return createTeamDTO(team);
    }

    // ----------- Delete Team -----------
    /**
     * Deletes a team by its name.
     * Only users with the ADMIN role can perform this operation.
     * Sets the team of all players in the deleted team to UNASSIGNED.
     *
     * @param name name of the team to delete
     * @throws ResourceNotFoundException if the team does not exist
     */
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteTeam(String name) {
        var team = teamRepository.findByName(name)
            .orElseThrow(() -> new ResourceNotFoundException("Team with name " + name + " does not exist."));

        team.getPlayers().forEach(player -> player.setTeam(getUnassignedTeam()));

        teamRepository.delete(team);
    }

    // ----------- Helper Methods -----------
    public static TeamDTO createTeamDTO(Team team) {
        var playersList = team.getPlayers();
        List<PlayerDTO> playerDTOs = new ArrayList<>();

        if (playersList != null && !playersList.isEmpty()) {
            playerDTOs = playersList.stream().map(PlayerService::createPlayerDTO).toList();
        }

        return new TeamDTO(team.getName(), playerDTOs);
    }

    private Team getUnassignedTeam() {
        return teamRepository.findByName("UNASSIGNED")
            .orElseThrow(() -> new ResourceNotFoundException("UNASSIGNED team does not exist."));
    }
}
