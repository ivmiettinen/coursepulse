INSERT INTO course (id, code, name)
VALUES (900001, 'API101', 'API test course');

INSERT INTO course_offering (id, course_id, academic_year, period)
VALUES
    (900001, 900001, '2026-2027', '1'),
    (900002, 900001, '2026-2027', '2'),
    (900003, 900001, '2026-2027', '3');

INSERT INTO feedback (id, course_offering_id, rating, comment, submitted_at)
VALUES
    (900001, 900001, 3, 'Earlier feedback', '2026-09-24T12:00:00Z'),
    (900002, 900001, 5, 'Clear lectures and useful exercises.', '2026-09-25T12:00:00Z'),
    (900003, 900001, 4, NULL, '2026-09-25T12:00:00Z'),
    (900004, 900002, 1, 'Different offering of the same course', '2026-09-26T12:00:00Z');
