package com.coursepulse.course;

public record CourseOfferingDto(
        Long id,
        Long courseId,
        String courseCode,
        String courseName,
        String academicYear,
        String period
) {}
