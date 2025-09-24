# Project Context: Golf Club Tracker

## Overview
The **Golf Club Tracker** is a Spring Boot application designed to manage and automate key aspects of a golf club's competitive season. It provides features for managing players, teams, tournaments, and season-long analytics.

---

## Key Features
1. **Player and Team Management**:
   - Players have unique IDs, names, passwords (hashed), handicaps, and roles.
   - Teams are collections of players, and players can be assigned to teams for each season.

2. **Tournament Tracking**:
   - Tracks tournament results, awards points for top performers, and supports both manual and automated result entry.

3. **Flight and Score Recording**:
   - Manages flights (groups of players) and records individual scores.

4. **Season Summary**:
   - Aggregates team points, identifies top-performing teams, and adjusts player handicaps based on performance.

5. **User Permissions**:
   - Admins can add/edit data, while players can only log in and view data.

---

## Technologies Used
- **Languages**: Java, Kotlin, SQL
- **Frameworks**: Spring Boot
- **Build Tool**: Gradle
- **Database**: PostgreSQL

---

## Entities (package: `com.pinewoods.score.tracker.entities`)
### Admin entities (package: `com.pinewoods.score.tracker.entities.admin`)
1. **Player**:
   - Attributes: `id`, `name`, `password`, `role`, `handicap`, `team`.
   - Relationships: Many-to-One with `Team`.
   - **Purpose**: Represents a player in the system. Used by `PlayerService` and `PlayerController`.

2. **Team**:
   - Attributes: `id`, `name`.
   - Relationships: One-to-Many with `Player`.
   - **Purpose**: Represents a team in the system. Used by `TeamService` and `TeamController`.

---

## DTOs (package: `com.pinewoods.score.tracker.dtos`)
### Admin DTOs (package: `com.pinewoods.score.tracker.dtos.admin`)
1. **PlayerDTO**:
   - Contains: `name`, `handicap`, `teamName`.
   - **Purpose**: Simplifies data transfer for `Player` entities.

2. **TeamDTO**:
   - Contains: `name`, `players` (list of player names).
   - **Purpose**: Simplifies data transfer for `Team` entities.

---

## Services (package: `com.pinewoods.score.tracker.services`)
### Admin Services (package: `com.pinewoods.score.tracker.services.admin`)
1. **PlayerService**:
   - Handles player creation, retrieval, updates (e.g., password changes), and deletion.
   - Includes team assignment for players.
   - **Security**: Method-level security annotations restrict access to certain operations.

2. **TeamService**:
   - Manages team creation, retrieval, and deletion.
   - **Security**: Admin-only access for certain operations.

---

## Controllers (package: `com.pinewoods.score.tracker.controllers`)
### Admin Controllers (package: `com.pinewoods.score.tracker.controllers.admin`)
1. **PlayerController**:
   - Endpoints:
      - `POST /players`: Create a new player.
      - `GET /players`: Retrieve all players.
      - `GET /players/{name}`: Retrieve a player by name.
      - `PUT /players/{name}/password`: Change a player's password. Use a String body with the new raw password.
      - `PUT /players/{name}/handicap`: Update a player's handicap. Use an Int body with the new handicap
      - `PUT /players/{name}/team`: Assign a player to a team. Use a String body with the team name.
      - `DELETE /players/{name}`: Delete a player.
   - **Purpose**: Manages player-related operations.

2. **TeamController**:
   - Endpoints:
      - `POST /teams`: Create a new team.
      - `GET /teams`: Retrieve all teams.
      - `GET /teams/{name}`: Retrieve a team by name.
      - `DELETE /teams/{name}`: Delete a team.
   - **Purpose**: Manages team-related operations.

---

## Security (package: `com.pinewoods.score.tracker.config`)
- **Spring Security**:
   - Uses method-level security annotations (e.g., `@PreAuthorize`).
   - Passwords are hashed using `BCryptPasswordEncoder`.
- **Roles**:
   - Admins: Full access to manage players and teams.
   - Players: Limited access to view and update their own data.

---
## Exception handling (package: `com.pinewoods.score.tracker.exceptions`)
- Custom exception for entity conflicts
- Custom exception for entity not found
- Global exception handler to manage and format error responses

---

## Database (location: db)
- **Player** table:
   - Stores player details.
   - Relationships: Many-to-One with `Team`.

- **Team** table:
   - Stores team details.
   - Relationships: One-to-Many with `Player`.

---

## How to Use This Context
- **For Prompting**: Use the detailed endpoint descriptions and entity relationships to ask specific questions about functionality.
- **For Assisting**: Refer to the services and DTOs for understanding the flow of data and business logic.