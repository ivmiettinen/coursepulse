CREATE INDEX idx_feedback_offering_submitted_at_id
    ON feedback (course_offering_id, submitted_at DESC, id DESC);
