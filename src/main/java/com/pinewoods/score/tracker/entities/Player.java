package com.pinewoods.score.tracker.entities;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.Id;

/**
 * A Player is defined by his name, password, handicap, role and team
 */
@Entity
@Table(name="players")
public class Player {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "name is required")
    @Column(nullable = false, unique = true)
    private String name;

    @NotBlank(message = "password is required")
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    private int handicap;

    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;

    // --- Constructors ---
    public Player() {}

    public Player(Long id, String name, String password, Integer handicap, Role role, Team team) {
        this.name = name;
        this.password = password;
        this.handicap = handicap;
        this.role = role;
        this.team = team;
    }

    public Player(String name, String password, Integer handicap, Role role, Team team) {
        this(null, name, password, handicap, role, team);
    }

    // --- Getters and Setters ---
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public int getHandicap() {
        return handicap;
    }

    public void setHandicap(int handicap) {
        this.handicap = handicap;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }
}
