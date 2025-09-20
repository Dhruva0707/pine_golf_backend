package com.pinewoods.score.tracker.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/*
 * A team is defined is an abstract concept that players can be a part of
 */
@Entity
@Table(name = "teams")
public class Team {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL)
    private List<Player> players;

    //--- Constructors ---
    public Team() {}

    public Team(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Team(String name) {
        this(null, name);
    }

    // --- Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void setPlayers(List<Player> players) {
        this.players = players;
    }
}
