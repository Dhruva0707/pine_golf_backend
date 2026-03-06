package com.pinewoods.score.tracker.services.scoring;

import com.pinewoods.score.tracker.dto.scoring.ScoreCardDTO;
import com.pinewoods.score.tracker.entities.admin.Player;
import com.pinewoods.score.tracker.entities.flight.Flight;
import com.pinewoods.score.tracker.entities.flight.FlightScore;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.*;

public record StablefordScoringStrategy(List<Integer> pars, List<Integer> indexes,
                                        Map<Integer, Integer> pointsMap) implements IScoringStrategy {
    public StablefordScoringStrategy {
        if (pars.size() != 18 || indexes.size() != 18) {
            throw new IllegalArgumentException("Pars and indexes must be of length 18");
        }

    }

    public StablefordScoringStrategy(Integer par, List<Integer> indexes, Map<Integer, Integer> pointsMap) {
        this(Collections.nCopies(18, par), indexes, pointsMap);
    }

    @Override
    public Flight calculateScores(List<ScoreCardDTO> cards) {
        Flight flight = Flight.builder()
                .date(new Date())
                .flightScores(new ArrayList<>())
                .build();

        for (ScoreCardDTO card : cards) {
            Player player = card.player();

            // Perform the handicap/par/index math we discussed
            int totalPoints = calculateScore(card.holeScores(), player.getHandicap());
            int birdies = countBirdies(card.holeScores());

            FlightScore fs = FlightScore.builder()
                    .player(player)
                    .score(totalPoints)
                    .birdies(birdies)
                    .flight(flight) // Set back-reference
                    .build();

            flight.getFlightScores().add(fs);
        }
        return flight;
    }

    @Override
    public String getName() {
        return "STABLEFORD";
    }

    private int countBirdies(
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

    private int calculateScore(
            @NotNull @Size(min = 18, max = 18, message = "Exactly 18 hole scores must be provided.")
            List<Integer> scores,
            @NotNull double hcp) {
        int totalPoints = 0;
        int minDiff = pointsMap.keySet().stream().min(Integer::compare).orElse(0);
        int maxDiff = pointsMap.keySet().stream().max(Integer::compare).orElse(0);
        for (int i = 0; i < 18; i++) {
            int par = pars.get(i);
            int index = indexes.get(i);
            int gross = scores.get(i);
            int strokesReceived = (int) ((hcp / 18) + (hcp % 18 >= index ? 1 : 0));
            int net = gross - strokesReceived;
            int diff = net - par;
            // Points calculation
            totalPoints += diff < minDiff ? pointsMap.get(minDiff) :
                    diff > maxDiff ? pointsMap.get(maxDiff) :
                            pointsMap.get(diff);
        }

        return totalPoints;
    }
}
