package com.coursepulse.course;

import com.coursepulse.PostgresTestConfiguration;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestConstructor;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(PostgresTestConfiguration.class)
@Transactional
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class CourseOfferingRepositoryTests {

    private final CourseRepository courseRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final EntityManager entityManager;

    CourseOfferingRepositoryTests(CourseRepository courseRepository,
                                  CourseOfferingRepository courseOfferingRepository,
                                  EntityManager entityManager) {
        this.courseRepository = courseRepository;
        this.courseOfferingRepository = courseOfferingRepository;
        this.entityManager = entityManager;
    }

    @Test
    void save_twoOfferingsForSameCourse_persistsFieldsRelationshipAndAuditTimestamps() {
        // Arrange
        Course course = courseRepository.saveAndFlush(new Course("CS202", "Data Structures and Algorithms"));
        Long courseId = course.getId();

        // Act
        CourseOffering first = courseOfferingRepository.saveAndFlush(
                new CourseOffering(course, "2026-2027", "1"));
        CourseOffering second = courseOfferingRepository.saveAndFlush(
                new CourseOffering(course, "2026-2027", "2"));
        Long firstId = first.getId();
        Long secondId = second.getId();
        entityManager.clear();

        // Assert
        assertThat(firstId).isNotNull();
        assertThat(secondId).isNotNull().isNotEqualTo(firstId);

        CourseOffering savedFirst = courseOfferingRepository.findById(firstId).orElseThrow();
        CourseOffering savedSecond = courseOfferingRepository.findById(secondId).orElseThrow();

        assertThat(savedFirst.getAcademicYear()).isEqualTo("2026-2027");
        assertThat(savedFirst.getPeriod()).isEqualTo("1");
        assertThat(savedSecond.getAcademicYear()).isEqualTo("2026-2027");
        assertThat(savedSecond.getPeriod()).isEqualTo("2");

        for (CourseOffering saved : new CourseOffering[]{savedFirst, savedSecond}) {
            assertThat(saved.getCourse().getId()).isEqualTo(courseId);
            assertThat(saved.getCourse().getCode()).isEqualTo("CS202");
            assertThat(saved.getCourse().getName()).isEqualTo("Data Structures and Algorithms");
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getUpdatedAt()).isEqualTo(saved.getCreatedAt());
        }
    }
}
