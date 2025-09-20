package com.pinewoods.score.tracker.services;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import com.pinewoods.score.tracker.dao.TeamRepository;
import com.pinewoods.score.tracker.entities.Team;

import java.util.List;
import java.util.Optional;

@Service
public class TeamService {
    private final TeamRepository teamRepo;

    public TeamService(TeamRepository teamRepo) {
        this.teamRepo = teamRepo;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Team createTeam(Team team) {
        if (teamRepo.findByName(team.getName()).isPresent()) {
            throw new IllegalArgumentException("Team name already exists");
        }
        return teamRepo.save(team);
    }

    public List<Team> getAllTeams() {
        return teamRepo.findAll();
    }

    public Optional<Team> getTeamById(Long id) {
        return teamRepo.findById(id);
    }

    public Optional<Team> getTeamByName(String name) {
        return teamRepo.findByName(name);
    }
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteTeam(Long id) {
        teamRepo.deleteById(id);
    }
}
