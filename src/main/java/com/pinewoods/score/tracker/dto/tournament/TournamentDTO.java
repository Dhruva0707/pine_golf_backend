package com.pinewoods.score.tracker.dto.tournament;

import com.pinewoods.score.tracker.dto.flight.FlightDTO;
import com.pinewoods.score.tracker.dto.flight.FlightScoreDTO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

public record TournamentDTO(
        @Schema(description = "Tournament name", example = "02_02_2026_PineWoodsMMR")
        String name,
        @Schema(description = "Awards for each player")
        Map<Long, Integer> awards,
        Long id,
        Long seasonId,
        String strategyName,
        List<FlightDTO> flights){}

