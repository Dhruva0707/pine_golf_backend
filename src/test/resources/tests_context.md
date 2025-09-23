# Test Context: Golf Club Tracker

## Overview
This document provides the context and guidelines for writing test cases for the **Golf Club Tracker** application. The goal is to ensure that tests are reliable, maintainable, and optimized for parallel execution while preventing any impact on the actual database.

---

## General Guidelines
1. **Transactional Tests**:
    - All test cases must be annotated with `@Transactional` to ensure that changes made during the test are rolled back after execution. This prevents tests from affecting the actual data.

2. **Full Spring Setup**:
    - Use `@SpringBootTest` to load the full application context. This ensures that all components, services, and repositories are available during testing.

3. **Database Validation**:
    - Use `JdbcTemplate` to validate database actions performed during the test. This ensures that the database state matches the expected outcomes.

4. **Default Entities**:
    - If default entities (e.g., an Admin player or a default team) are required for the tests, they should be created in the `@BeforeEach` or `@BeforeAll` section to ensure consistency across tests.

5. **Parallel Execution**:
    - Tests should be written to support parallel execution. Avoid shared mutable state and ensure that each test is independent of others.

6. **WebClient for HTTP Requests**:
    - Use `WebClient` to perform HTTP requests in tests instead of `MockMvc`. This provides a more realistic simulation of client-server communication.

7. **Test Naming**:
    - naming conventions should be concise, clear and consistent:
        - `createPlayer_success` for successful player creation.
        - `createPlayer_failure_DuplicateName` for failure due to duplicate names.
        - `createPlayer_failure_unauthorized` for unauthorized access.
        - `deletePlayer_failure_notFound()` when player is not found
    - Use underscores to separate different parts of the test name for better readability.
    - Include the method being tested, the expected outcome, and any specific conditions.

---

## Test Setup
1. **Annotations**:
    - Use `@SpringBootTest` for loading the application context.
    - Use `@Transactional` to ensure data rollback after each test.

2. **Default Data Setup**:
    - Use `@BeforeEach` or `@BeforeAll` to insert default entities like an Admin player or a default team into the database.

3. **JdbcTemplate Utility Methods**:
    - Create utility methods to query the database for validation. For example:
        - `getIds()`: Retrieve all player IDs from the database.
        - `getPlayerById(Long id)`: Retrieve a player entity by its ID.
        - `getTeamById(Long id)`: Retrieve a team entity by its ID.

4. **WebClient Configuration**:
    - Use `WebClient` to perform HTTP requests in tests. Configure it to point to the application's base URL.

---

## Example Test Structure

### Test Class Annotations
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
class PlayerControllerTest {
    // Test methods go here
}
```

### Example utility methods
```java
public class PlayerTest {
   private List<Long> getIds() {
      return jdbcTemplate.queryForList("SELECT id FROM players", Long.class);
   }

   private Player getPlayerById(Long id) {
      return jdbcTemplate.queryForObject("SELECT * FROM players WHERE id = ?",
              (rs, row) -> new Player(rs.getLong("id"),
                      rs.getString("name"),
                      rs.getString("password"),
                      rs.getInt("handicap"),
                      Role.valueOf(rs.getString("role")),
                      null),
              id);
   }

   private Team getTeamById(Long id) {
      return jdbcTemplate.queryForObject("SELECT * FROM teams WHERE id = ?",
              (rs, row) -> new Team(rs.getLong("id"), rs.getString("name")),
              id);
   }
}
```

### Example default test setup
```java
public class PlayerTest {
   @BeforeEach
   void setupDefaultEntities() {
      jdbcTemplate.update("INSERT INTO teams (name) VALUES (?)", "Default Team");
      jdbcTemplate.update("INSERT INTO players (name, password, role, handicap, team_id) VALUES (?, ?, ?, ?, ?)",
              "Admin", "hashed_password", "ADMIN", 0, 1);
   }
}
```

### Example test case:
```java
public class PlayerControllerTest {
   @Test
   void createPlayer_ShouldAddPlayerToDatabase() {
      // Arrange
      String newPlayerJson = """
                  {
                      "name": "JohnDoe",
                      "password": "password123",
                      "handicap": 10,
                      "role": "USER"
                  }
              """;

      // Act
      var response = client.post()
              .uri("/players")
              .bodyValue(newPlayerJson)
              .retrieve()
              .toEntity(Player.class)
              .block();

      // Assert
      assertNotNull(response);
      assertEquals(HttpStatus.OK, response.getStatusCode());
      List<Long> ids = getIds();
      assertEquals(2, ids.size()); // Assuming 1 default player already exists
      Player newPlayer = getPlayerById(ids.get(1));
      assertEquals("JohnDoe", newPlayer.getName());
   }
}
```

# Summary
- Use @Transactional for all tests to ensure rollback.
- Vaidate database actions using JDBCTemplate
- Set up default entities in @BeforeEach or @BeforeAll
- Use WebClient for HTTP requests
- Write independent tests to support parallel execution
- Follow consistent naming conventions for clarity

