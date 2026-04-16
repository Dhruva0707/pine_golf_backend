package com.pinewoods.score.tracker.services.flight;

import com.pinewoods.score.tracker.dao.admin.PlayerRepository;
import com.pinewoods.score.tracker.dao.course.CourseRepository;
import com.pinewoods.score.tracker.dao.flight.FlightRepository;
import com.pinewoods.score.tracker.dao.flight.FlightScoreRepository;
import com.pinewoods.score.tracker.dto.flight.FlightDTO;
import com.pinewoods.score.tracker.dto.flight.FlightScoreDTO;
import com.pinewoods.score.tracker.entities.admin.Player;
import com.pinewoods.score.tracker.entities.course.Course;
import com.pinewoods.score.tracker.entities.flight.Flight;
import com.pinewoods.score.tracker.entities.flight.FlightScore;
import com.pinewoods.score.tracker.exceptions.ResourceNotFoundException;
import com.pinewoods.score.tracker.services.course.CourseService;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * Service for flight management
 */
@Service
@Transactional
public class FlightService {

    private final FlightRepository flightRepository;
    private final PlayerRepository playerRepository;
    private final FlightScoreRepository flightScoreRepository;
    private final CourseRepository courseRepository;

    public FlightService(FlightRepository flightRepository, PlayerRepository playerRepository, FlightScoreRepository flightScoreRepository,
        CourseRepository courseRepository) {
        this.flightRepository = flightRepository;
        this.playerRepository = playerRepository;
        this.flightScoreRepository = flightScoreRepository;
        this.courseRepository = courseRepository;
    }

    // ----------- Create Flight -----------

    @PreAuthorize("isAuthenticated()")
    public Flight createFlight(List<FlightScoreDTO> scores) {
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();

            boolean containsCurrentUser = scores.stream()
                    .anyMatch(dto -> dto.playerName().equals(currentUser));

            if (!containsCurrentUser) {
                throw new AccessDeniedException("Reporting Player must be part of flight");
            }
        }

        Flight flight = Flight.builder()
                .date(new Date())
                .build();
        flightRepository.save(flight);

        List<FlightScore> flightScores = scores.stream()
                .map(dto -> {
                    Player player = playerRepository.findByName(dto.playerName())
                            .orElseThrow(() -> new ResourceNotFoundException("Player not found: " + dto.playerName()));

                    return FlightScore.builder()
                            .player(player)
                            .holeScores(dto.holeScores())
                            .courseName(dto.courseName())
                            .score(dto.holeScores().stream().mapToInt(Integer::intValue).sum())
                            .flight(flight)
                            .birdies(dto.birdies() != null ?  dto.birdies() : 0)
                            .build();
                }).toList();

        flightScoreRepository.saveAll(flightScores);

        flight.getFlightScores().addAll(flightScores);

        return flight;
    }

    // ----------- Get Methods --------------
    public Flight getFlight(long id) {
        return flightRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flight not found: " + id));
    }

    public List<Flight> getAllFlights() {
        return flightRepository.findAll();
    }

    public List<Integer> getDefaultScores(Long courseId, double effectiveHandicap) {
        // fetch the pars for the course
        Course course = courseRepository.findById(courseId).orElseThrow();
        List<Integer> expectedPars = new ArrayList<>();
        for (int i = 0; i < 18; i++) {
            int strokesReceived = (int) ((effectiveHandicap / 18) + (effectiveHandicap % 18 >= i ? 1 : 0));
            expectedPars.add(course.getPars().get(i) + strokesReceived);
        }

        return expectedPars;
    }
}
