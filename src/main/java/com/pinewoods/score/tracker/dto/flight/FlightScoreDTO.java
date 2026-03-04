package com.pinewoods.score.tracker.dto.flight;

import io.swagger.v3.oas.annotations.media.Schema;

public  record FlightScoreDTO (
        @Schema(description = "Player name", example = "Tiger Woods")
        String playerName,
        @Schema(description = "Total score (max 36)", example = "36", minimum = "0", maximum = "100")
        Integer score,
        @Schema(description = "Total score (max 36)", example = "3", minimum = "0", maximum = "18")
        Integer birdies){}

