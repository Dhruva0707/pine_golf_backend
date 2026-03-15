package com.pinewoods.score.tracker.entities.season;

import com.pinewoods.score.tracker.dto.season.TeamStandingDTO;
import com.pinewoods.score.tracker.entities.admin.Team;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TeamStanding {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Team team;

    @ManyToOne(optional = false)
    private Season season;

    private int points = 0;
    private int wins = 0;
    private int losses = 0;
    private int draws = 0;
    private int birdies = 0;

    public TeamStandingDTO toDTO() {
        return new TeamStandingDTO(
                team.getName(),
                points,
                wins,
                losses,
                draws,
                birdies
        );
    }
}
