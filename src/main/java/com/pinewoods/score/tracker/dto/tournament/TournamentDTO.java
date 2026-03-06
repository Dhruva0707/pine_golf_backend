package com.pinewoods.score.tracker.dto.tournament;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

public record TournamentDTO(
        @Schema(description = "Tournament name", example = "02_02_2026_PineWoodsMMR")
        String name,
        Map<Long, Integer> awards){}

