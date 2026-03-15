package com.pinewoods.score.tracker.entities.tournament;

import com.pinewoods.score.tracker.dto.tournament.TournamentDTO;
import com.pinewoods.score.tracker.entities.season.Season;
import com.pinewoods.score.tracker.entities.flight.Flight;
import com.pinewoods.score.tracker.services.scoring.IScoringStrategy;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.*;

@Entity
@Table(name = "tournaments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Tournament {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @NotNull
    private String name;

    @ManyToOne
    private Season season;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinTable(
            name = "tournament_flights",
            joinColumns = @JoinColumn(name = "tournament_id"),
            inverseJoinColumns = @JoinColumn(name = "flight_id")
    )
    private List<Flight> flights = new ArrayList<>();

    private String strategyName; // e.g., "STABLEFORD"

    @Transient
    private IScoringStrategy scoringEngine;

    private boolean isFinished;

    @ElementCollection
    private Map<Long, Integer> awards = new HashMap<>();

    public TournamentDTO toDTO() {
        return new TournamentDTO(name, awards, id, season.getId());
    }
}
