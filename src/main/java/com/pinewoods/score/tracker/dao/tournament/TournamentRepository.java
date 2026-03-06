package com.pinewoods.score.tracker.dao.tournament;

import com.pinewoods.score.tracker.entities.tournament.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TournamentRepository extends JpaRepository<Tournament, Long> {
    List<Tournament> findBySeasonId(Long seasonId);
}
