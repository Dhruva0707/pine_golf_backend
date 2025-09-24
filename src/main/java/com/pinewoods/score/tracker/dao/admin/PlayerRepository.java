package com.pinewoods.score.tracker.dao.admin;

import com.pinewoods.score.tracker.entities.admin.Player;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

public interface PlayerRepository extends JpaRepository<Player, Integer> {
    Optional<Player> findByName(String name);
    List<Player> findByTeam_Name(String teamName);

    boolean existsByName(String name);
}
