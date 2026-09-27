package com.coursepulse.feedback;

import com.coursepulse.course.CourseOfferingNotFoundException;
import com.coursepulse.course.CourseOfferingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final CourseOfferingRepository courseOfferingRepository;

    public FeedbackService(FeedbackRepository feedbackRepository,
                           CourseOfferingRepository courseOfferingRepository) {
        this.feedbackRepository = feedbackRepository;
        this.courseOfferingRepository = courseOfferingRepository;
    }

    public List<FeedbackDto> getFeedback(Long courseOfferingId) {
        if (!courseOfferingRepository.existsById(courseOfferingId)) {
            throw new CourseOfferingNotFoundException(courseOfferingId);
        }

        return feedbackRepository.findAllByCourseOfferingIdOrderBySubmittedAtDescIdDesc(courseOfferingId)
                .stream()
                .map(FeedbackMapper::toDto)
                .toList();
    }
}
