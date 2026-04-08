package com.pinewoods.score.tracker.dto.course;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseDTO {
    private String name;
    private List<Integer> pars;
    private List<Integer> indexes;
}

