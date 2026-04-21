package com.pinewoods.score.tracker.services.scoring;

import com.pinewoods.score.tracker.dao.admin.PlayerRepository;
import com.pinewoods.score.tracker.dao.course.CourseHandicapRepository;
import com.pinewoods.score.tracker.dto.admin.PlayerDTO;
import com.pinewoods.score.tracker.entities.admin.Player;
import com.pinewoods.score.tracker.entities.course.Course;
import com.pinewoods.score.tracker.entities.flight.Flight;
import com.pinewoods.score.tracker.entities.flight.FlightScore;
import com.pinewoods.score.tracker.services.course.CourseService;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.*;

public class StablefordScoringStrategy extends BaseScoringStrategy {

    final Map<Integer, Integer> pointsMap;
    PlayerRepository playerRepo;
    final List<Integer> pars;
    final List<Integer> indexes;

    public StablefordScoringStrategy(Course course,
                                     Map<Integer, Integer> pointsMap, double handicapMultiplier,
                                     PlayerRepository playerRepo, CourseService courseService) {

        super(handicapMultiplier);

        this.course = course;
        this.pointsMap = pointsMap;
        this.playerRepo = playerRepo;
        this.courseService = courseService;
        this.pars = course.getPars();
        this.indexes = course.getIndexes();
    }

    @Override
    public Flight calculateScores(Flight flight) {
        Flight calculatedFlight = Flight.builder()
                .date(new Date())
                .build();

        for (FlightScore card : flight.getFlightScores()) {
            Player player = card.getPlayer();
            PlayerDTO playerDTO = player.toDTO();
            int handicap = (int) Math.round(getCourseHandicap(player.getId(), course.getId()) * handicapMultiplier);

            // Perform the handicap/par/index math we discussed
            int totalPoints = calculateScore(card.getHoleScores(), handicap);
            int birdies = countBirdies(card.getHoleScores());

            FlightScore fs = FlightScore.builder()
                    .player(playerRepo.findByName(playerDTO.name()).orElseThrow())
                    .score(totalPoints)
                    .birdies(birdies)
                    .flight(calculatedFlight) // Set back-reference
                    .build();

            calculatedFlight.getFlightScores().add(fs);
        }
        return calculatedFlight;
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
        int minDiff = pointsMap.keySet().stream().min(Integer::compare).orElse(-2);
        int maxDiff = pointsMap.keySet().stream().max(Integer::compare).orElse(2);
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
