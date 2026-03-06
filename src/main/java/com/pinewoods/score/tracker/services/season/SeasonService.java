package com.pinewoods.score.tracker.services.season;

import com.pinewoods.score.tracker.dao.season.SeasonRepository;
import com.pinewoods.score.tracker.dto.season.SeasonDTO;
import com.pinewoods.score.tracker.dto.season.TeamStandingDTO;
import com.pinewoods.score.tracker.dto.tournament.TournamentDTO;
import com.pinewoods.score.tracker.entities.season.Season;
import com.pinewoods.score.tracker.entities.season.TeamStanding;
import com.pinewoods.score.tracker.entities.tournament.Tournament;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
    public SeasonDTO createSeason(String name) {
        String seasonName = new Date().toInstant()
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + "_" + name;
        Season season = Season.builder()
                .name(seasonName)
                .build();
        seasonRepo.save(season);

        return new SeasonDTO(
                season.getName(),
                new TeamStandingDTO[]{},
                new TournamentDTO[]{}
        );
    }

    /**
     * get the season and its entiter dto
     * @param name name of the season
     * @return season dto containing all the information
     */
    public SeasonDTO getSeasonByName(String name) {
        Season season = seasonRepo.findByName(name)
                .orElseThrow(() -> new RuntimeException("Season not found"));
        return toDTO(season);
    }

    /************************** drill downs ****************************
     * Drill-down: get tournaments for a season.
     */
    public List<TournamentDTO> getTournaments(String seasonName) {
        Season season = seasonRepo.findByName(seasonName)
                .orElseThrow(() -> new RuntimeException("Season not found"));

        return season.getTournaments().stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Drill-down: get standings for a season.
     */
    public List<TeamStandingDTO> getStandings(String seasonName) {
        Season season = seasonRepo.findByName(seasonName)
                .orElseThrow(() -> new RuntimeException("Season not found"));

        return season.getStandings().stream()
                .map(this::toDTO)
                .toList();
    }

    //=================== Delete Season ======================
    public void deleteSeason(String seasonName) {
        Season season = seasonRepo.findByName(seasonName)
                .orElseThrow(() -> new RuntimeException("Season not found"));
        seasonRepo.delete(season);
    }


    /**************** Converters  ****************/
    private SeasonDTO toDTO(Season season) {
        return new SeasonDTO(
                season.getName(),
                season.getStandings().stream()
                        .map(this::toDTO)
                        .toArray(TeamStandingDTO[]::new),
                season.getTournaments().stream()
                        .map(this::toDTO)
                        .toArray(TournamentDTO[]::new)
        );
    }

    private TeamStandingDTO toDTO(TeamStanding ts) {
        return new TeamStandingDTO(
                ts.getTeam().getName(),
                ts.getPoints(),
                ts.getWins(),
                ts.getLosses(),
                ts.getDraws(),
                ts.getBirdies(),
                ts.getEagles()
        );
    }

    private TournamentDTO toDTO(Tournament t) {
        return new TournamentDTO(
                t.getName(),
                t.getAwards()
        );
    }
}
