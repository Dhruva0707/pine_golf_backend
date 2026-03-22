package com.pinewoods.score.tracker.services.course;

import com.pinewoods.score.tracker.dao.course.CourseRepository;
import com.pinewoods.score.tracker.dto.course.CourseDTO;
import com.pinewoods.score.tracker.entities.course.Course;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseService {

    private final CourseRepository courseRepository;

    public Course createCourse(CourseDTO dto) {
        Course course = Course.builder()
                .name(dto.getName())
                .pars(dto.getPars())
                .indexes(dto.getIndexes())
                .build();
        courseRepository.save(course);
        return course;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Course updateCourse(String courseName, CourseDTO dto) {
        Course course = courseRepository.findByName(courseName)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        course.setName(dto.getName());
        course.setPars(dto.getPars());
        course.setIndexes(dto.getIndexes());
        courseRepository.save(course);

        return course;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void deleteCourse(String name) {
        courseRepository.deleteByName(name);
    }

    public List<CourseDTO> getAllCourses() {
        return courseRepository.findAll().stream().map(Course::toDTO).toList();
    }

    public CourseDTO getCourse(String name) {
        return courseRepository.findByName(name).map(Course::toDTO)
                .orElseThrow(() -> new RuntimeException("Course not found"));
    }
}

