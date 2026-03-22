package com.pinewoods.score.tracker.dao.course;

import com.pinewoods.score.tracker.entities.course.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    Optional<Course> findByName(String name);
    void deleteByName(String name);
}
