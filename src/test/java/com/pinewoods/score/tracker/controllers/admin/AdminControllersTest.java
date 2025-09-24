// java
package com.pinewoods.score.tracker.controllers.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pinewoods.score.tracker.dto.admin.PlayerDTO;
import com.pinewoods.score.tracker.dto.admin.TeamDTO;
import com.pinewoods.score.tracker.entities.admin.Player;
import com.pinewoods.score.tracker.entities.admin.Role;
import com.pinewoods.score.tracker.entities.admin.Team;
import com.pinewoods.score.tracker.services.admin.PlayerService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import static com.pinewoods.score.tracker.utilities.HttpUtilities.sendRequest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@ActiveProfiles("test")
class AdminControllersTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @LocalServerPort
    int port;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String adminUsername = "test_admin";
    private final String adminPassword = "yolo";
    private final String player1Username = "player1";
    private final String player1Password = "password";
    private final String path = "/players";

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
        String adminHash = org.springframework.security.crypto.bcrypt.BCrypt.hashpw("yolo", org.springframework.security.crypto.bcrypt.BCrypt.gensalt());
        jdbcTemplate.update(
            "INSERT INTO players (name, password, role, handicap, team_id) VALUES (?, ?, ?, ?, ?) ON CONFLICT (name) DO NOTHING",
            "test_admin", adminHash, "ADMIN", 0.0, unassignedId
        );

        // insert a test player tied to Team1 using the resolved id
        String playerHash = org.springframework.security.crypto.bcrypt.BCrypt.hashpw("password", org.springframework.security.crypto.bcrypt.BCrypt.gensalt());
        jdbcTemplate.update(
            "INSERT INTO players (name, password, role, handicap, team_id) VALUES (?, ?, ?, ?, ?) ON CONFLICT (name) DO NOTHING",
            "player1", playerHash, "PLAYER", 15.0, team1Id
        );

        // insert a test player tied to Team2 using the resolved id
        jdbcTemplate.update(
            "INSERT INTO players (name, password, role, handicap, team_id) VALUES (?, ?, ?, ?, ?) ON CONFLICT (name) DO NOTHING",
            "player2", playerHash, "PLAYER", 15.3, team2Id
        );
    }

    @AfterEach
    void cleanup() {
        jdbcTemplate.update("DELETE FROM players");
        jdbcTemplate.update("DELETE FROM teams");
    }

    /****************************************************** PLAYER TESTS ******************************************************/
    // ------------------- Create test --------------------
    @Test
    void createPlayer_success_teamProvided() throws Exception {
        String newPlayerJson = """
        {
            "name": "newPlayer",
            "password": "pass123",
            "handicap": 10,
            "role": "USER",
            "team": "Team1"
        }
    """;

        ResponseEntity<String> response = sendRequest(path, newPlayerJson, adminUsername, adminPassword,
            HttpMethod.POST, restClient);

        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());

        PlayerDTO responseDTO = objectMapper.readValue(response.getBody(), PlayerDTO.class);

        Player persistedPlayer = getPlayerByName("newPlayer");
        PlayerDTO persistedPlayerDTO = PlayerService.createPlayerDTO(persistedPlayer);
        PlayerDTO expectedDTO = new PlayerDTO("newPlayer", "Team1", 10.0);


        assertEquals(expectedDTO, persistedPlayerDTO, "The created player should match the expected values.");
        assertEquals(expectedDTO, responseDTO, "The created player should match the expected values.");
    }

    @Test
    void createPlayer_success_teamNotProvided() throws Exception {
        String newPlayerJson = """
        {
            "name": "newPlayer",
            "password": "pass123",
            "handicap": 10,
            "role": "USER"
        }
    """;

        ResponseEntity<String> response = sendRequest(path, newPlayerJson, adminUsername, adminPassword,
            HttpMethod.POST, restClient);

        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());

        PlayerDTO responseDTO = objectMapper.readValue(response.getBody(), PlayerDTO.class);

        Player persistedPlayer = getPlayerByName("newPlayer");
        PlayerDTO persistedPlayerDTO = PlayerService.createPlayerDTO(persistedPlayer);
        PlayerDTO expectedDTO = new PlayerDTO("newPlayer", "UNASSIGNED", 10.0);


        assertEquals(expectedDTO, persistedPlayerDTO, "The created player should match the expected values.");
        assertEquals(expectedDTO, responseDTO, "The created player should match the expected values.");
    }

    @Test
    void createPlayer_failure_unauthorized() {
        String newPlayerJson = """
            {
                "name": "test_player",
                "password": "pass123",
                "handicap": 10,
                "role": "USER",
                "teamName": "Team1"
            }
        """;

        ResponseEntity<String> response = sendRequest(path, newPlayerJson, player1Username, player1Password,
            HttpMethod.POST, restClient);

        assertNotNull(response);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void createPlayer_failure_duplicateName() {
        String newPlayerJson = """
            {
                "name": "player1",
                "password": "pass123",
                "handicap": 10,
                "role": "USER",
                "teamName": "Team1"
            }
        """;

        ResponseEntity<String> response = sendRequest(path, newPlayerJson, adminUsername, adminPassword,
            HttpMethod.POST, restClient);

        assertNotNull(response);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void createPlayer_failure_unknownTeam() {
        String newPlayerJson = """
            {
                "name": "test_player",
                "password": "pass123",
                "handicap": 10,
                "role": "USER",
                "team": "UnknownTeam"
            }
        """;

        ResponseEntity<String> response = sendRequest(path, newPlayerJson, adminUsername, adminPassword,
            HttpMethod.POST, restClient);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }


    // ------------------- Read tests --------------------
    @Test
    void getAllPlayers_success_adminCall() {
        ResponseEntity<String> response = sendRequest(path, null, adminUsername, adminPassword,
            HttpMethod.GET, restClient);

        assertAll(
            () -> assertNotNull(response),
            () -> assertEquals(HttpStatus.OK, response.getStatusCode()),
            () -> {
                assertNotNull(response.getBody());
                PlayerDTO[] players = objectMapper.readValue(response.getBody(), PlayerDTO[].class);
                assertEquals(3, players.length, "There should be 3 players returned.");
            }
        );
    }

    @Test
    void getAllPlayers_success_playerCall() {
        ResponseEntity<String> response = sendRequest(path, null, player1Username, player1Password,
            HttpMethod.GET, restClient);

        assertAll(
            () -> assertNotNull(response),
            () -> assertEquals(HttpStatus.OK, response.getStatusCode()),
            () -> {
                assertNotNull(response.getBody());
                PlayerDTO[] players = objectMapper.readValue(response.getBody(), PlayerDTO[].class);
                assertEquals(3, players.length, "There should be 3 players returned.");
            }
        );
    }

    @Test
    void getPlayerByName_success_existingPlayer() {
        ResponseEntity<String> response = sendRequest(path + "/player1", null, adminUsername, adminPassword,
            HttpMethod.GET, restClient);

        assertAll(
            () -> assertNotNull(response),
            () -> assertEquals(HttpStatus.OK, response.getStatusCode()),
            () -> {
                assertNotNull(response.getBody());
                PlayerDTO player = objectMapper.readValue(response.getBody(), PlayerDTO.class);
                PlayerDTO expectedPlayer = new PlayerDTO("player1", "Team1", 15.0);
                assertEquals(expectedPlayer, player, "The returned player should match the expected values.");
            }
        );
    }

    @Test
    void getPlayerByName_success_differentExistingPlayer() {
        ResponseEntity<String> response = sendRequest(path + "/player2", null, player1Username, player1Password,
            HttpMethod.GET, restClient);

        assertAll(
            () -> assertNotNull(response),
            () -> assertEquals(HttpStatus.OK, response.getStatusCode()),
            () -> {
                assertNotNull(response.getBody());
                PlayerDTO player = objectMapper.readValue(response.getBody(), PlayerDTO.class);
                PlayerDTO expectedPlayer = new PlayerDTO("player2", "Team2", 15.3);
                assertEquals(expectedPlayer, player, "The returned player should match the expected values.");
            }
        );
    }

    @Test
    void getPlayerByName_failure_unknownPlayer() {
        ResponseEntity<String> response = sendRequest(path + "/unknownPlayer", null, adminUsername, adminPassword, HttpMethod.GET, restClient);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getPlayerByName_failure_unauthorized() {
        ResponseEntity<String> response = sendRequest(path + "/player1", null, "invalidUser", "invalidPass", HttpMethod.GET, restClient);

        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    // ------------------- Update tests --------------------

    // Update the team of a player
    @Test
    void updatePlayerTeam_success() throws Exception {
        String updateTeamJson = "Team2"; // JSON string for the new team name
        ResponseEntity<String> response = sendRequest(path + "/player1/team", updateTeamJson, adminUsername, adminPassword,
            HttpMethod.PUT, restClient);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        PlayerDTO responseDTO = objectMapper.readValue(response.getBody(), PlayerDTO.class);
        Player updatedPlayer = getPlayerByName("player1");
        Team expectedTeam = getTeamByName("Team2");
        PlayerDTO expectedDTO = new PlayerDTO("player1", expectedTeam.getName(), 15.0);
        PlayerDTO updatedPlayerDTO = PlayerService.createPlayerDTO(updatedPlayer);
        assertEquals(expectedDTO, updatedPlayerDTO, "The updated player should match the expected values.");
        assertEquals(expectedDTO, responseDTO, "The updated player should match the expected values.");
    }

    @Test
    void updatePlayerTeam_failure_unauthorized_playerCall() {
        String updateTeamJson = "Team2";
        ResponseEntity<String> response = sendRequest(path + "/player2/team", updateTeamJson,
            player1Username, player1Password, HttpMethod.PUT, restClient);

        assertNotNull(response);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void updatePlayerTeam_failure_unauthorized_invalidCredentials() {
        String updateTeamJson = "\"Team2\"";
        ResponseEntity<String> response = sendRequest(path + "/player1/team", updateTeamJson,
            "invalidUser", "invalidPass", HttpMethod.PUT, restClient);

        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void updatePlayerTeam_failure_unknownPlayer() {
        String updateTeamJson = "Team1";
        ResponseEntity<String> response = sendRequest(path + "/unknownPlayer/team", updateTeamJson,
            adminUsername, adminPassword, HttpMethod.PUT, restClient);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void updatePlayerTeam_failure_unknownTeam() {
        String updateTeamJson = "UnknownTeam";
        ResponseEntity<String> response = sendRequest(path + "/player1/team", updateTeamJson,
            adminUsername, adminPassword, HttpMethod.PUT, restClient);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // Update the handicap of a player

    @Test
    void updatePlayerHandicap_success() throws Exception {
        String updateHandicapJson = "20";
        ResponseEntity<String> response = sendRequest(path + "/player1/handicap", updateHandicapJson,
            adminUsername, adminPassword, HttpMethod.PUT, restClient);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        PlayerDTO responseDTO = objectMapper.readValue(response.getBody(), PlayerDTO.class);

        Player updatedPlayer = getPlayerByName("player1");
        PlayerDTO expectedDTO = new PlayerDTO("player1", updatedPlayer.getTeam().getName(), 20.0);
        PlayerDTO updatedPlayerDTO = PlayerService.createPlayerDTO(updatedPlayer);

        assertEquals(expectedDTO, updatedPlayerDTO, "The updated player should match the expected values.");
        assertEquals(expectedDTO, responseDTO, "The updated player should match the expected values.");
    }

    @Test
    void updatePlayerHandicap_failure_unauthorized_playerCall() {
        String updateHandicapJson = "20";
        ResponseEntity<String> response = sendRequest(path + "/player2/handicap", updateHandicapJson,
            player1Username, player1Password, HttpMethod.PUT, restClient);

        assertNotNull(response);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void updatePlayerHandicap_failure_unauthorized_invalidCredentials() {
        String updateHandicapJson = "20";
        ResponseEntity<String> response = sendRequest(path + "/player1/handicap", updateHandicapJson,
            "invalidUser", "invalidPass", HttpMethod.PUT, restClient);

        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void updatePlayerHandicap_failure_unknownPlayer() {
        String updateHandicapJson = "10";
        ResponseEntity<String> response = sendRequest(path + "/unknownPlayer/handicap", updateHandicapJson,
            adminUsername, adminPassword, HttpMethod.PUT, restClient);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // Update the password of a player

    @Test
    void updatePlayerPassword_success_adminCall() throws Exception {
        String newPassword = "newAdminSetPass";

        // capture original hash
        Player before = getPlayerByName("player1");
        String originalHash = before.getPassword();

        ResponseEntity<String> response = sendRequest(path + "/player1/password", newPassword,
            adminUsername, adminPassword, HttpMethod.PUT, restClient);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        PlayerDTO responseDTO = objectMapper.readValue(response.getBody(), PlayerDTO.class);

        Player updatedPlayer = getPlayerByName("player1");
        PlayerDTO expectedDTO = PlayerService.createPlayerDTO(updatedPlayer);
        PlayerDTO updatedPlayerDTO = PlayerService.createPlayerDTO(updatedPlayer);

        // hash changed and matches new password
        assertNotEquals(originalHash, updatedPlayer.getPassword(), "Password hash should have changed.");
        assertEquals(expectedDTO, updatedPlayerDTO, "The updated player DTO should match persisted values.");
        assertEquals(expectedDTO, responseDTO, "Response DTO should match persisted values.");
        assertTrue(org.springframework.security.crypto.bcrypt.BCrypt.checkpw(newPassword, updatedPlayer.getPassword()),
            "Stored hash should verify the new raw password.");
    }

    @Test
    void updatePlayerPassword_success_selfCall() throws Exception {
        String newPassword = "myNewPass123";

        Player before = getPlayerByName("player1");
        String originalHash = before.getPassword();

        ResponseEntity<String> response = sendRequest(path + "/player1/password", newPassword,
            player1Username, player1Password, HttpMethod.PUT, restClient);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        PlayerDTO responseDTO = objectMapper.readValue(response.getBody(), PlayerDTO.class);

        Player updatedPlayer = getPlayerByName("player1");
        PlayerDTO expectedDTO = PlayerService.createPlayerDTO(updatedPlayer);

        assertNotEquals(originalHash, updatedPlayer.getPassword(), "Password hash should have changed.");
        assertEquals(expectedDTO, responseDTO, "Response DTO should match persisted values.");
        assertTrue(org.springframework.security.crypto.bcrypt.BCrypt.checkpw(newPassword, updatedPlayer.getPassword()),
            "Stored hash should verify the new raw password.");
    }

    @Test
    void updatePlayerPassword_failure_unauthorized_otherPlayerCall() {
        String updatePasswordJson = "hackedPass";

        ResponseEntity<String> response = sendRequest(path + "/player2/password", updatePasswordJson,
            player1Username, player1Password, HttpMethod.PUT, restClient);

        assertNotNull(response);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void updatePlayerPassword_failure_unauthorized_invalidCredentials() {
        String updatePasswordJson = "doesntMatter";

        ResponseEntity<String> response = sendRequest(path + "/player1/password", updatePasswordJson,
            "invalidUser", "invalidPass", HttpMethod.PUT, restClient);

        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    // ------------------- Delete tests --------------------

    @Test
    void deletePlayer_success_adminCall() {
        ResponseEntity<String> response = sendRequest(path + "/player1", null,
            adminUsername, adminPassword, HttpMethod.DELETE, restClient);

        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM players WHERE name = ?", Integer.class, "player1");
        assertNotNull(count);
        assertEquals(0, count.intValue(), "Player row should be removed from the database.");
    }

    @Test
    void deletePlayer_failure_unauthorized_playerCall() {
        ResponseEntity<String> response = sendRequest(path + "/player2", null,
            player1Username, player1Password, HttpMethod.DELETE, restClient);

        assertNotNull(response);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void deletePlayer_failure_unknownPlayer() {
        ResponseEntity<String> response = sendRequest(path + "/unknownPlayer", null,
            adminUsername, adminPassword, HttpMethod.DELETE, restClient);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    /****************************************************** TEAM TESTS ******************************************************/

    // -------------------- Create tests --------------------

    @Test
    void createTeam_success_adminCall() throws Exception {
        String teamPath = "/teams";
        String newTeamName = "NewTeam";

        ResponseEntity<String> response = sendRequest(teamPath, newTeamName, adminUsername, adminPassword,
            HttpMethod.POST, restClient);

        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());

        TeamDTO responseDTO =
            objectMapper.readValue(response.getBody(), TeamDTO.class);

        TeamDTO expectedDTO =
            new TeamDTO(newTeamName, java.util.Collections.emptyList());

        assertEquals(expectedDTO, responseDTO, "Created team DTO should match expected values.");

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM teams WHERE name = ?", Integer.class, newTeamName);
        assertNotNull(count);
        assertEquals(1, count.intValue(), "Team row should be present in the database.");
    }

    @Test
    void createTeam_failure_unauthorized_nonAdmin() {
        String teamPath = "/teams";
        String newTeamName = "ShouldNotCreate";

        ResponseEntity<String> response = sendRequest(teamPath, newTeamName, player1Username, player1Password,
            HttpMethod.POST, restClient);

        assertNotNull(response);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void createTeam_failure_duplicateName() {
        String teamPath = "/teams";
        String existing = "Team1";

        ResponseEntity<String> response = sendRequest(teamPath, existing, adminUsername, adminPassword,
            HttpMethod.POST, restClient);

        assertNotNull(response);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    // -------------------- Read tests --------------------

    @Test
    void getTeam_success_existingTeam() throws Exception {
        String teamPath = "/teams/Team1";

        ResponseEntity<String> response = sendRequest(teamPath, null, adminUsername, adminPassword,
            HttpMethod.GET, restClient);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        TeamDTO teamDTO =
            objectMapper.readValue(response.getBody(), TeamDTO.class);

        Team team = getTeamByName("Team1");
        List<Player> expectedPlayers =  new ArrayList<>();
        expectedPlayers.add(getPlayerByName("player1"));
        team.setPlayers(expectedPlayers);
        TeamDTO expected =
            new TeamDTO(team.getName(),
                team.getPlayers().stream().map(PlayerService::createPlayerDTO).toList());

        assertEquals(expected, teamDTO, "Returned team should match expected values.");
    }

    @Test
    void getTeam_failure_unknownTeam() {
        String teamPath = "/teams/UnknownTeam";

        ResponseEntity<String> response = sendRequest(teamPath, null, adminUsername, adminPassword,
            HttpMethod.GET, restClient);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getAllTeams_success_adminCall() throws Exception {
        String teamPath = "/teams";

        ResponseEntity<String> response = sendRequest(teamPath, null, adminUsername, adminPassword,
            HttpMethod.GET, restClient);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        TeamDTO[] teams =
            objectMapper.readValue(response.getBody(), TeamDTO[].class);

        // Setup ensures 3 teams: UNASSIGNED, Team1, Team2
        assertEquals(3, teams.length, "There should be 3 teams returned.");
    }

    @Test
    void getAllTeams_success_playerCall() throws Exception {
        String teamPath = "/teams";

        ResponseEntity<String> response = sendRequest(teamPath, null, player1Username, player1Password,
            HttpMethod.GET, restClient);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        TeamDTO[] teams =
            objectMapper.readValue(response.getBody(), TeamDTO[].class);

        assertEquals(3, teams.length, "There should be 3 teams returned.");
    }

    // -------------------- Update tests --------------------

    @Test
    void renameTeam_success_adminCall() throws Exception {
        String teamPath = "/teams/Team1/rename";
        String newName = "Team1Renamed";

        ResponseEntity<String> response = sendRequest(teamPath, newName, adminUsername, adminPassword,
            HttpMethod.PUT, restClient);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        TeamDTO responseDTO =
            objectMapper.readValue(response.getBody(), TeamDTO.class);

        // DB should reflect renamed team
        Team renamed = getTeamByName(newName);

        List<Player> expectedPlayers =  new ArrayList<>();
        expectedPlayers.add(getPlayerByName("player1"));
        renamed.setPlayers(expectedPlayers);
        TeamDTO expectedDTO =
            new TeamDTO(renamed.getName(),
                renamed.getPlayers().stream().map(PlayerService::createPlayerDTO).toList());

        assertEquals(expectedDTO, responseDTO, "Rename should return the updated team DTO.");
    }

    @Test
    void renameTeam_failure_unauthorized() {
        String teamPath = "/teams/Team1/rename";
        String newName = "ShouldNotRename";

        ResponseEntity<String> response = sendRequest(teamPath, newName, player1Username, player1Password,
            HttpMethod.PUT, restClient);

        assertNotNull(response);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    // -------------------- Delete tests --------------------

    @Test
    void deleteTeam_success_adminCall() {
        String teamPath = "/teams/Team2";

        ResponseEntity<String> response = sendRequest(teamPath, null, adminUsername, adminPassword,
            HttpMethod.DELETE, restClient);

        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM teams WHERE name = ?", Integer.class, "Team2");
        assertNotNull(count);
        assertEquals(0, count.intValue(), "Team row should be removed from the database.");

        // player2 should be reassigned to UNASSIGNED
        Player player2 = getPlayerByName("player2");
        assertEquals("UNASSIGNED", player2.getTeam().getName(), "Player from deleted team should be reassigned to UNASSIGNED.");
    }

    @Test
    void deleteTeam_failure_unknownTeam() {
        String teamPath = "/teams/UnknownTeam";

        ResponseEntity<String> response = sendRequest(teamPath, null, adminUsername, adminPassword,
            HttpMethod.DELETE, restClient);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void deleteTeam_failure_unauthorized() {
        String teamPath = "/teams/Team1";

        ResponseEntity<String> response = sendRequest(teamPath, null, player1Username, player1Password,
            HttpMethod.DELETE, restClient);

        assertNotNull(response);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }



    /****************************************************** UTILITIES ******************************************************/
    private Player getPlayerByName(String name) {
        return jdbcTemplate.queryForObject("SELECT * FROM players WHERE name = ?", (rs, rowNum) -> {
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

    private Team getTeamByName(String name) {
        return jdbcTemplate.queryForObject("SELECT * FROM teams WHERE name = ?", (rs, rowNum) -> {
            Team team = new Team();
            team.setId(rs.getLong("id"));
            team.setName(rs.getString("name"));
            return team;
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