-- Create a default team
INSERT INTO teams (name)
VALUES ('UNASSIGNED');

-- Insert the admin entry
INSERT INTO players (name, password, role, handicap, team_id)
VALUES ('pinewoods_admin', '$2a$12$aRL6oTTPRkcc1WHv0ZjdB.4kht7C5CX4/JV6XbIrFfK1gwzvt0tqO', 'ADMIN', 0.0,
    (SELECT id FROM teams WHERE name = 'UNASSIGNED'));
-- default password is 'P!neWo0d$'