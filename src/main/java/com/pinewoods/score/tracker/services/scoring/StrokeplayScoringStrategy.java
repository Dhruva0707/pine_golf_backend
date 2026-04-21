package com.pinewoods.score.tracker.services.scoring;

import com.pinewoods.score.tracker.dao.admin.PlayerRepository;
import com.pinewoods.score.tracker.dto.admin.PlayerDTO;
import com.pinewoods.score.tracker.entities.admin.Player;
import com.pinewoods.score.tracker.entities.course.Course;
import com.pinewoods.score.tracker.entities.flight.Flight;
import com.pinewoods.score.tracker.entities.flight.FlightScore;

import com.pinewoods.score.tracker.services.course.CourseService;
import java.util.Date;
import java.util.List;

public class StrokeplayScoringStrategy extends BaseScoringStrategy {

    PlayerRepository playerRepo;
    final int totalPar;

    public StrokeplayScoringStrategy(
        Course course, PlayerRepository playerRepo,
                                     double handicapMultiplier, CourseService courseService) {
        super(handicapMultiplier);

        this.course = course;
        this.playerRepo = playerRepo;
        totalPar = course.getPars().stream().mapToInt(Integer::intValue).sum();
        this.courseService = courseService;
    }

    @Override
    public Flight calculateScores(Flight originalFlight) {
        Flight calculatedFlight = Flight.builder()
                .date(new Date())
                .build();

        for (FlightScore card : originalFlight.getFlightScores()) {
            Player player = card.getPlayer();
            PlayerDTO playerDto = player.toDTO();
            int handicap = (int) Math.round(getCourseHandicap(player.getId(), course.getId()) * handicapMultiplier);
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
