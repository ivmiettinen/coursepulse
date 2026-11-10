package com.coursepulse.course;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseOfferingRepository courseOfferingRepository;

    public CourseService(CourseRepository courseRepository, CourseOfferingRepository courseOfferingRepository) {
        this.courseRepository = courseRepository;
        this.courseOfferingRepository = courseOfferingRepository;
    }

    public List<CourseDto> getCourses() {
        return courseRepository.findAllByOrderByCodeAsc().stream()
                .map(CourseMapper::toDto)
                .toList();
    }

    public List<CourseOfferingDto> getCourseOfferings(Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new CourseNotFoundException(courseId);
        }

        return courseOfferingRepository.findByCourseIdOrderByAcademicYearDescPeriodAscIdAsc(courseId).stream()
                .map(CourseOfferingMapper::toDto)
                .toList();
    }
}
