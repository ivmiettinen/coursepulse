package com.coursepulse.feedback;

import com.coursepulse.course.CourseOfferingNotFoundException;
import com.coursepulse.course.CourseOfferingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public FeedbackSummaryDto getFeedbackSummary(Long courseOfferingId) {
        if (!courseOfferingRepository.existsById(courseOfferingId)) {
            throw new CourseOfferingNotFoundException(courseOfferingId);
        }

        Map<Integer, Long> countsByRating = feedbackRepository
                .countByRatingForCourseOffering(courseOfferingId)
                .stream()
                .collect(Collectors.toMap(
                        FeedbackRatingCountDto::rating,
                        FeedbackRatingCountDto::count));

        List<FeedbackRatingCountDto> ratingDistribution = new ArrayList<>();
        long totalFeedback = 0;
        long ratingSum = 0;
        // Include all ratings from 1 to 5; use zero when this course offering has no feedback with that rating.
        for (int rating = 1; rating <= 5; rating++) {
            long count = countsByRating.getOrDefault(rating, 0L);
            ratingDistribution.add(new FeedbackRatingCountDto(rating, count));
            totalFeedback += count;
            ratingSum += rating * count;
        }

        Double averageRating = null;

        if(totalFeedback > 0 ) {
            double weightedRatingSum = ratingSum;
            averageRating = weightedRatingSum / totalFeedback;
        }

        return new FeedbackSummaryDto(totalFeedback, averageRating, List.copyOf(ratingDistribution));
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
