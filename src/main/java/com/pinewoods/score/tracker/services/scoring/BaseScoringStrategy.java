package com.pinewoods.score.tracker.services.scoring;

import com.pinewoods.score.tracker.entities.course.Course;
import com.pinewoods.score.tracker.services.course.CourseService;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.List;

public abstract class BaseScoringStrategy implements IScoringStrategy {
    @Getter
    Course course;
    double handicapMultiplier;
    CourseService courseService;

    protected BaseScoringStrategy(double handicapMultiplier) {
        this.handicapMultiplier = handicapMultiplier;
    }

    @Override
    public String getCourseName() {
        return course.getName();
    }

    @Override
    public int countBirdies(
            @NotNull @Size(min = 18, max = 18, message = "Exactly 18 hole scores must be provided.")
            List<Integer> scores) {
        int birdies = 0;
        for (int i = 0; i < 18; i++) {
            if (scores.get(i) - course.getPars().get(i) < 0) {
                birdies++;
            }
        }
        return birdies;
    }

    @Override
    public double getHandicapMultiplier() {
        return handicapMultiplier > 0 ? handicapMultiplier : 1;
    }

    protected double getCourseHandicap(Long playerId, Long courseId) {
        return courseService.getCourseHandicap(playerId, courseId).getHandicap();
    }
}
