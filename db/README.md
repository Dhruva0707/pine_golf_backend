# 🏌️ Golf Club Management System

## 🧱 Database Architecture

### 🧍 Player & Team Management

- `Player`: Stores player info like name, ID, password (hashed), and handicap
- `Team`: Represents teams per season
- `PlayerTeamAssignment`: Links players to teams per season *(can be derived from `team_id` in `players` if 1:1)*

### 🏆 Tournament & Flight Tracking

- `Tournament`: Represents a tournament event
- `Flight`: Represents a group of players competing on a specific date
- `FlightPlayer`: Join table linking players to flights
- `FlightScore`: Player’s performance in a flight
- `TournamentResult`: Final rankings and points
- `TeamScore`: Points earned by a team

### 📊 Season Summary

- `HandicapHistory`: Tracks handicap changes across seasons
- `TeamPoints`: Aggregates team performance per tournament
## Setup Database
- Install postgres: https://www.postgresql.org/download/ This will also give you PGAdmin with which you can:
  - add/create database users
  - add/create/drop database
- Create the admin user (super User) <golfAdmin>
- Create the database and make the golfAdmin user its admin <golf_database>
- Run the setup_tables.sql script: </br>
  `psql -U <golfAdmin> -d <golf_database> -f db/setup_tables.sql`