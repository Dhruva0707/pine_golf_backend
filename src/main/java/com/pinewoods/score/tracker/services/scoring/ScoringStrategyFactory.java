package com.pinewoods.score.tracker.services.scoring;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ScoringStrategyFactory {
    public IScoringStrategy getStrategy(String type, List<Integer> pars, List<Integer> indexes,
                                        Map<Integer, Integer> pointsMap, double handicapMultiplier) {
        if ("STABLEFORD".equalsIgnoreCase(type)) {
            return new StablefordScoringStrategy(pars, indexes, pointsMap, handicapMultiplier);
        }

        // You can expand this with more strategies as they are built
        throw new IllegalArgumentException("Unsupported scoring strategy: " + type);
    }
}
