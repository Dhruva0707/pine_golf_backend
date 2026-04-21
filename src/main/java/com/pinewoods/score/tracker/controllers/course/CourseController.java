package com.pinewoods.score.tracker.controllers.course;

import com.pinewoods.score.tracker.controllers.admin.utilities.ControllerUtilities;
import com.pinewoods.score.tracker.dto.course.CourseDTO;
import com.pinewoods.score.tracker.entities.course.Course;
import com.pinewoods.score.tracker.services.course.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
@Tag(name = "Course Management", description = "Endpoints for managing the golf course lifecycle")
public class CourseController {

    private final CourseService courseService;

    // ==================== Create Course ====================
    @Operation(
            summary = "Create a new course",
            description = "Creates a new golf course with name, pars, and indexes",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Course created",
                            content = @Content(schema = @Schema(implementation = CourseDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid input")
            }
    )
    @PostMapping
    public ResponseEntity<CourseDTO> createCourse(@RequestBody @Valid CourseDTO dto) {
        Course course = courseService.createCourse(dto);
        URI resourceUri = ControllerUtilities.createResourceURI("id", course.getId());
        return ResponseEntity.created(resourceUri).body(course.toDTO());
    }

    // ===================Get courses =========================
    @Operation(
            summary = "Get all courses",
            description = "Retrieves all golf courses",
            responses = {
                    @ApiResponse(responseCode = "200", description = "List of courses",
                            content = @Content(schema = @Schema(implementation = CourseDTO.class)))
            }
    )
    @GetMapping
    public ResponseEntity<List<CourseDTO>> getAllCourses() {
        return ResponseEntity.ok(courseService.getAllCourses());
    }

    @Operation(
            summary = "Get a course by name",
            description = "Retrieves a single course by its name",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Course found",
                            content = @Content(schema = @Schema(implementation = CourseDTO.class))),
                    @ApiResponse(responseCode = "404", description = "Course not found")
            }
    )
    @GetMapping("/{name}")
    public ResponseEntity<CourseDTO> getCourse(@PathVariable String name) {
        return ResponseEntity.ok(courseService.getCourse(name));
    }

    // ===================Update course ===================
    @Operation(
            summary = "Update an existing course",
            description = "Updates pars and indexes for a course identified by name",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Course updated",
                            content = @Content(schema = @Schema(implementation = Course.class))),
                    @ApiResponse(responseCode = "404", description = "Course not found")
            }
    )
    @PutMapping("/{name}")
    public ResponseEntity<Course> updateCourse(@PathVariable String name, @RequestBody @Valid CourseDTO dto) {
        return ResponseEntity.ok(courseService.updateCourse(name, dto));
    }

    // ===================Delete course ===================
    @Operation(
        summary = "Delete a course",
        description = "Deletes a course by name",
        responses = {
            @ApiResponse(responseCode = "204", description = "Course deleted"),
            @ApiResponse(responseCode = "404", description = "Course not found")
        }
    )
    @DeleteMapping("/{name}")
    public ResponseEntity<Void> deleteCourse(@PathVariable String name) {
        courseService.deleteCourse(name);
        return ResponseEntity.noContent().build();
    }
}

