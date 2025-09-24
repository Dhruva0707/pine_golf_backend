-- TEAM TABLE
CREATE TABLE teams (
   id BIGSERIAL PRIMARY KEY,
   name VARCHAR(100) NOT NULL UNIQUE
);

-- PLAYER TABLE
CREATE TABLE players (
   id BIGSERIAL PRIMARY KEY,
   name VARCHAR(100) NOT NULL UNIQUE,
   password VARCHAR(255) NOT NULL,
   handicap DOUBLE PRECISION,
   role VARCHAR(50) NOT NULL,
   team_id BIGINT REFERENCES teams(id)
);

-- FLIGHT TABLE
CREATE TABLE flights (
   id BIGSERIAL PRIMARY KEY,
   date DATE NOT NULL
);

-- TOURNAMENT TABLE
CREATE TABLE tournaments (
   id BIGSERIAL PRIMARY KEY,
   name VARCHAR(100) NOT NULL,
   flight_id BIGINT REFERENCES flights(id),
   winner_player_id BIGINT REFERENCES players(id),
   runner_player_id BIGINT REFERENCES players(id)
);

-- FLIGHT SCORES TABLE
CREATE TABLE flight_scores (
   id BIGSERIAL PRIMARY KEY,
   player_id BIGINT REFERENCES players(id),
   flight_id BIGINT REFERENCES flights(id),
   score INT
);

-- SEASON TABLE
CREATE TABLE seasons (
   id BIGSERIAL PRIMARY KEY,
   start_date DATE NOT NULL,
   end_date DATE
);

-- SEASON SCORES TABLE
CREATE TABLE season_scores (
   id BIGSERIAL PRIMARY KEY,
   season_id BIGINT REFERENCES seasons(id),
   team_id BIGINT REFERENCES teams(id),
   score INT
);

-- WINNERS TABLE
CREATE TABLE winners (
   id BIGSERIAL PRIMARY KEY,
   player_id BIGINT REFERENCES players(id)
);
