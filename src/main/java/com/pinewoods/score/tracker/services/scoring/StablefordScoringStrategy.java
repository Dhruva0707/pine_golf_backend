package com.pinewoods.score.tracker.services.scoring;

import com.pinewoods.score.tracker.dao.admin.PlayerRepository;
import com.pinewoods.score.tracker.dto.admin.PlayerDTO;
import com.pinewoods.score.tracker.dto.scoring.ScoreCardDTO;
import com.pinewoods.score.tracker.entities.flight.Flight;
import com.pinewoods.score.tracker.entities.flight.FlightScore;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.*;

public class StablefordScoringStrategy extends BaseScoringStrategy {

    final Map<Integer, Integer> pointsMap;
    final double handicapMultiplier;
    PlayerRepository playerRepo;

    public StablefordScoringStrategy(List<Integer> pars, List<Integer> indexes,
                                     Map<Integer, Integer> pointsMap, double handicapMultiplier, String courseName,
                                     PlayerRepository playerRepo) {
        if (pars.size() != 18 || indexes.size() != 18) {
            throw new IllegalArgumentException("Pars and indexes must be of length 18");
        }

        this.pars = pars;
        this.indexes = indexes;
        this.pointsMap = pointsMap;
        this.handicapMultiplier = handicapMultiplier;
        this.playerRepo = playerRepo;
        this.courseName = courseName;
    }

    @Override
    public Flight calculateScores(List<ScoreCardDTO> cards) {
        Flight flight = Flight.builder()
                .date(new Date())
                .build();

        for (ScoreCardDTO card : cards) {
            PlayerDTO player = card.player();
            int handicap = (int) Math.round(player.handicap() * handicapMultiplier);

            // Perform the handicap/par/index math we discussed
            int totalPoints = calculateScore(card.holeScores(), handicap);
            int birdies = countBirdies(card.holeScores());

            FlightScore fs = FlightScore.builder()
                    .player(playerRepo.findByName(player.name()).orElseThrow())
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
