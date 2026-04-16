package com.pinewoods.score.tracker.controllers.tournament;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pinewoods.score.tracker.dto.admin.AuthenticationDTOs;
import com.pinewoods.score.tracker.dto.course.CourseDTO;
import com.pinewoods.score.tracker.dto.scoring.ScoreCardDTO;
import com.pinewoods.score.tracker.dto.season.SeasonDTO;
import com.pinewoods.score.tracker.dto.season.TeamStandingDTO;
import com.pinewoods.score.tracker.dto.tournament.TournamentDTO;
import com.pinewoods.score.tracker.entities.admin.Player;
import com.pinewoods.score.tracker.entities.admin.Role;
import com.pinewoods.score.tracker.entities.admin.Team;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static com.pinewoods.score.tracker.utilities.HttpUtilities.sendRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@ActiveProfiles("test")
public class SeasonScoringControllersTest {

    private static final String TEST_COURSE = "testCourse";
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired private BCryptPasswordEncoder passwordEncoder;
    @LocalServerPort
    int port;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private RestClient restClient;
    private final String TOURNAMENT_PATH = "/tournaments";
    private final String SEASON_PATH = "/seasons";

    @BeforeEach
    void setup() {
        this.restClient = RestClient.builder().baseUrl("http://localhost:" + port).build();

        // 1. Setup Teams
        Long readTeamId = jdbcTemplate.queryForObject("INSERT INTO teams (name) VALUES (?)RETURNING id", Long.class, "RedTeam");
        Long blueTeamId = jdbcTemplate.queryForObject("INSERT INTO teams (name) VALUES (?)RETURNING id", Long.class, "BlueTeam");

        // 2. Setup Admin for Auth
        String adminHash = passwordEncoder.encode("admin_pass");
        jdbcTemplate.update("INSERT INTO players (name, password, role, handicap, team_id) VALUES (?, ?, ?, ?, ?)",
                "admin", adminHash, "ADMIN", 0.0, readTeamId);

        // 3. Setup 3 Test Players
        String pHash = passwordEncoder.encode("pass");
        jdbcTemplate.update("INSERT INTO players (name, password, role, handicap, team_id) VALUES (?, ?, 'PLAYER', ?, ?)",
                "Pro_Phil", pHash, 2.0, readTeamId); // Team Red
        jdbcTemplate.update("INSERT INTO players (name, password, role, handicap, team_id) VALUES (?, ?, 'PLAYER', ?, ?)",
                "Average_Joe", pHash, 18.0, readTeamId); // Team Red
        jdbcTemplate.update("INSERT INTO players (name, password, role, handicap, team_id) VALUES (?, ?, 'PLAYER', ?, ?)",
                "High_Harry", pHash, 36.0, blueTeamId); // Team Blue
    }

