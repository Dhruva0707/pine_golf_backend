package com.pinewoods.score.tracker.dao.season;

import com.pinewoods.score.tracker.entities.season.TeamStanding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamStandingRepository extends JpaRepository<TeamStanding, Long> {
    Optional<TeamStanding> findBySeasonNameAndTeamName(String seasonName, String teamName);
    List<TeamStanding> findAllByTeamName(String TeamName);
}
