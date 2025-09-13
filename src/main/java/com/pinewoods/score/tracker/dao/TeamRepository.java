package com.pinewoods.score.tracker.dao;

import com.pinewoods.score.tracker.entities.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {
    Optional<Team> findByName(String name);
}
