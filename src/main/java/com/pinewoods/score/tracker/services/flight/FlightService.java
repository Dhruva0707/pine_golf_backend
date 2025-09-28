package com.pinewoods.score.tracker.services.flight;

import com.pinewoods.score.tracker.dao.admin.PlayerRepository;
import com.pinewoods.score.tracker.dao.flight.FlightRepository;
import com.pinewoods.score.tracker.dao.flight.FlightScoreRepository;
import com.pinewoods.score.tracker.dto.flight.FlightDTO;
import com.pinewoods.score.tracker.dto.flight.FlightScoreDTO;
import com.pinewoods.score.tracker.entities.admin.Player;
import com.pinewoods.score.tracker.entities.flight.Flight;
import com.pinewoods.score.tracker.entities.flight.FlightScore;
import com.pinewoods.score.tracker.exceptions.ResourceNotFoundException;
import jakarta.transaction.Transactional;
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

    public FlightService(FlightRepository flightRepository, PlayerRepository playerRepository, FlightScoreRepository flightScoreRepository) {
        this.flightRepository = flightRepository;
        this.playerRepository = playerRepository;
        this.flightScoreRepository = flightScoreRepository;
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
                            .score(dto.score())
                            .flight(flight)
                            .birdies(dto.birdies() != null ?  dto.birdies() : 0)
                            .build();
                }).toList();

        flightScoreRepository.saveAll(flightScores);

        flight.setFlightScores(flightScores);

        return flight;
    }

    // ----------- Get Methods --------------
    public Flight getFlight(long id) {
        return flightRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flight not found: " + id));
    }

    public FlightDTO getFlightDTO(long id) {
        Flight flight = getFlight(id);
        return createDTO(flight);
    }

    // ----------- Helper Methods -----------
    public static FlightDTO createDTO(Flight flight) {
        List<FlightScoreDTO> scoreDTOs = flight.getFlightScores().stream()
                .map(fs ->
                        new FlightScoreDTO(fs.getPlayer().getName(), fs.getScore(), fs.getBirdies()))
                .toList();

        return new FlightDTO(flight.getDate(), scoreDTOs);
    }
}
