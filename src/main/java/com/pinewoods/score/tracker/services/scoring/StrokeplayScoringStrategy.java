package com.pinewoods.score.tracker.services.scoring;

import com.pinewoods.score.tracker.dao.admin.PlayerRepository;
import com.pinewoods.score.tracker.dto.admin.PlayerDTO;
import com.pinewoods.score.tracker.entities.flight.Flight;
import com.pinewoods.score.tracker.entities.flight.FlightScore;

import java.util.Date;
import java.util.List;

public class StrokeplayScoringStrategy extends BaseScoringStrategy {

    PlayerRepository playerRepo;
    final int totalPar;

    public StrokeplayScoringStrategy(List<Integer> pars, String courseName, PlayerRepository playerRepo,
                                     double handicapMultiplier) {
        super(handicapMultiplier);

        if (pars.size() != 18 || indexes.size() != 18) {
            throw new IllegalArgumentException("Pars and indexes must be of length 18");
        }

        this.pars = pars;
        this.courseName = courseName;
        this.playerRepo = playerRepo;
        totalPar = pars.stream().mapToInt(Integer::intValue).sum();
    }

    @Override
    public Flight calculateScores(Flight originalFlight) {
        Flight calculatedFlight = Flight.builder()
                .date(new Date())
                .build();

        for (FlightScore card : originalFlight.getFlightScores()) {
            PlayerDTO player = card.getPlayer().toDTO();
            int handicap = (int) Math.round(player.handicap() * handicapMultiplier);
            int totalScore = card.getHoleScores().stream().mapToInt(Integer::intValue).sum();
            int birdies = countBirdies(card.getHoleScores());
            int totalPoints = totalPar + handicap - totalScore;

            FlightScore fs = FlightScore.builder()
                    .player(playerRepo.findByName(player.name()).orElseThrow())
                    .score(totalPoints)
                    .birdies(birdies)
                    .flight(calculatedFlight)
                    .build();

            calculatedFlight.getFlightScores().add(fs);
        }

        return calculatedFlight;
    }

    @Override
    public String getName() {
        return "STROKEPLAY";
    }
}
