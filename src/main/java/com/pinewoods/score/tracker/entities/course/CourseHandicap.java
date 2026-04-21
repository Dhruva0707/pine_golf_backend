package com.pinewoods.score.tracker.entities.course;

import com.pinewoods.score.tracker.entities.admin.Player;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "course_handicaps")
@Getter
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
public class CourseHandicap {

    @EmbeddedId
    Id id;

    @NotNull
    private double handicap;

    @ManyToOne
    @JoinColumn(name = "player_id")
    @MapsId("playerId")
    @NotNull
    Player player;

    @ManyToOne
    @JoinColumn(name = "course_id")
    @MapsId("courseId")
    @NotNull
    Course course;


    @Embeddable
    @Getter
    @Setter
    @EqualsAndHashCode
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Id implements Serializable {
        private Long playerId;
        private Long courseId;
    }
}



