package com.coursepulse.course;

import com.coursepulse.PostgresTestConfiguration;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.data.auditing.CurrentDateTimeProvider;
import org.springframework.test.context.TestConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(PostgresTestConfiguration.class)
@Transactional
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class CourseRepositoryTests {

    private final CourseRepository courseRepository;

    private final EntityManager entityManager;

    private final AuditingHandler auditingHandler;

    CourseRepositoryTests(CourseRepository courseRepository,
                          EntityManager entityManager,
                          AuditingHandler auditingHandler) {
        this.courseRepository = courseRepository;
        this.entityManager = entityManager;
        this.auditingHandler = auditingHandler;
    }

    @AfterEach
    void restoreAuditClock() {
        auditingHandler.setDateTimeProvider(CurrentDateTimeProvider.INSTANCE);
    }

    @Test
    void save_newCourse_persistsCourseWithAuditTimestamps() {
        // Arrange
        Instant createdAt = Instant.parse("2026-09-12T09:00:00Z");
        auditingHandler.setDateTimeProvider(() -> Optional.of(createdAt));

        // Act
        Course course = courseRepository.saveAndFlush(new Course("TEST101", "Original name"));
        Long courseId = course.getId();

        // Assert: read from PostgreSQL instead of the persistence context.
        assertThat(courseId).isNotNull();
        entityManager.clear();
        Course saved = courseRepository.findById(courseId).orElseThrow();
        assertThat(saved.getCode()).isEqualTo("TEST101");
        assertThat(saved.getName()).isEqualTo("Original name");
        assertThat(saved.getCreatedAt()).isEqualTo(createdAt);
        assertThat(saved.getUpdatedAt()).isEqualTo(createdAt);
    }

    @Test
    void update_existingCourse_updatesNameAndLastModifiedTimestampOnly() {
        // Arrange
        Instant createdAt = Instant.parse("2026-09-12T09:00:00Z");
        Instant updatedAt = Instant.parse("2026-09-12T10:00:00Z");
        auditingHandler.setDateTimeProvider(() -> Optional.of(createdAt));
        Course course = courseRepository.saveAndFlush(new Course("TEST101", "Original name"));
        Long courseId = course.getId();
        entityManager.clear();
        Course saved = courseRepository.findById(courseId).orElseThrow();

        auditingHandler.setDateTimeProvider(() -> Optional.of(updatedAt));

        // Act
        saved.setName("Updated name");
        courseRepository.flush();

        // Assert: read from PostgreSQL instead of the persistence context.
        entityManager.clear();
        Course updated = courseRepository.findById(courseId).orElseThrow();
        assertThat(updated.getCode()).isEqualTo("TEST101");
        assertThat(updated.getName()).isEqualTo("Updated name");
        assertThat(updated.getCreatedAt()).isEqualTo(createdAt);
        assertThat(updated.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void save_nameAtMaximumLength_persistsName() {
        // Arrange
        String maximumLengthName = "a".repeat(200);

        // Act
        Course course = courseRepository.saveAndFlush(new Course("TEST201", maximumLengthName));
        Long courseId = course.getId();
        entityManager.clear();

        // Assert
        Course saved = courseRepository.findById(courseId).orElseThrow();
        assertThat(saved.getName()).isEqualTo(maximumLengthName);
    }

    @Test
    void save_nameExceedingMaximumLength_throwsConstraintViolationException() {
        // Arrange
        Course courseWithTooLongName = new Course("TEST202", "a".repeat(201));

        // Act and assert
        assertThatThrownBy(() -> courseRepository.saveAndFlush(courseWithTooLongName))
                .isInstanceOfSatisfying(ConstraintViolationException.class, exception ->
                        assertThat(exception.getConstraintViolations())
                                .anySatisfy(violation -> {
                                    assertThat(violation.getPropertyPath().toString()).isEqualTo("name");
                                    assertThat(violation.getConstraintDescriptor().getAnnotation())
                                            .isInstanceOf(jakarta.validation.constraints.Size.class);
                                }));
    }

}
