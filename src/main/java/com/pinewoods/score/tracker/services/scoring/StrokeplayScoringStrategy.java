package com.pinewoods.score.tracker.services.scoring;

import com.pinewoods.score.tracker.dao.admin.PlayerRepository;
import com.pinewoods.score.tracker.dto.admin.PlayerDTO;
import com.pinewoods.score.tracker.dto.scoring.ScoreCardDTO;
import com.pinewoods.score.tracker.entities.flight.Flight;
import com.pinewoods.score.tracker.entities.flight.FlightScore;

import java.util.Date;
import java.util.List;

public class StrokeplayScoringStrategy extends BaseScoringStrategy {

    PlayerRepository playerRepo;
    final double handicapMultiplier;
    final int totalPar;

    public StrokeplayScoringStrategy(List<Integer> pars, String courseName, PlayerRepository playerRepo,
                                     double handicapMultiplier) {
        this.pars = pars;
        this.courseName = courseName;
        this.playerRepo = playerRepo;
        this.handicapMultiplier = handicapMultiplier;
        totalPar = pars.stream().mapToInt(Integer::intValue).sum();
    }

    @Override
    public Flight calculateScores(List<ScoreCardDTO> cards) {
        Flight flight = Flight.builder()
                .date(new Date())
                .build();

        for (ScoreCardDTO card : cards) {
            PlayerDTO player = card.player();
            int handicap = (int) Math.round(player.handicap() * handicapMultiplier);
            int totalScore = card.holeScores().stream().mapToInt(Integer::intValue).sum();
            int birdies = countBirdies(card.holeScores());
            int totalPoints = totalPar + handicap - totalScore;

            FlightScore fs = FlightScore.builder()
                    .player(playerRepo.findByName(player.name()).orElseThrow())
                    .score(totalPoints)
                    .birdies(birdies)
                    .flight(flight)
                    .build();

            flight.getFlightScores().add(fs);
        }

        return flight;
    }

    @Override
    public String getName() {
        return "STROKEPLAY";
    }
}
