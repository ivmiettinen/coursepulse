package com.coursepulse.feedback;

import com.coursepulse.course.CourseOfferingNotFoundException;
import com.coursepulse.course.CourseOfferingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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

        Specification<Feedback> specification = FeedbackSpecifications.hasCourseOfferingId(courseOfferingId);
        if (query.getRating() != null) {
            specification = specification.and(FeedbackSpecifications.hasRating(query.getRating()));
        }
        if (query.getSearch() != null && !query.getSearch().isBlank()) {
            specification = specification.and(FeedbackSpecifications.commentContains(query.getSearch().strip()));
        }

        Page<FeedbackDto> feedbackPage = feedbackRepository
                .findAll(specification, PageRequest.of(query.getPage(), query.getSize(),
                        createSort(query.getSort())))
                .map(FeedbackMapper::toDto);

        return new FeedbackPageDto(feedbackPage.getContent(), feedbackPage.getNumber(), feedbackPage.getSize(),
                feedbackPage.getTotalElements(), feedbackPage.getTotalPages());
    }

    private Sort createSort(String requestedSort) {
        Sort sort = switch (requestedSort) {
            case "submittedAt,asc" -> Sort.by(Sort.Order.asc("submittedAt"));
            case "submittedAt,desc" -> Sort.by(Sort.Order.desc("submittedAt"));
            case "rating,asc" -> Sort.by(Sort.Order.asc("rating"), Sort.Order.desc("submittedAt"));
            case "rating,desc" -> Sort.by(Sort.Order.desc("rating"), Sort.Order.desc("submittedAt"));
            default -> throw new IllegalArgumentException("Unsupported feedback sort.");
        };
        // A unique final sort key keeps equal ratings and timestamps in a deterministic order.
        return sort.and(Sort.by(Sort.Order.desc("id")));
    }
}
