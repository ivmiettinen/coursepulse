package com.coursepulse.course;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CourseOfferingService {

    private final CourseOfferingRepository courseOfferingRepository;

    public CourseOfferingService(CourseOfferingRepository courseOfferingRepository) {
        this.courseOfferingRepository = courseOfferingRepository;
    }

    public CourseOfferingDto getCourseOffering(Long id) {
        CourseOffering offering = courseOfferingRepository.findById(id)
                .orElseThrow(() -> new CourseOfferingNotFoundException(id));

        return CourseOfferingMapper.toDto(offering);
    }
}