    @Test
    void testStablefordTournamentFlow() throws Exception {
        String token = loginAndGetToken("admin", "admin_pass");

        // --- STEP 1: Start Tournament ---
        List<Integer> pars = Collections.nCopies(18, 4); // All Par 4s
        List<Integer> indexes = IntStream.rangeClosed(1, 18).boxed().toList(); // 1 to 18
        Map<Integer, Integer> pointsMap = Map.of(
                -1, 3,
                0, 2,
                1, 1
        );

        double handicapMultiplier = 1.0;

        ResponseEntity<String> createSeasonResp = sendRequest(
                SEASON_PATH + "/start",
                "Open Season",
                token,
                HttpMethod.POST, restClient
        );

        assertEquals(HttpStatus.CREATED, createSeasonResp.getStatusCode());
        String seasonName = objectMapper.readValue(createSeasonResp.getBody(), SeasonDTO.class).name();

        final String tourName = "Open Championship";
        final String strategyType = "STABLEFORD";

        // create the test course
        CourseDTO courseDTO = CourseDTO.builder()
                .name(TEST_COURSE)
                .pars(pars)
                .indexes(indexes)
                .build();

        sendRequest("/courses", objectMapper.writeValueAsString(courseDTO), token, HttpMethod.POST, restClient);

        TournamentController.TournamentCreateRequest tournamentCreateRequest =
                new TournamentController.TournamentCreateRequest(
                tourName, seasonName, strategyType, TEST_COURSE, pointsMap, handicapMultiplier
        );

        ResponseEntity<String> startRes = sendRequest(TOURNAMENT_PATH + "/start",
                objectMapper.writeValueAsString(tournamentCreateRequest), token,
                HttpMethod.POST, restClient);

        Long tournamentId = objectMapper.readValue(startRes.getBody(), TournamentDTO.class).id();
        assertEquals(HttpStatus.CREATED, startRes.getStatusCode());

        // see if the entity is created
        ResponseEntity<String> tournamentCreatedResponse = sendRequest(TOURNAMENT_PATH + "/" + tourName,
                null, token, HttpMethod.GET, restClient);
        assertEquals(HttpStatus.OK, tournamentCreatedResponse.getStatusCode());
        List<TournamentDTO> resultList = objectMapper.readValue(
                tournamentCreatedResponse.getBody(),
                new TypeReference<List<TournamentDTO>>() {}
        );

        TournamentDTO tournamentDTO = resultList.getFirst();
        assertEquals(tourName, tournamentDTO.name(), "Tournament name should be " + tourName);

        // --- STEP 2: Submit Scores for a Flight ---
        // Let's give them all 4s (Gross Par) on all holes
        List<Integer> defaultTestScores = Collections.nCopies(18, 4);
        List<Integer> proScores = new java.util.ArrayList<>(List.copyOf(defaultTestScores));
        proScores.set(4, 3);
        proScores.set(8, 3);

        List<Integer>  highHandicapScores = new java.util.ArrayList<>(List.copyOf(defaultTestScores));
        highHandicapScores.set(2, 5);
        highHandicapScores.set(9, 6);



        // We fetch players from DB to get real IDs for the DTO
        Player proPlayer = getPlayerByName("Pro_Phil");
        Player averagePlayer = getPlayerByName("Average_Joe");
        Player highHandicapPlayer = getPlayerByName("High_Harry");

        List<ScoreCardDTO> cards = List.of(
                new ScoreCardDTO(proPlayer.toDTO(), proScores),
                new ScoreCardDTO(averagePlayer.toDTO(), defaultTestScores),
                new ScoreCardDTO(highHandicapPlayer.toDTO(), highHandicapScores)
        );

        ResponseEntity<String> flightSubmission = sendRequest(
                TOURNAMENT_PATH + "/" + tournamentId + "/flights",
                objectMapper.writeValueAsString(cards), token,
                HttpMethod.POST, restClient);

        assertEquals(HttpStatus.NO_CONTENT, flightSubmission.getStatusCode());

        // --- STEP 3: Finalize Tournament ---
        sendRequest(
                TOURNAMENT_PATH + "/" + tournamentId + "/end",
                "", token,
                HttpMethod.POST, restClient
        );

        token = loginAndGetToken("admin", "admin_pass");

        ResponseEntity<String> tournamentResult = sendRequest(
                TOURNAMENT_PATH + "/" + seasonName + "/" + tourName,
                null, token,
                HttpMethod.GET, restClient
        );

        assertEquals(HttpStatus.OK, tournamentResult.getStatusCode());
        TournamentDTO tournament = objectMapper.readValue(tournamentResult.getBody(), TournamentDTO.class);
        Map<Long, Integer> awards = tournament.awards();
        Map<Long, Integer> expectedAwards = Map.of(
                averagePlayer.getId(), 1,
                highHandicapPlayer.getId(), 2,
                proPlayer.getId(), 3
        );

        assertEquals(expectedAwards, awards, "Awards should be " + expectedAwards);

        // ------- Step 4. End season and get standings -------------
        ResponseEntity<String> seasonEndResponse = sendRequest(
                SEASON_PATH + "/" + seasonName + "/finish",
                "", token,
                HttpMethod.POST, restClient
        );

        assertEquals(HttpStatus.OK, seasonEndResponse.getStatusCode());
        List<TeamStandingDTO> teamStanding = objectMapper.readValue(seasonEndResponse.getBody(),
                new TypeReference<List<TeamStandingDTO>>() {});
        List<TeamStandingDTO> expectedTeamStanding = objectMapper.readValue(
                """
                        [{"teamName":"RedTeam","points":183,"wins":2,"losses":0,"draws":0,"birdies":0},
                        {"teamName":"BlueTeam","points":66,"wins":1,"losses":0,"draws":0,"birdies":0}]
                        """, new TypeReference<List<TeamStandingDTO>>() {});

        assertEquals(expectedTeamStanding, teamStanding, "Team standings should be " + expectedTeamStanding);

//
//        // --- STEP 3: Finalize Tournament ---
//        restClient.post()
//                .uri("/api/v1/tournaments/" + tournamentId + "/end")
//                .header("Authorization", "Bearer " + token)
//                .retrieve().toBodilessEntity();
//
//        // --- STEP 4: Assertions (Standings Update) ---
//        // High_Harry should be 1st (most points due to high handicap), Average_Joe 2nd, Pro_Phil 3rd
//        Integer blueTeamPoints = jdbcTemplate.queryForObject(
//                "SELECT points FROM team_standings ts JOIN teams t ON ts.team_id = t.id WHERE t.name = 'BlueTeam'", Integer.class);
//        Integer redTeamPoints = jdbcTemplate.queryForObject(
//                "SELECT points FROM team_standings ts JOIN teams t ON ts.team_id = t.id WHERE t.name = 'RedTeam'", Integer.class);
//
//        // Blue team has Harry (1st place = 100 points)
//        assertEquals(100, blueTeamPoints, "Blue team should have 100 points from Harry's win");
//
//        // Red team has Joe (2nd = 66) and Phil (3rd = 33) = 99 points
//        assertEquals(99, redTeamPoints, "Red team should have 99 points from 2nd and 3rd place");
    }

    // =================== Utilities ====================
    private String loginAndGetToken(String username, String password) throws JsonProcessingException {
        String loginJson = objectMapper.writeValueAsString(new AuthenticationDTOs.AuthRequestDTO(username, password));

        ResponseEntity<String> response = restClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginJson)
                .retrieve()
                .toEntity(String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        AuthenticationDTOs.AuthResponseDTO authResponse = objectMapper.readValue(response.getBody(), AuthenticationDTOs.AuthResponseDTO.class);
        return authResponse.token();

    }

    private Player getPlayerByName(String name) {
        return jdbcTemplate
                .queryForObject("SELECT * FROM players WHERE name = ?", (rs, rowNum) -> {
                    Player player = new Player();
                    player.setId(rs.getLong("id"));
                    player.setName(rs.getString("name"));
                    player.setPassword(rs.getString("password"));
                    player.setRole(Role.valueOf(rs.getString("role")));
                    player.setHandicap(rs.getDouble("handicap"));
                    player.setTeam(getTeamById(rs.getLong("team_id")));
                    return player;
                }, name);
    }

    private Team getTeamById(Long id) {
        return jdbcTemplate.queryForObject("SELECT * FROM teams WHERE id = ?", (rs, rowNum) -> {
            Team team = new Team();
            team.setId(rs.getLong("id"));
            team.setName(rs.getString("name"));
            return team;
        }, id);
    }
}
