# 🏌️ Golf Club Management System

## 🧱 Database Architecture

### 🧍 Player & Team Management

- `Player`: id, name (unique), password, handicap, role, team_id (FK to Team)
- `Team`: id, name (unique)

### 🏆 Tournament & Flight Tracking

- `Tournament`: id, name, flight_id (FK to Flight), winner_player_id (FK to Player), runner_player_id (FK to Player)
- `Flight`: id, date (not null)
- `FlightScore`: id, player_id (FK to Player), flight_id (FK to Flight)

### 📊 Season Summary

- `Season`: id, start_date (not null), end_date
- `SeasonScore`: id, season_id (FK to Season), team_id (FK to Team)
- `Winner`: id, player_id (FK to Player)

## Setup Database
- Install postgres: https://www.postgresql.org/download/
- Create the admin user (super User) `<golfAdmin>`
- Create the database and make the golfAdmin user its admin `<golf_database>`
- Run the setup_tables.sql script:  
  `psql -U <golfAdmin> -d <golf_database> -f db/setup_tables.sql`
- Run the add_initial_data.sql script:  
  `psql -U <golfAdmin> -d <golf_database> -f db/add_initial_data.sql`