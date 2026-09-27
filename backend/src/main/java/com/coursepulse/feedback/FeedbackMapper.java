package com.coursepulse.feedback;

public final class FeedbackMapper {

    private FeedbackMapper() {
    }

    public static FeedbackDto toDto(Feedback feedback) {
        return new FeedbackDto(
                feedback.getId(),
                feedback.getRating(),
                feedback.getComment(),
                feedback.getSubmittedAt()
        );
    }
}
