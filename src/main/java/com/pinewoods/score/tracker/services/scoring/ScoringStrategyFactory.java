package com.pinewoods.score.tracker.services.scoring;

import com.pinewoods.score.tracker.dao.admin.PlayerRepository;
import com.pinewoods.score.tracker.entities.course.Course;
import com.pinewoods.score.tracker.services.course.CourseService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ScoringStrategyFactory {
    public IScoringStrategy getStrategy(String type, Course course,
                                        Map<Integer, Integer> pointsMap, double handicapMultiplier,
                                        PlayerRepository playerRepository, CourseService courseService) {
        return switch (type.trim().toUpperCase()) { // Added .trim()
            case "STABLEFORD" -> new StablefordScoringStrategy(course, pointsMap, handicapMultiplier,
                    playerRepository, courseService);
            case "STROKEPLAY" -> new StrokeplayScoringStrategy(course, playerRepository, handicapMultiplier, courseService);
            default -> throw new IllegalArgumentException("Unsupported scoring strategy: " + type);
        };
    }
}
