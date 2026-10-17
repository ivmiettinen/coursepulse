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
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@Sql("/sql/course-offering-api-data.sql")
@Sql(scripts = "/sql/course-offering-api-cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class CourseOfferingApiIntegrationTest {

    private final MockMvc mockMvc;

    CourseOfferingApiIntegrationTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
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
    void getFeedback_ratingFilter_returnsOnlyMatchingFeedbackForCourseOffering() throws Exception {
        mockMvc.perform(get("/api/course-offerings/{id}/feedback", 900001).queryParam("rating", "5"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "content": [
                            {
                              "id": 900002,
                              "rating": 5,
                              "comment": "Clear lectures and useful exercises.",
                              "submittedAt": "2026-09-25T12:00:00Z"
                            }
                          ],
                          "page": 0,
                          "size": 20,
                          "totalElements": 1,
                          "totalPages": 1
                        }
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void getFeedback_ratingFilterWithoutMatches_returnsEmptyPage() throws Exception {
        mockMvc.perform(get("/api/course-offerings/{id}/feedback", 900001).queryParam("rating", "2"))
                .andExpect(status().isOk())
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

    @ParameterizedTest
    @ValueSource(strings = {"clear", "CLEAR", "  Clear  "})
    void getFeedback_search_returnsCaseInsensitiveCommentMatches(String search) throws Exception {
        mockMvc.perform(get("/api/course-offerings/{id}/feedback", 900001).queryParam("search", search))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(900002))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ÄÄNI", "ääni"})
    @SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
    @Sql("/sql/feedback-search-finnish-case-data.sql")
    void getFeedback_finnishSearch_matchesCommentsRegardlessOfCase(String search) throws Exception {
        mockMvc.perform(get("/api/course-offerings/{id}/feedback", 900001).queryParam("search", search))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(900011))
                .andExpect(jsonPath("$.content[1].id").value(900010))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void getFeedback_blankSearch_returnsUnfilteredPage(String search) throws Exception {
        mockMvc.perform(get("/api/course-offerings/{id}/feedback", 900001).queryParam("search", search))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].comment").value(nullValue()));
    }

    @ParameterizedTest
    @CsvSource({"5,1", "3,0"})
    void getFeedback_searchWithRating_combinesBothFilters(int rating, int expectedCount) throws Exception {
        mockMvc.perform(get("/api/course-offerings/{id}/feedback", 900001)
                        .queryParam("search", "clear").queryParam("rating", Integer.toString(rating)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(expectedCount))
                .andExpect(jsonPath("$.totalElements").value(expectedCount))
                .andExpect(jsonPath("$.totalPages").value(expectedCount));
    }

    @Test
    @SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
    @Sql("/sql/feedback-search-rating-page-data.sql")
    void getFeedback_searchAndRatingWithPaging_returnsSecondPageAndFilteredTotal() throws Exception {
        // Three rows match both filters; other rows match only one filter or belong to another offering.
        mockMvc.perform(get("/api/course-offerings/{id}/feedback", 900001)
                        .queryParam("search", "clear")
                        .queryParam("rating", "5")
                        .queryParam("page", "1")
                        .queryParam("size", "1"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "content": [
                            {
                              "id": 900002,
                              "rating": 5,
                              "comment": "Clear lectures and useful exercises.",
                              "submittedAt": "2026-09-25T12:00:00Z"
                            }
                          ],
                          "page": 1,
                          "size": 1,
                          "totalElements": 3,
                          "totalPages": 3
                        }
                        """, JsonCompareMode.STRICT));
    }

    @ParameterizedTest
    @ValueSource(strings = {"no matching text", "' OR 1=1 --", "<img src=x onerror=alert(1)>"})
    void getFeedback_searchWithoutMatches_returnsEmptyPage(String search) throws Exception {
        mockMvc.perform(get("/api/course-offerings/{id}/feedback", 900001).queryParam("search", search))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"%", "_", "!", "100%", "part_1", "great!"})
    @SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
    @Sql("/sql/feedback-search-special-characters-data.sql")
    void getFeedback_searchSpecialCharacters_matchesLiteralText(String search) throws Exception {
        mockMvc.perform(get("/api/course-offerings/{id}/feedback", 900001).queryParam("search", search))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(900012))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getFeedback_searchAtMaximumLength_returnsOk() throws Exception {
        mockMvc.perform(get("/api/course-offerings/{id}/feedback", 900001).queryParam("search", "a".repeat(200)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void getFeedback_searchExceedingMaximumLength_returnsBadRequestProblemDetail() throws Exception {
        mockMvc.perform(get("/api/course-offerings/{id}/feedback", 900001).queryParam("search", "a".repeat(201)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[0].field").value("search"))
                .andExpect(jsonPath("$.errors[0].message").value("Search must not exceed 200 characters."))
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(jsonPath("$.trace").doesNotExist());
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
            "page=not-a-number|page|Must have a valid format.",
            "rating=0|rating|Rating must be at least 1.",
            "rating=6|rating|Rating must not exceed 5.",
            "rating=not-a-number|rating|Must have a valid format."
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
