package com.coursepulse.common.web;

import com.coursepulse.course.CourseOfferingNotFoundException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(CourseOfferingNotFoundException.class)
    public ProblemDetail handleCourseOfferingNotFound(CourseOfferingNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    // Describe the expected format without echoing rejected input or internal exception details.
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail errorResponse = ProblemDetail.forStatusAndDetail(
                status, "A request parameter has an invalid format.");

        if (exception instanceof MethodArgumentTypeMismatchException argumentException) {
            String message = "Must have a valid format.";
            Class<?> requiredType = argumentException.getRequiredType();

            if (requiredType == Long.class || requiredType == Integer.class) {
                message = "Must be a valid integer.";
            }

            ParameterError error = new ParameterError(argumentException.getName(), message);
            errorResponse.setProperty("errors", List.of(error));
        }

        return handleExceptionInternal(exception, errorResponse, headers, status, request);
    }

    private record ParameterError(String field, String message) {
    }
}
