package com.coursepulse.course;

import com.coursepulse.PostgresTestConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.nullValue;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@Sql("/sql/course-offering-api-data.sql")
@Sql(scripts = "/sql/course-offering-api-cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class CourseOfferingApiIntegrationTest {

    private final MockMvc mockMvc;
    private final JdbcTemplate jdbcTemplate;

    CourseOfferingApiIntegrationTest(MockMvc mockMvc, JdbcTemplate jdbcTemplate) {
        this.mockMvc = mockMvc;
        this.jdbcTemplate = jdbcTemplate;
    }

    // @Sql runs course-offering-api-data.sql before each test and
    // course-offering-api-cleanup.sql afterwards; this MockMvc test has no test-wide transaction.
    @Test
    void getCourseOffering_existingOffering_returnsCourseAndOfferingDetails() throws Exception {
        // Arrange
        long offeringId = 900001;

        // Act and assert
        mockMvc.perform(get("/api/course-offerings/{id}", offeringId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "id": 900001,
                          "courseId": 900001,
                          "courseCode": "API101",
                          "courseName": "API test course",
                          "academicYear": "2026-2027",
                          "period": "1"
                        }
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void getFeedback_existingCourseOffering_returnsFirstPageWithStableOrderAndTotalCount() throws Exception {
        // Arrange: fixture includes a null comment, equal timestamps and another offering of the same course.
        long offeringId = 900001;

        // Act and assert
        mockMvc.perform(get("/api/course-offerings/{id}/feedback", offeringId).queryParam("size", "2"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "content": [
                            {
                            "id": 900003,
                            "rating": 4,
                            "comment": null,
                            "submittedAt": "2026-09-25T12:00:00Z"
                            },
                            {
                            "id": 900002,
                            "rating": 5,
                            "comment": "Clear lectures and useful exercises.",
                            "submittedAt": "2026-09-25T12:00:00Z"
                            }
                          ],
                          "page": 0,
                          "size": 2,
                          "totalElements": 3,
                          "totalPages": 2
                        }
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void getFeedback_secondPage_returnsRemainingFeedbackAndTotalCount() throws Exception {
        mockMvc.perform(get("/api/course-offerings/{id}/feedback", 900001)
                        .queryParam("page", "1")
                        .queryParam("size", "2"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "content": [
                            {
                              "id": 900001,
                              "rating": 3,
                              "comment": "Earlier feedback",
                              "submittedAt": "2026-09-24T12:00:00Z"
                            }
                          ],
                          "page": 1,
                          "size": 2,
                          "totalElements": 3,
                          "totalPages": 2
                        }
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void getFeedback_courseOfferingWithoutFeedback_returnsEmptyPage() throws Exception {
        // Arrange
        long emptyOfferingId = 900003;

        // Act and assert
        mockMvc.perform(get("/api/course-offerings/{id}/feedback", emptyOfferingId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "content": [],
                          "page": 0,
                          "size": 20,
                          "totalElements": 0,
                          "totalPages": 0
                        }
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void getFeedback_withoutPagingParameters_usesFirstPageAndDefaultSize() throws Exception {
        mockMvc.perform(get("/api/course-offerings/{id}/feedback", 900001))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.content.length()").value(3));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "page=-1|page|Page must be at least 0.",
            "size=0|size|Size must be at least 1.",
            "size=101|size|Size must not exceed 100.",
            "page=2147483647&size=100|offsetWithinRange|Requested page is too large.",
            "page=not-a-number|page|Must have a valid format."
    }, delimiter = '|')
    void getFeedback_invalidPagingParameter_returnsBadRequestProblemDetail(
            String query, String field, String message) throws Exception {
        MockHttpServletRequestBuilder request = get("/api/course-offerings/{id}/feedback", 900001);
        for (String parameter : query.split("&")) {
            String[] nameAndValue = parameter.split("=", 2);
            request.queryParam(nameAndValue[0], nameAndValue[1]);
        }

        mockMvc.perform(request)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("Request parameters are invalid."))
                .andExpect(jsonPath("$.errors[?(@.field == '%s')].message".formatted(field)).value(message))
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/feedback"})
    void get_missingOffering_returnsNotFoundProblemDetail(String suffix) throws Exception {
        // Arrange
        long missingOfferingId = Long.MAX_VALUE;
        String path = "/api/course-offerings/" + missingOfferingId + suffix;

        // Act and assert
        mockMvc.perform(get(path))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(content().json("""
                        {
                          "title": "Not Found",
                          "status": 404,
                          "detail": "Course offering %d was not found.",
                          "instance": "%s"
                        }
                        """.formatted(missingOfferingId, path), JsonCompareMode.STRICT));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/feedback"})
    void get_nonNumericOfferingId_returnsBadRequestProblemDetail(String suffix) throws Exception {
        // Arrange
        String path = "/api/course-offerings/not-a-number" + suffix;

        // Act and assert
        mockMvc.perform(get(path))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(content().json("""
                        {
                          "title": "Bad Request",
                          "status": 400,
                          "detail": "A request parameter has an invalid format.",
                          "instance": "%s",
                          "errors": [{"field": "id", "message": "Must be a valid integer."}]
                        }
                        """.formatted(path), JsonCompareMode.STRICT));
    }

    @ParameterizedTest
    @ValueSource(strings = {"' OR 1=1 --", "<img src=x onerror=alert(1)>", "9223372036854775808"})
    void get_untrustedOrOverflowingId_returnsSafeParameterMessage(String invalidId) throws Exception {
        // Arrange: the instance URI identifies the request; messages must not repeat its rejected input.

        // Act and assert
        mockMvc.perform(get("/api/course-offerings/{id}", invalidId))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("A request parameter has an invalid format."))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("id"))
                .andExpect(jsonPath("$.errors[0].message").value("Must be a valid integer."))
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }
}
