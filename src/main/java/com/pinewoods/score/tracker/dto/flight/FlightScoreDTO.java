package com.pinewoods.score.tracker.dto.flight;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public  record FlightScoreDTO (
        @Schema(description = "Player name", example = "Tiger Woods")
        String playerName,
        @Schema(example = "36", minimum = "0", maximum = "100")
        Integer score,
        @Schema(example = "3", minimum = "0", maximum = "18")
        Integer birdies,
        List<Integer> holeScores,
        String courseName){}

