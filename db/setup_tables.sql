-- TEAM TABLE
CREATE TABLE teams (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

-- PLAYER TABLE
CREATE TABLE players (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    password VARCHAR(100) NOT NULL,
    handicap INT NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'PLAYER',
    team_id INT REFERENCES teams(id)
);

-- TOURNAMENT TABLE
CREATE TABLE tournaments (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    date DATE NOT NULL
);

-- MATCH TABLE
CREATE TABLE matches (
    id SERIAL PRIMARY KEY,
    date DATE NOT NULL
);

-- PLAYER MATCH SCORES
CREATE TABLE match_scores (
    id SERIAL PRIMARY KEY,
    match_id INT REFERENCES matches(id),
    player_id INT REFERENCES players(id),
    score INT NOT NULL
);

-- TOURNAMENT RESULTS
CREATE TABLE tournament_results (
    id SERIAL PRIMARY KEY,
    tournament_id INT REFERENCES tournaments(id),
    player_id INT REFERENCES players(id),
    position INT, -- 1 = Winner, 2 = Runner-up, 3 = Third
    stableford_points INT NOT NULL,
    birdies INT DEFAULT 0,
    nearest_to_pin BOOLEAN DEFAULT FALSE
);

-- TEAM PERFORMANCE TRACKING
CREATE TABLE team_points (
    id SERIAL PRIMARY KEY,
    team_id INT REFERENCES teams(id),
    tournament_id INT REFERENCES tournaments(id),
    total_points INT NOT NULL
);

-- HANDICAP HISTORY (OPTIONAL)
CREATE TABLE handicap_history (
    id SERIAL PRIMARY KEY,
    player_id INT REFERENCES players(id),
    season_year INT NOT NULL,
    old_handicap INT,
    new_handicap INT
);

-- Insert the admin entry
INSERT INTO players (name, password, role, handicap)
VALUES ('pinewoods_admin', '$2a$10$jivffkWyc/IG7APi2HdPgeW3lsYEJT7n9ydQO2I6g3YIvu44OZ5z.', 'ADMIN', 0);

-- Insert the default team
INSERT INTO teams (name)
VALUES ('UNASSIGNED')