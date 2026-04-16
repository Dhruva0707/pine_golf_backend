package com.pinewoods.score.tracker.services.scoring;

import com.pinewoods.score.tracker.dto.scoring.ScoreCardDTO;
import com.pinewoods.score.tracker.entities.flight.Flight;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public interface IScoringStrategy {
    Flight calculateScores(List<ScoreCardDTO> scoreCards);

    String getName();

    int countBirdies(
            @NotNull @Size(min = 18, max = 18, message = "Exactly 18 hole scores must be provided.")
            List<Integer> scores);

    String getCourseName();

    double getHandicapMultiplier();
}
