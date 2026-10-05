INSERT INTO course (code, name)
VALUES
    ('CS101', 'Introduction to Programming'),
    ('CS204', 'Databases'),
    ('CS305', 'Software Engineering');

INSERT INTO course_offering (course_id, academic_year, period)
VALUES
    (
        (SELECT id FROM course WHERE code = 'CS101'),
        '2025-2026',
        'Period 1'
    ),
    (
        (SELECT id FROM course WHERE code = 'CS204'),
        '2025-2026',
        'Period 2'
    ),
    (
        (SELECT id FROM course WHERE code = 'CS305'),
        '2025-2026',
        'Period 3'
    );
