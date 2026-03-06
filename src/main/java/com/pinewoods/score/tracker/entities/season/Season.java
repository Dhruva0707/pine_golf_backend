package com.pinewoods.score.tracker.entities.season;

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
    @Column(unique = true)
    private String name;

    @OneToMany(mappedBy = "season", cascade = CascadeType.ALL)
    private List<Tournament> tournaments = new ArrayList<>();

    @OneToMany(mappedBy = "season", cascade = CascadeType.ALL)
    private List<TeamStanding> standings = new ArrayList<>();
}
