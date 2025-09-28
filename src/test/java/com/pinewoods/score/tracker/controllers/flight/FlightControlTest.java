package com.pinewoods.score.tracker.controllers.flight;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pinewoods.score.tracker.dto.flight.FlightDTO;
import com.pinewoods.score.tracker.dto.flight.FlightScoreDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

import java.util.List;

import static com.pinewoods.score.tracker.utilities.HttpUtilities.sendRequest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@ActiveProfiles("test")
public class FlightControlTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @LocalServerPort
    int port;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String adminUsername = "test_admin";
    private final String adminPassword = "yolo";
    private final String player1Username = "player1";
    private final String player2Username = "player2";
    private final String playerPassword = "password";
    private final String playerPath = "/players";
    private final String flightPath = "/flights";

    private RestClient restClient;

    @BeforeEach
    void setup() {
        this.restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();

        // ensure teams exist (do nothing if already present)
        jdbcTemplate.update("INSERT INTO teams (name) VALUES (?) ON CONFLICT (name) DO NOTHING", "UNASSIGNED");
        jdbcTemplate.update("INSERT INTO teams (name) VALUES (?) ON CONFLICT (name) DO NOTHING", "Team1");
        jdbcTemplate.update("INSERT INTO teams (name) VALUES (?) ON CONFLICT (name) DO NOTHING", "Team2");

        Long unassignedId = jdbcTemplate.queryForObject("SELECT id FROM teams WHERE name = ?", Long.class, "UNASSIGNED");
        Long team1Id = jdbcTemplate.queryForObject("SELECT id FROM teams WHERE name = ?", Long.class, "Team1");
        Long team2Id = jdbcTemplate.queryForObject("SELECT id FROM teams WHERE name = ?", Long.class, "Team2");

        // ensure admin exists (do nothing for name conflict)
        String adminHash = org.springframework.security.crypto.bcrypt.BCrypt.hashpw(adminPassword, org.springframework.security.crypto.bcrypt.BCrypt.gensalt());
        jdbcTemplate.update(
                "INSERT INTO players (name, password, role, handicap, team_id) VALUES (?, ?, ?, ?, ?) ON CONFLICT (name) DO NOTHING",
                adminUsername, adminHash, "ADMIN", 0.0, unassignedId
        );

        // insert a test player tied to Team1 using the resolved id
        String playerHash = org.springframework.security.crypto.bcrypt.BCrypt.hashpw(playerPassword, org.springframework.security.crypto.bcrypt.BCrypt.gensalt());
        jdbcTemplate.update(
                "INSERT INTO players (name, password, role, handicap, team_id) VALUES (?, ?, ?, ?, ?) ON CONFLICT (name) DO NOTHING",
                "player1", playerHash, "PLAYER", 15.0, team1Id
        );

        // insert a test player tied to Team2 using the resolved id
        jdbcTemplate.update(
                "INSERT INTO players (name, password, role, handicap, team_id) VALUES (?, ?, ?, ?, ?) ON CONFLICT (name) DO NOTHING",
                player2Username, playerHash, "PLAYER", 15.3, team2Id
        );
    }

    @AfterEach
    void cleanup() {
        jdbcTemplate.update("DELETE FROM flight_scores");
        jdbcTemplate.update("DELETE FROM flights");
        jdbcTemplate.update("DELETE FROM players");
        jdbcTemplate.update("DELETE FROM teams");
    }

    @Test
    public void create_flight_success_admin() throws Exception {
        String flightScores = "[" +
                "{\"playerName\":\"player2\",\"score\":36, \"birdies\": 2}," +
                "{\"playerName\":\"player1\",\"score\":32}" +
                "]";

        ResponseEntity<String> flightAdditionResponse = sendRequest(flightPath, flightScores, adminUsername, adminPassword,
                HttpMethod.POST, restClient);

        String playerFlightPath = playerPath + "/player2/flights";
        ResponseEntity<String> playerResponse = sendRequest(playerFlightPath, null, player2Username, playerPassword,
                HttpMethod.GET, restClient);

        // Deserialize creation response
        FlightDTO createdFlight = objectMapper.readValue(
                flightAdditionResponse.getBody(),
                FlightDTO.class
        );

        // Deserialize retrieval response
        List<FlightDTO> flights = objectMapper.readValue(
                playerResponse.getBody(),
                new TypeReference<List<FlightDTO>>() {}
        );

        // Extract the first flight and its scores
        FlightDTO retrievedFlight = flights.getFirst();
        List<FlightScoreDTO> scores = retrievedFlight.flights();

        // Create expected score entries
        FlightScoreDTO expectedScore1 = new FlightScoreDTO("player2", 36, 2);
        FlightScoreDTO expectedScore2 = new FlightScoreDTO("player1", 32, 0);

        assertAll("Flight creation and retrieval",
            // Creation response
            () -> assertNotNull(flightAdditionResponse),
            () -> assertEquals(HttpStatus.CREATED, flightAdditionResponse.getStatusCode()),
            () -> assertNotNull(createdFlight.date(), "Created flight date should not be null"),
            () -> assertEquals(2, createdFlight.flights().size(), "Created flight should contain two scores"),
            () -> assertTrue(createdFlight.flights().contains(expectedScore1), "Created flight should include player2"),
            () -> assertTrue(createdFlight.flights().contains(expectedScore2), "Created flight should include player1"),

            // Retrieval response
            () -> assertNotNull(playerResponse),
            () -> assertEquals(HttpStatus.OK, playerResponse.getStatusCode()),
            () -> assertEquals(createdFlight.date(), retrievedFlight.date(), "Flight dates should match"),
            () -> assertEquals(2, retrievedFlight.flights().size(), "Retrieved flight should contain two scores"),
            () -> assertTrue(retrievedFlight.flights().contains(expectedScore1), "Retrieved flight should include player2"),
            () -> assertTrue(retrievedFlight.flights().contains(expectedScore2), "Retrieved flight should include player1")
        );
    }

    @Test
    public void create_flight_failure_inValidPlayer() throws Exception {
        String flightScores = "[" +
                "{\"playerName\":\"player2\",\"score\":80}" +
                "]";

        ResponseEntity<String> response = sendRequest(flightPath, flightScores, player1Username, playerPassword,
                HttpMethod.POST, restClient);

        assertNotNull(response);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }
}
