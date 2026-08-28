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

INSERT INTO feedback (course_offering_id, rating, comment, submitted_at)
VALUES
    (
        (
            SELECT co.id
            FROM course_offering co
            JOIN course c ON c.id = co.course_id
            WHERE c.code = 'CS101'
              AND co.academic_year = '2025-2026'
              AND co.period = 'Period 1'
        ),
        5,
        'The exercises were practical and helped me understand the concepts well.',
        CURRENT_TIMESTAMP
    ),
    (
        (
            SELECT co.id
            FROM course_offering co
            JOIN course c ON c.id = co.course_id
            WHERE c.code = 'CS101'
              AND co.academic_year = '2025-2026'
              AND co.period = 'Period 1'
        ),
        4,
        'Good course overall, but some of the assignment instructions could be clearer.',
        CURRENT_TIMESTAMP
    ),
    (
        (
            SELECT co.id
            FROM course_offering co
            JOIN course c ON c.id = co.course_id
            WHERE c.code = 'CS204'
              AND co.academic_year = '2025-2026'
              AND co.period = 'Period 2'
        ),
        4,
        'The database examples were useful, especially the SQL exercises.',
        CURRENT_TIMESTAMP
    ),
    (
        (
            SELECT co.id
            FROM course_offering co
            JOIN course c ON c.id = co.course_id
            WHERE c.code = 'CS204'
              AND co.academic_year = '2025-2026'
              AND co.period = 'Period 2'
        ),
        3,
        'The course covered useful topics, but the pace was quite fast.',
        CURRENT_TIMESTAMP
    ),
    (
        (
            SELECT co.id
            FROM course_offering co
            JOIN course c ON c.id = co.course_id
            WHERE c.code = 'CS305'
              AND co.academic_year = '2025-2026'
              AND co.period = 'Period 3'
        ),
        5,
        'The group project was challenging but very useful for learning software development practices.',
        CURRENT_TIMESTAMP
    );