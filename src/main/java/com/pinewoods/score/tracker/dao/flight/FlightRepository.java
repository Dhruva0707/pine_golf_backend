package com.pinewoods.score.tracker.dao.flight;

import com.pinewoods.score.tracker.entities.flight.Flight;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlightRepository extends JpaRepository<Flight, Long> {
}
