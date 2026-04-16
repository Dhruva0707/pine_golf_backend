package com.pinewoods.score.tracker.services.scoring;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.List;

public abstract class BaseScoringStrategy implements IScoringStrategy {
    @Getter
    List<Integer> pars;
    List<Integer> indexes;
    String courseName;
    double handicapMultiplier;

    protected BaseScoringStrategy(double handicapMultiplier) {
        this.handicapMultiplier = handicapMultiplier;
    }

    @Override
    public String getCourseName() {
        return courseName;
    }

    @Override
    public int countBirdies(
            @NotNull @Size(min = 18, max = 18, message = "Exactly 18 hole scores must be provided.")
            List<Integer> scores) {
        int birdies = 0;
        for (int i = 0; i < 18; i++) {
            if (scores.get(i) - pars.get(i) < 0) {
                birdies++;
            }
        }
        return birdies;
    }

    @Override
    public double getHandicapMultiplier() {
        return handicapMultiplier > 0 ? handicapMultiplier : 1;
    }
}
