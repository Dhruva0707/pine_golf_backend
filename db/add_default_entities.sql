-- Insert the admin entry
INSERT INTO players (name, password, role, handicap)
VALUES ('pinewoods_admin', '$2a$12$c1mPnwk0n5mgybg8keKPKegjUcM9VfeJ.5/RvOTdO1zdg7Zj2eTIW', 'ADMIN', 0);

-- Insert the default team
INSERT INTO teams (name)
VALUES ('UNASSIGNED')