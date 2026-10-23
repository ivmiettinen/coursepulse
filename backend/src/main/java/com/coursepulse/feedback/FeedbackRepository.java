package com.coursepulse.feedback;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long>, JpaSpecificationExecutor<Feedback> {

    @Query("""
            select f.rating, count(f)
            from Feedback f
            where f.courseOffering.id = :courseOfferingId
            group by f.rating
            """)
    List<FeedbackRatingCountDto> countByRatingForCourseOffering(@Param("courseOfferingId") Long courseOfferingId);
}
