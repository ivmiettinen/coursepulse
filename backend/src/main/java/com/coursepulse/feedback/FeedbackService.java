package com.coursepulse.feedback;

import com.coursepulse.course.CourseOfferingNotFoundException;
import com.coursepulse.course.CourseOfferingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public FeedbackPageDto getFeedback(Long courseOfferingId, FeedbackQuery query) {
        if (!courseOfferingRepository.existsById(courseOfferingId)) {
            throw new CourseOfferingNotFoundException(courseOfferingId);
        }

        Page<FeedbackDto> feedbackPage = feedbackRepository
                .findAllByCourseOfferingIdOrderBySubmittedAtDescIdDesc(
                        courseOfferingId, PageRequest.of(query.getPage(), query.getSize()))
                .map(FeedbackMapper::toDto);


        return new FeedbackPageDto(feedbackPage.getContent(), feedbackPage.getNumber(), feedbackPage.getSize(),
                feedbackPage.getTotalElements(), feedbackPage.getTotalPages());

    }

}
