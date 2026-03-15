package com.pinewoods.score.tracker.dto.season;

import com.pinewoods.score.tracker.dto.tournament.TournamentDTO;

import java.util.List;

public record SeasonDTO(String name, List<TeamStandingDTO> standings, List<TournamentDTO> tournaments) {
}
