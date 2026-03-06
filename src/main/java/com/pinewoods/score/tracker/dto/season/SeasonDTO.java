package com.pinewoods.score.tracker.dto.season;

import com.pinewoods.score.tracker.dto.tournament.TournamentDTO;

public record SeasonDTO(String name, TeamStandingDTO[] standings, TournamentDTO[] tournaments) {
}
