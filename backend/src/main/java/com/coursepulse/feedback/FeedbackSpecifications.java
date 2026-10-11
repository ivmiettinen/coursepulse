package com.coursepulse.feedback;

import org.springframework.data.jpa.domain.Specification;

public final class FeedbackSpecifications {

    private FeedbackSpecifications() {
    }

    public static Specification<Feedback> hasCourseOfferingId(Long courseOfferingId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(
                root.get("courseOffering").get("id"), courseOfferingId);
    }

    public static Specification<Feedback> hasRating(Integer rating) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("rating"), rating);
    }
}