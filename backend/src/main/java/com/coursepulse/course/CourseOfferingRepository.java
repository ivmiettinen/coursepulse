package com.coursepulse.course;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseOfferingRepository extends JpaRepository<CourseOffering, Long> {

    @Override
    @EntityGraph(attributePaths = "course")
    Optional<CourseOffering> findById(Long id);

    @EntityGraph(attributePaths = "course")
    List<CourseOffering> findByCourseIdOrderByAcademicYearDescPeriodAscIdAsc(Long courseId);
}
