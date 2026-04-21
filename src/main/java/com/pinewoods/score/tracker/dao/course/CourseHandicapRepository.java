package com.pinewoods.score.tracker.dao.course;

import com.pinewoods.score.tracker.entities.course.Course;
import com.pinewoods.score.tracker.entities.course.CourseHandicap;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseHandicapRepository extends JpaRepository<CourseHandicap, CourseHandicap.Id> {
    Optional<CourseHandicap> findByPlayerIdAndCourseId(Long playerId, Long courseId);
}
