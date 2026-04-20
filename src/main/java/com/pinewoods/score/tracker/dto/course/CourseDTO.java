package com.pinewoods.score.tracker.dto.course;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseDTO {
    private Long id;
    private String name;
    private List<Integer> pars;
    private List<Integer> indexes;

    private double slopeRating;
    private double courseRating;
}
