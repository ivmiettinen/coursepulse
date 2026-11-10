package com.coursepulse.course;

import com.coursepulse.PostgresTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@Transactional
@Sql("/sql/course-list-api-data.sql")
class CourseApiIntegrationTest {

    private final MockMvc mockMvc;

    CourseApiIntegrationTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    // Fixture changes, including removal of demo data, roll back after each test.
    @Test
    void getCourses_returnsAllCoursesInCodeOrderIncludingCourseWithoutOfferings() throws Exception {
        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        [
                          {"id": 910002, "code": "AA101", "name": "Another course"},
                          {"id": 910003, "code": "MM101", "name": "Course without offerings"},
                          {"id": 910001, "code": "ZZ101", "name": "Course with offerings"}
                        ]
                        """, JsonCompareMode.STRICT));
    }

    @Test
    @Sql(statements = {"DELETE FROM course_feedback", "DELETE FROM course_offering", "DELETE FROM course"})
    void getCourses_withoutCourses_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]", JsonCompareMode.STRICT));
    }

    @Test
    void getCourseOfferings_returnsOnlySelectedCourseInYearPeriodAndIdOrder() throws Exception {
        mockMvc.perform(get("/api/courses/{id}/offerings", 910001))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[*].id", contains(910003, 910004, 910005, 910002, 910001)))
                .andExpect(jsonPath("$[0].length()").value(6))
                .andExpect(jsonPath("$[0].courseId").value(910001))
                .andExpect(jsonPath("$[0].courseCode").value("ZZ101"))
                .andExpect(jsonPath("$[0].courseName").value("Course with offerings"))
                .andExpect(jsonPath("$[0].academicYear").value("2026-2027"))
                .andExpect(jsonPath("$[0].period").value("Period 1"));
    }

    @Test
    void getCourseOfferings_courseWithoutOfferings_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/courses/{id}/offerings", 910003))
                .andExpect(status().isOk())
                .andExpect(content().json("[]", JsonCompareMode.STRICT));
    }

    @Test
    void getCourseOfferings_missingCourse_returnsNotFoundProblemDetail() throws Exception {
        mockMvc.perform(get("/api/courses/{id}/offerings", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(content().json("""
                        {
                          "title": "Not Found",
                          "status": 404,
                          "detail": "Course 9223372036854775807 was not found.",
                          "instance": "/api/courses/9223372036854775807/offerings"
                        }
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void getCourseOfferings_nonNumericCourseId_returnsBadRequestProblemDetail() throws Exception {
        mockMvc.perform(get("/api/courses/not-a-number/offerings"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(content().json("""
                        {
                          "title": "Bad Request",
                          "status": 400,
                          "detail": "A request parameter has an invalid format.",
                          "instance": "/api/courses/not-a-number/offerings",
                          "errors": [{"field": "id", "message": "Must be a valid integer."}]
                        }
                        """, JsonCompareMode.STRICT));
    }
}
