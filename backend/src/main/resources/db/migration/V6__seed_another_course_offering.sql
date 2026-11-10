WITH new_offering AS (
    INSERT INTO course_offering (course_id, academic_year, period)
    SELECT id, '2026-2027', 'Period 1'
    FROM course
    WHERE code = 'CS101'
    RETURNING id
)
INSERT INTO course_feedback (
    course_offering_id, rating, comment, submitted_at
)
VALUES
    (
        (SELECT id FROM new_offering),
        5,
        'The weekly exercises helped me learn.',
        TIMESTAMPTZ '2026-10-20 12:00:00+00'
    ),
    (
        (SELECT id FROM new_offering),
        4,
        'Clear examples and useful feedback.',
        TIMESTAMPTZ '2026-10-21 09:00:00+00'
    ),
    (
        (SELECT id FROM new_offering),
        3,
        'More time for the final assignment would help.',
        TIMESTAMPTZ '2026-10-22 15:00:00+00'
    );