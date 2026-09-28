package com.coursepulse;

import com.coursepulse.course.Course;
import com.coursepulse.course.CourseOffering;
import com.coursepulse.feedback.Feedback;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class EntityValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @ParameterizedTest
    @MethodSource("invalidValues")
    void validateValue_invalidField_returnsReadableMessageWithResolvedLimit(
            Class<?> entityType, String field, Object value, String expectedMessage) {
        // Arrange: each case targets one existing constraint.

        // Act
        var violations = validator.validateValue(entityType, field, value);

        // Assert
        assertThat(violations).singleElement().satisfies(violation -> {
            assertThat(violation.getPropertyPath().toString()).isEqualTo(field);
            assertThat(violation.getMessage()).isEqualTo(expectedMessage);
        });
    }

    static Stream<Arguments> invalidValues() {
        return Stream.of(
                Arguments.of(Course.class, "code", " ", "Course code is required."),
                Arguments.of(Course.class, "code", "a".repeat(21), "Course code must not exceed 20 characters."),
                Arguments.of(Course.class, "name", null, "Course name is required."),
                Arguments.of(Course.class, "name", "a".repeat(201), "Course name must not exceed 200 characters."),
                Arguments.of(CourseOffering.class, "academicYear", "", "Academic year is required."),
                Arguments.of(CourseOffering.class, "academicYear", "a".repeat(21),
                        "Academic year must not exceed 20 characters."),
                Arguments.of(CourseOffering.class, "period", " ", "Period is required."),
                Arguments.of(CourseOffering.class, "period", "a".repeat(51),
                        "Period must not exceed 50 characters."),
                Arguments.of(Feedback.class, "rating", 0, "Rating must be at least 1."),
                Arguments.of(Feedback.class, "rating", 6, "Rating must not exceed 5."),
                Arguments.of(Feedback.class, "comment", "a".repeat(2001), "Comment must not exceed 2000 characters."),
                Arguments.of(Feedback.class, "submittedAt", null, "Submission time is required.")
        );
    }

    @Test
    void validateValue_untrustedComment_doesNotIncludeSubmittedTextInMessage() {
        // Arrange: the input contains markup, a URL, SQL-like text and an expression.
        String untrustedComment = "<script>alert(1)</script> https://example.invalid ' OR 1=1 -- 49 ".repeat(40);

        // Act
        var violations = validator.validateValue(Feedback.class, "comment", untrustedComment);

        // Assert
        assertThat(violations).singleElement().satisfies(violation ->
                assertThat(violation.getMessage()).isEqualTo("Comment must not exceed 2000 characters."));
    }

    @Test
    void validateValue_nullComment_remainsAllowed() {
        // Arrange
        String comment = null;

        // Act and assert
        assertThat(validator.validateValue(Feedback.class, "comment", comment)).isEmpty();
    }
}
