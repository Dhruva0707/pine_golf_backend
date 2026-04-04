package com.pinewoods.score.tracker.entities.flight;

import com.pinewoods.score.tracker.dto.flight.FlightDTO;
import com.pinewoods.score.tracker.dto.flight.FlightScoreDTO;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "flights")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Flight {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @NotNull
    private Date date;

    @OneToMany(mappedBy = "flight", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<FlightScore> flightScores = new ArrayList<>();

    public FlightDTO toDTO() {
        List<FlightScoreDTO> scoreDTOs = new ArrayList<>();
        if (!flightScores.isEmpty()) {
            scoreDTOs = flightScores.stream()
                    .map(FlightScore::toDto)
                    .toList();
        }

        return new FlightDTO(date, scoreDTOs);
    }
}
