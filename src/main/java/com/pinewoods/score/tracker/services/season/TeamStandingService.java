package com.pinewoods.score.tracker.services.season;

import com.pinewoods.score.tracker.dao.season.SeasonRepository;
import com.pinewoods.score.tracker.dao.season.TeamStandingRepository;
import com.pinewoods.score.tracker.dto.season.TeamStandingDTO;
import com.pinewoods.score.tracker.entities.season.TeamStanding;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class TeamStandingService {
    private final TeamStandingRepository standingRepo;
    private final SeasonRepository seasonRepo;

    /**
     * Get the team standing for a specific season and team.
     * @param seasonName name of the season
     * @param teamName name of the team
     * @return TeamStandingDTO representing the team standing
     */
    public TeamStandingDTO getTeamStanding(String seasonName, String teamName) {
        TeamStanding standing = standingRepo.findBySeasonNameAndTeamName(seasonName, teamName)
                .orElseThrow(() -> new RuntimeException("Standing not found"));

        return new TeamStandingDTO(
                standing.getTeam().getName(),
                standing.getPoints(),
                standing.getWins(),
                standing.getLosses(),
                standing.getDraws(),
                standing.getBirdies(),
                standing.getEagles()
        );
    }

    /**
     * Fetches the team standings for all the seasons of a team
     * @param teamName name of the team
     * @return List of TeamStandingDTO representing the team standings for all seasons
     */
    public List<TeamStandingDTO> getAllStandingsByTeam(String teamName) {
        return standingRepo.findAllByTeamName(teamName)
                .stream()
                .map(ts -> new TeamStandingDTO(
                        ts.getTeam().getName(),
                        ts.getPoints(),
                        ts.getWins(),
                        ts.getLosses(),
                        ts.getDraws(),
                        ts.getBirdies(),
                        ts.getEagles()
                ))
                .toList();
    }
}
