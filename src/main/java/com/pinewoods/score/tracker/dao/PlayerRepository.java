package com.pinewoods.score.tracker.dao;

import com.pinewoods.score.tracker.entities.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {
    Optional<Player> findByName(String name);
}
