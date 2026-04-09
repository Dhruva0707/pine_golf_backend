package com.pinewoods.score.tracker.services.scoring;

import com.pinewoods.score.tracker.dao.admin.PlayerRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ScoringStrategyFactory {
    public IScoringStrategy getStrategy(String type, List<Integer> pars, List<Integer> indexes,
                                        Map<Integer, Integer> pointsMap, double handicapMultiplier, String courseName,
                                        PlayerRepository playerRepository) {
        return switch (type.trim().toUpperCase()) { // Added .trim()
            case "STABLEFORD" -> new StablefordScoringStrategy(pars, indexes, pointsMap, handicapMultiplier,
                    courseName, playerRepository);
            case "STROKEPLAY" -> new StrokeplayScoringStrategy(pars, courseName, playerRepository, handicapMultiplier);
            default -> throw new IllegalArgumentException("Unsupported scoring strategy: " + type);
        };
    }
}
