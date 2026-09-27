package com.coursepulse.feedback;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    List<Feedback> findAllByCourseOfferingIdOrderBySubmittedAtDescIdDesc(Long courseOfferingId);
}
