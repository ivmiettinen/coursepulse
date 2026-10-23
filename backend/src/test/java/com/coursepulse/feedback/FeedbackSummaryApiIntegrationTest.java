package com.coursepulse.feedback;

import com.coursepulse.PostgresTestConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@Sql("/sql/feedback-summary-data.sql")
@Sql(scripts = "/sql/feedback-summary-cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class FeedbackSummaryApiIntegrationTest {

    private final MockMvc mockMvc;

    FeedbackSummaryApiIntegrationTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void getSummary_existingCourseOffering_returnsCountWeightedAverageAndDistribution() throws Exception {
        // Arrange: ratings are 1, 2, 2, 3, 4 and 5; include the null-comment feedback.
        // The rating sum is 17 and count is 6, so averaging distinct ratings would be incorrect.

        // Act and assert
        mockMvc.perform(get("/api/course-offerings/{id}/feedback-summary", 910001))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "totalFeedback": 6,
                          "averageRating": 2.8333333333333335,
                          "ratingDistribution": [
                            {"rating": 1, "count": 1},
                            {"rating": 2, "count": 2},
                            {"rating": 3, "count": 1},
                            {"rating": 4, "count": 1},
                            {"rating": 5, "count": 1}
                          ]
                        }
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void getSummary_otherOfferingOfSameCourse_returnsOnlyItsFeedbackAndZeroCounts() throws Exception {
        // Arrange: period 2 has only one five-star feedback.

        // Act and assert
        mockMvc.perform(get("/api/course-offerings/{id}/feedback-summary", 910002))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "totalFeedback": 1,
                          "averageRating": 5.0,
                          "ratingDistribution": [
                            {"rating": 1, "count": 0},
                            {"rating": 2, "count": 0},
                            {"rating": 3, "count": 0},
                            {"rating": 4, "count": 0},
                            {"rating": 5, "count": 1}
                          ]
                        }
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void getSummary_offeringWithoutFeedback_returnsNullAverageAndZeroCounts() throws Exception {
        // Act and assert
        mockMvc.perform(get("/api/course-offerings/{id}/feedback-summary", 910003))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "totalFeedback": 0,
                          "averageRating": null,
                          "ratingDistribution": [
                            {"rating": 1, "count": 0},
                            {"rating": 2, "count": 0},
                            {"rating": 3, "count": 0},
                            {"rating": 4, "count": 0},
                            {"rating": 5, "count": 0}
                          ]
                        }
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void getSummary_feedbackListParameters_doNotFilterOrPaginateSummary() throws Exception {
        // Act and assert: these parameters affect the feedback list, not the summary.
        mockMvc.perform(get("/api/course-offerings/{id}/feedback-summary", 910001)
                        .queryParam("search", "clear")
                        .queryParam("rating", "5")
                        .queryParam("page", "1")
                        .queryParam("size", "1")
                        .queryParam("sort", "rating,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFeedback").value(6))
                .andExpect(jsonPath("$.averageRating").value(17.0 / 6))
                .andExpect(jsonPath("$.ratingDistribution.length()").value(5));
    }

    @Test
    void getSummary_missingCourseOffering_returnsNotFoundProblemDetail() throws Exception {
        // Act and assert
        mockMvc.perform(get("/api/course-offerings/{id}/feedback-summary", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Course offering " + Long.MAX_VALUE + " was not found."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-number", "9223372036854775808"})
    void getSummary_invalidCourseOfferingId_returnsBadRequestProblemDetail(String invalidId) throws Exception {
        // Act and assert
        mockMvc.perform(get("/api/course-offerings/{id}/feedback-summary", invalidId))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[0].field").value("id"))
                .andExpect(jsonPath("$.errors[0].message").value("Must be a valid integer."));
    }
}
