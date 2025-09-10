# 🏌️ Golf Club Management System

## 🧱 Database Architecture

### 🧍 Player & Team Management

- `Player`: Stores player info like name, ID, and handicap

- `Team`: Represents teams per season

- `PlayerTeamAssignment`: Links players to teams per season

### 🏆 Tournament & Match Tracking

- `Tournament`: Represents a tournament event

- `Match`: Optional, represents individual matches within a tournament

- `PlayerMatchScore`: Player’s performance in a match

- `TournamentResult`: Final rankings and points

- `TeamScore`: Points earned by a team

### 📊 Season Summary

- `SeasonResult`: Final team standings

- `HandicapAdjustment`: Tracks handicap changes