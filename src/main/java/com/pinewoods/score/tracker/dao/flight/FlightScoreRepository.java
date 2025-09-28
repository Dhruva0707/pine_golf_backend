package com.pinewoods.score.tracker.dao.flight;

import com.pinewoods.score.tracker.entities.admin.Player;
import com.pinewoods.score.tracker.entities.flight.FlightScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FlightScoreRepository extends JpaRepository<FlightScore, Long> {
    List<FlightScore> findByPlayerOrderByFlight_DateDesc(Player player);
}
