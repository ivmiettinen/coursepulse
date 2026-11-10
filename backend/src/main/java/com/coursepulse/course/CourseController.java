package com.coursepulse.course;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public List<CourseDto> getCourses() {
        return courseService.getCourses();
    }

    @GetMapping("/{id}/offerings")
    public List<CourseOfferingDto> getCourseOfferings(@PathVariable Long id) {
        return courseService.getCourseOfferings(id);
    }
}
