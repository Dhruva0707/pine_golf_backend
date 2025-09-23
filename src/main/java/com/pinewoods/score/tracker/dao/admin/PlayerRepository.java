package com.pinewoods.score.tracker.dao.admin;

import java.util.List;
import java.util.Optional;

public class PlayerRepository {
    Optional<Player> findByName(String name);
    List<Player> findAll();
    List<Player> findByTeam_Name(String teamName);
}
