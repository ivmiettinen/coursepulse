-- All data for FeedbackSummaryApiIntegrationTest, independent of other test fixtures.
INSERT INTO course (id, code, name)
VALUES (910001, 'SUMMARY101', 'Feedback summary test course');

-- Period 1 has six ratings, period 2 has one, and period 3 has no feedback.
INSERT INTO course_offering (id, course_id, academic_year, period)
VALUES
    (910001, 910001, '2026-2027', '1'),
    (910002, 910001, '2026-2027', '2'),
    (910003, 910001, '2026-2027', '3');

-- Ratings 1, 2, 2, 3, 4 and 5 verify the weighted average, including a null comment.
-- The last row belongs to period 2 and must not affect period 1's summary.
INSERT INTO course_feedback (id, course_offering_id, rating, comment, submitted_at)
VALUES
    (910001, 910001, 1, 'Too few examples.', '2026-09-26T12:00:00Z'),
    (910002, 910001, 2, 'More practice needed.', '2026-09-26T13:00:00Z'),
    (910003, 910001, 2, 'The pace was too fast.', '2026-09-26T14:00:00Z'),
    (910004, 910001, 3, 'Earlier feedback', '2026-09-24T12:00:00Z'),
    (910005, 910001, 4, NULL, '2026-09-25T12:00:00Z'),
    (910006, 910001, 5, 'Clear lectures and useful exercises.', '2026-09-25T12:00:00Z'),
    (910007, 910002, 5, 'Feedback for the same course in period 2', '2026-09-26T12:00:00Z');
