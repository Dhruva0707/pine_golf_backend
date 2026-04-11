package com.pinewoods.score.tracker.services.season;

import com.pinewoods.score.tracker.dao.season.SeasonRepository;
import com.pinewoods.score.tracker.dto.season.SeasonDTO;
import com.pinewoods.score.tracker.dto.season.TeamStandingDTO;
import com.pinewoods.score.tracker.dto.tournament.TournamentDTO;
import com.pinewoods.score.tracker.entities.season.Season;
import com.pinewoods.score.tracker.entities.season.TeamStanding;
import com.pinewoods.score.tracker.entities.tournament.Tournament;
import com.pinewoods.score.tracker.exceptions.ResourceConflictException;
import com.pinewoods.score.tracker.exceptions.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class SeasonService {
    private final SeasonRepository seasonRepo;

    /**
     * Creates a season with the given name.
     * @param name name of the season
     * @return season dto containing all the information
     */
    @PreAuthorize("hasRole('ADMIN')")
    public Season createSeason(String name) {
        String seasonName = new Date().toInstant()
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "_" + name;
        Season season = Season.builder()
                .name(seasonName)
                .build();

        if (seasonRepo.findByName(seasonName).isPresent()) {
            throw new ResourceConflictException("Season already exists");
        }

        seasonRepo.save(season);

        return season;
    }

    /**
     * get the season and its entiter dto
     * @param name name of the season
     * @return season dto containing all the information
     */
    public Season getSeasonByName(String name) {
        return seasonRepo.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Season not found"));
    }

    public List<Season> getAllSeasons() {
        return seasonRepo.findAll();
    }

    /************************** drill downs ****************************
     * Drill-down: get tournaments for a season.
     */
    public List<Tournament> getTournaments(String seasonName) {
        Season season = seasonRepo.findByName(seasonName)
                .orElseThrow(() -> new ResourceNotFoundException("Season not found"));

        return season.getTournaments();
    }

    /**
     * Drill-down: get standings for a season.
     */
    public List<TeamStanding> getStandings(String seasonName) {
        Season season = seasonRepo.findByName(seasonName)
                .orElseThrow(() -> new ResourceNotFoundException("Season not found"));

        return season.getStandings();
    }

    // ================== Finish Season ======================
    public Season finishSeason(String seasonName) {
        Season season = seasonRepo.findByName(seasonName)
                .orElseThrow(() -> new ResourceNotFoundException("Season not found"));
        season.setFinished(true);
        seasonRepo.save(season);

        return season;
    }

    //=================== Delete Season ======================
    public void deleteSeason(String seasonName) {
        Season season = seasonRepo.findByName(seasonName)
                .orElseThrow(() -> new ResourceNotFoundException("Season not found"));
        seasonRepo.delete(season);
    }
}
