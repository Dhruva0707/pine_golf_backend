package com.pinewoods.score.tracker.dao.season;

import com.pinewoods.score.tracker.entities.season.Season;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SeasonRepository extends JpaRepository<Season, Long> {
    Optional<Season> findByName(String name);
    List<Season> findByNameStartingWith(String datePrefix);
}
