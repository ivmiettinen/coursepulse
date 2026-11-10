package com.coursepulse.course;

public final class CourseMapper {

    private CourseMapper() {
    }

    public static CourseDto toDto(Course course) {
        return new CourseDto(course.getId(), course.getCode(), course.getName());
    }
}
