package com.coursepulse.feedback;

import com.coursepulse.PostgresTestConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@Sql({"/sql/course-offering-api-data.sql", "/sql/feedback-search-rating-page-data.sql"})
@Sql(scripts = "/sql/course-offering-api-cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class FeedbackSortingApiIntegrationTest {

    private final MockMvc mockMvc;

    FeedbackSortingApiIntegrationTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @ParameterizedTest
    @MethodSource("supportedSorts")
    void getFeedback_supportedSort_returnsStableOrderWithinCourseOffering(
            String sort, List<Integer> expectedIds) throws Exception {
        // Base feedback and additional query-test feedback form the expected ordering below.

        // Act and assert
        ResultActions response = mockMvc.perform(
                        get("/api/course-offerings/{id}/feedback", 900001).queryParam("sort", sort))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(expectedIds.size()))
                .andExpect(jsonPath("$.totalElements").value(7));
        for (int index = 0; index < expectedIds.size(); index++) {
            response.andExpect(jsonPath("$.content[%d].id".formatted(index)).value(expectedIds.get(index)));
        }
    }

    static Stream<Arguments> supportedSorts() {
        return Stream.of(
                Arguments.of("submittedAt,asc", List.of(900006, 900001, 900005, 900003, 900002, 900008, 900007)),
                Arguments.of("submittedAt,desc", List.of(900008, 900007, 900005, 900003, 900002, 900006, 900001)),
                Arguments.of("rating,asc", List.of(900007, 900001, 900003, 900008, 900005, 900002, 900006)),
                Arguments.of("rating,desc", List.of(900008, 900005, 900002, 900006, 900003, 900007, 900001)));
    }

    @Test
    void getFeedback_sortWithSearchRatingAndPaging_returnsFilteredSecondPage() throws Exception {
        // Arrange: ascending time orders the three matches as 900006, 900005, 900002.

        // Act and assert
        mockMvc.perform(get("/api/course-offerings/{id}/feedback", 900001)
                        .queryParam("search", "clear")
                        .queryParam("rating", "5")
                        .queryParam("sort", "submittedAt,asc")
                        .queryParam("page", "1")
                        .queryParam("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(900005))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "comment,asc", "id,desc", "rating", "rating,sideways",
            "rating,desc,submittedAt,asc", "rating,desc; DROP TABLE feedback", "courseOffering.id,asc"})
    void getFeedback_invalidSort_returnsBadRequestProblemDetail(String sort) throws Exception {
        // Act and assert
        mockMvc.perform(get("/api/course-offerings/{id}/feedback", 900001).queryParam("sort", sort))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Request parameters are invalid."))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("sort"))
                .andExpect(jsonPath("$.errors[0].message").value(
                        "Sort must be submittedAt,asc; submittedAt,desc; rating,asc; or rating,desc."))
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }
}
