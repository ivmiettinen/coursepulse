package com.coursepulse.feedback;

import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

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

    public static Specification<Feedback> commentContains(String search) {
        // Treat LIKE wildcard characters and the escape character as literal search text.
        String escapedSearch = search.replace("!", "!!").replace("%", "!%").replace("_", "!_");
        String pattern = ("%" + escapedSearch + "%").toLowerCase(Locale.ROOT);
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(criteriaBuilder.lower(root.get("comment")), pattern, '!');
    }
}
