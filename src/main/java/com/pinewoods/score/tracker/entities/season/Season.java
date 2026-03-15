package com.pinewoods.score.tracker.entities.season;

import com.pinewoods.score.tracker.dto.season.SeasonDTO;
import com.pinewoods.score.tracker.dto.season.TeamStandingDTO;
import com.pinewoods.score.tracker.dto.tournament.TournamentDTO;
import com.pinewoods.score.tracker.entities.tournament.Tournament;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.*;

@Entity
@Table(name = "seasons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Season {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private boolean isFinished;

    @NotNull
    @Column(name = "season_name", unique = true)
    private String name;

    @OneToMany(mappedBy = "season", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<Tournament> tournaments = new ArrayList<>();

    @OneToMany(mappedBy = "season", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<TeamStanding> standings = new ArrayList<>();

    public SeasonDTO toDto() {
        List<TournamentDTO> tournaments = new ArrayList<>();
        if (!this.tournaments.isEmpty()) {
            tournaments = this.tournaments.stream().map(Tournament::toDTO).toList();
        }
        List<TeamStandingDTO> standings = new ArrayList<>();
        if (!this.standings.isEmpty()) {
            standings = this.standings.stream().map(TeamStanding::toDTO).toList();
        }
        return new SeasonDTO(name,
                standings,
                tournaments);
    }
}
