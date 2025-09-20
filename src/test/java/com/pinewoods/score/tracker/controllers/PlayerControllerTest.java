package com.pinewoods.score.tracker.controllers;

import com.pinewoods.score.tracker.entities.Player;
import com.pinewoods.score.tracker.entities.Role;
import com.pinewoods.score.tracker.entities.Team;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.List;



@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
class PlayerControllerTest {

    @Autowired
    private WebClient client;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private List<Long> getIds() {
        return jdbcTemplate.queryForList("SELECT id FROM players", Long.class);
    }

    private Team getTeamById(Long id) {
        return jdbcTemplate.queryForObject("SELECT * From teams WHERE id = ?",
                (rs, row) -> new Team())
    }

    private Player getPlayerById(Long id) {
        return jdbcTemplate.queryForObject("SELECT * FROM players WHERE id = ?",
                (rs, row) -> new Player(rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("password"),
                        rs.getInt("handicap"),
                        Role.valueOf(rs.getString("role")),
                        ),
                id);
    }

    @Test
    void createPlayer() {

    }

    @Test
    void changePassword() {
    }

    @Test
    void deletePlayer() {
    }
}