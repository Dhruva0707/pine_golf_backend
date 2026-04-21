package com.pinewoods.score.tracker.services.course;

import com.pinewoods.score.tracker.dao.admin.PlayerRepository;
import com.pinewoods.score.tracker.dao.course.CourseHandicapRepository;
import com.pinewoods.score.tracker.dao.course.CourseRepository;
import com.pinewoods.score.tracker.dto.course.CourseDTO;
import com.pinewoods.score.tracker.entities.admin.Player;
import com.pinewoods.score.tracker.entities.course.Course;
import com.pinewoods.score.tracker.entities.course.CourseHandicap;
import com.pinewoods.score.tracker.services.admin.PlayerService;
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
    private final CourseHandicapRepository courseHandicapRepository;
    private final PlayerRepository playerRepository;

    public Course createCourse(CourseDTO dto) {

        double courseRating = dto.getCourseRating() == 0.0 ?
            dto.getPars().stream().mapToInt(Integer::intValue).sum() :
            dto.getCourseRating();

        double slopeRating = dto.getSlopeRating() == 0 ? 113.0 : dto.getSlopeRating();

        Course course = Course.builder()
                .name(dto.getName())
                .pars(dto.getPars())
                .indexes(dto.getIndexes())
                .courseRating(courseRating)
                .slopeRating(slopeRating)
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

    public CourseHandicap getCourseHandicap(Long playerId, Long courseId) {
        CourseHandicap.Id id = new CourseHandicap.Id(playerId, courseId);
        return courseHandicapRepository.findById(id)
                .orElseGet(() -> {
                    Player player = playerRepository.findById(playerId).orElseThrow();
                    Course course = courseRepository.findById(courseId).orElseThrow();
                    // calculate handicap for player and course
                    double scalingFactor = course.getSlopeRating()/113.0;
                    double additionFactor = course.getCourseRating() - course.getPars().stream().mapToInt(Integer::intValue).sum();
                    double handicap = scalingFactor * player.getHandicap() + additionFactor;

                    return updatePlayerHandicap(course.getId(), player.getId(), handicap);
                });
    }

    public List<CourseDTO> getAllCourses() {
        return courseRepository.findAll().stream().map(Course::toDTO).toList();
    }

    public CourseDTO getCourse(String name) {
        return courseRepository.findByName(name).map(Course::toDTO)
                .orElseThrow(() -> new RuntimeException("Course not found"));
    }

    public CourseDTO getCourse(Long id) {
        return courseRepository.findById(id).map(Course::toDTO)
                .orElseThrow(() -> new RuntimeException("Course not found"));
    }

    public CourseHandicap updatePlayerHandicap(Long courseId, Long playerId, double handicap) {
        CourseHandicap.Id id = new CourseHandicap.Id(playerId, courseId);
        CourseHandicap courseHandicap = CourseHandicap.builder()
                .id(id)
                .player(playerRepository.findById(playerId).orElseThrow())
                .course(courseRepository.findById(courseId).orElseThrow())
                .handicap(handicap)
                .build();
        return courseHandicapRepository.save(courseHandicap);
    }
}

