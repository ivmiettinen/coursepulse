package com.coursepulse.course;

public class CourseOfferingNotFoundException extends RuntimeException {

    public CourseOfferingNotFoundException(Long id) {
        super("Course offering " + id + " was not found.");
    }
}
