# ⛳ Golf Club Tracker

A Spring Boot application designed to manage and track golf tournaments, matches, player performance, and team standings across a season.

## 🏌️ Core Features

### 👤 Player & Team Management
- Add and manage players with unique identifiers and handicaps.
- Assign players to teams for each season.

### 🏆 Tournament Tracking
- Record tournament results including:
  - Top 3 players and their Stableford points:
    - 🥇 Winner: 100 points
    - 🥈 Runner-up: 66 points
    - 🥉 Third place: 33 points split among tied players
  - Most birdies: 50 points
  - Nearest to pin: 50 points  
  *(Note: Winner is excluded from birdie and nearest-to-pin awards; next eligible player is selected.)*

- Option to manually enter top performers or auto-generate results from individual player scores.

### 🎯 Match Tracking
- Create matches with unique IDs.
- Record individual player scores per match.

### 📊 Season Summary & Handicap Adjustment
- Aggregate team points across tournaments.
- Identify the top-performing team at season end.

## 🚀 MVP Goals
- Add tournament entries manually or auto-fill from player scores.
- Track individual and team performance throughout the season.

---

📁 For database schema and entity relationships, see [`documentation/database.md`](documentation/database.md)
