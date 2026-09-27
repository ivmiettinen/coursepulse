package com.coursepulse.course;

public final class CourseOfferingMapper {

    private CourseOfferingMapper() {
    }

    public static CourseOfferingDto toDto(CourseOffering offering) {
        Course course = offering.getCourse();

        return new CourseOfferingDto(
                offering.getId(),
                course.getId(),
                course.getCode(),
                course.getName(),
                offering.getAcademicYear(),
                offering.getPeriod()
        );
    }
}
