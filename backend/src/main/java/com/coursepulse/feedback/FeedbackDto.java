package com.coursepulse.feedback;

import java.time.Instant;

public record FeedbackDto(
        Long id,
        int rating,
        String comment,
        Instant submittedAt
) {}
