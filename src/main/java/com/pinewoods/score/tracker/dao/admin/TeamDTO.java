package com.pinewoods.score.tracker.dao.admin;

import java.util.List;
import java.util.Optional;

public class TeamDTO {
    Optional<Team> findByName(String name);
    List<Team> findAll();
}
