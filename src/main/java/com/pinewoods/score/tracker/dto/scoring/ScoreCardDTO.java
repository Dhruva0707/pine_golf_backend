package com.pinewoods.score.tracker.dto.scoring;

import com.pinewoods.score.tracker.entities.admin.Player;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ScoreCardDTO (
        @NotNull
        Player player,

        @NotNull
        @Size(min = 18, max = 18, message = "Exactly 18 hole scores must be provided.")
        List<Integer> holeScores
) {}
