package com.coursepulse.feedback;

import com.coursepulse.PostgresTestConfiguration;
import com.coursepulse.course.Course;
import com.coursepulse.course.CourseOffering;
import com.coursepulse.course.CourseOfferingRepository;
import com.coursepulse.course.CourseRepository;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(PostgresTestConfiguration.class)
@Transactional
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class FeedbackRepositoryTests {

    private final CourseRepository courseRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final FeedbackRepository feedbackRepository;
    private final EntityManager entityManager;

    FeedbackRepositoryTests(CourseRepository courseRepository,
                            CourseOfferingRepository courseOfferingRepository,
                            FeedbackRepository feedbackRepository,
                            EntityManager entityManager) {
        this.courseRepository = courseRepository;
        this.courseOfferingRepository = courseOfferingRepository;
        this.feedbackRepository = feedbackRepository;
        this.entityManager = entityManager;
    }

    @Test
    void save_validFeedback_persistsFieldsRelationshipAndAuditTimestamps() {
        // Arrange
        Course course = courseRepository.saveAndFlush(new Course("TEST301", "Distributed Systems"));
        CourseOffering offering = courseOfferingRepository.saveAndFlush(
                new CourseOffering(course, "2026-2027", "3"));
        Instant submittedAt = Instant.parse("2026-09-25T12:00:00Z");

        // Act
        Feedback feedback = feedbackRepository.saveAndFlush(
                new Feedback(offering, 5, "Clear lectures and useful exercises.", submittedAt));
        Long feedbackId = feedback.getId();
        entityManager.clear();

        // Assert
        Feedback saved = feedbackRepository.findById(feedbackId).orElseThrow();
        assertThat(saved.getRating()).isEqualTo(5);
        assertThat(saved.getComment()).isEqualTo("Clear lectures and useful exercises.");
        assertThat(saved.getSubmittedAt()).isEqualTo(submittedAt);
        assertThat(saved.getCourseOffering().getId()).isEqualTo(offering.getId());
        assertThat(saved.getCourseOffering().getCourse().getId()).isEqualTo(course.getId());
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isEqualTo(saved.getCreatedAt());
    }

    @Test
    void save_feedbackWithoutComment_allowsNullComment() {
        // Arrange
        Course course = courseRepository.saveAndFlush(new Course("TEST302", "Operating Systems"));
        CourseOffering offering = courseOfferingRepository.saveAndFlush(
                new CourseOffering(course, "2026-2027", "4"));

        // Act
        Feedback feedback = feedbackRepository.saveAndFlush(
                new Feedback(offering, 4, null, Instant.parse("2026-09-25T13:00:00Z")));
        Long feedbackId = feedback.getId();
        entityManager.clear();

        // Assert
        Feedback saved = feedbackRepository.findById(feedbackId).orElseThrow();
        assertThat(saved.getComment()).isNull();
    }

    @Test
    void save_ratingOutsideAllowedRange_throwsConstraintViolationException() {
        // Arrange
        Course course = courseRepository.saveAndFlush(new Course("TEST303", "Computer Networks"));
        CourseOffering offering = courseOfferingRepository.saveAndFlush(
                new CourseOffering(course, "2026-2027", "5"));
        Feedback feedback = new Feedback(
                offering, 6, "Invalid rating", Instant.parse("2026-09-25T14:00:00Z"));

        // Act and assert
        assertThatThrownBy(() -> feedbackRepository.saveAndFlush(feedback))
                .isInstanceOf(ConstraintViolationException.class);
    }
}
