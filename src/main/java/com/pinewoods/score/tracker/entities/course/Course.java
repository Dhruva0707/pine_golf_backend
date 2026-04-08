package com.pinewoods.score.tracker.entities.course;

import com.pinewoods.score.tracker.dto.course.CourseDTO;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.validator.constraints.UniqueElements;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(unique = true)
    private String name;

    @ElementCollection
    @CollectionTable(name = "course_pars", joinColumns = @JoinColumn(name = "course_id"))
    @Column(name = "par")
    @Size(min = 18, max = 18, message = "Course must have exactly 18 pars")
    private List<Integer> pars = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "course_indexes", joinColumns = @JoinColumn(name = "course_id"))
    @Column(name = "stroke_index")
    @Size(min = 18, max = 18, message = "Course must have exactly 18 stroke indexes")
    @UniqueElements(message = "Stroke indexes must be unique")
    private List<Integer> indexes = new ArrayList<>();

    public CourseDTO toDTO() {
        return new CourseDTO(name, pars, indexes);
    }
}

