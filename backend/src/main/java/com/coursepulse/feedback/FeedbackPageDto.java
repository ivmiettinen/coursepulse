package com.coursepulse.feedback;

import java.util.List;

public record FeedbackPageDto(
        List<FeedbackDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
