package com.pinewoods.score.tracker.services.scoring;

import com.pinewoods.score.tracker.dao.admin.PlayerRepository;
import com.pinewoods.score.tracker.dto.admin.PlayerDTO;
import com.pinewoods.score.tracker.entities.admin.Player;
import com.pinewoods.score.tracker.entities.flight.Flight;
import com.pinewoods.score.tracker.entities.flight.FlightScore;

import com.pinewoods.score.tracker.services.course.CourseService;
import java.util.Date;

public class StrokeplayScoringStrategy extends BaseScoringStrategy {

    PlayerRepository playerRepo;

    public StrokeplayScoringStrategy(
        Long courseId, PlayerRepository playerRepo,
                                     double handicapMultiplier, CourseService courseService) {
        super(handicapMultiplier);
        this.playerRepo = playerRepo;
        this.courseService = courseService;
        this.courseId = courseId;
    }

    @Override
    public Flight calculateScores(Flight originalFlight) {
        Flight calculatedFlight = Flight.builder()
                .date(new Date())
                .build();

        int totalPar = getCourse().getPars().stream().mapToInt(Integer::intValue).sum();

        for (FlightScore card : originalFlight.getFlightScores()) {
            Player player = card.getPlayer();
            PlayerDTO playerDto = player.toDTO();
            int handicap = (int) Math.round(getCourseHandicap(player.getId(), getCourse().getId()) * handicapMultiplier);
            int totalScore = card.getHoleScores().stream().mapToInt(Integer::intValue).sum();
            int birdies = countBirdies(card.getHoleScores());
            int totalPoints = totalPar + handicap - totalScore;

            FlightScore fs = FlightScore.builder()
                    .player(playerRepo.findByName(playerDto.name()).orElseThrow())
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
