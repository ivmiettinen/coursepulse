DELETE FROM course_feedback;
DELETE FROM course_offering;
DELETE FROM course;

INSERT INTO course (id, code, name)
VALUES
    (910001, 'ZZ101', 'Course with offerings'),
    (910002, 'AA101', 'Another course'),
    (910003, 'MM101', 'Course without offerings');

INSERT INTO course_offering (id, course_id, academic_year, period)
VALUES
    (910001, 910001, '2025-2026', 'Period 1'),
    (910002, 910001, '2026-2027', 'Period 2'),
    (910004, 910001, '2026-2027', 'Period 1'),
    (910003, 910001, '2026-2027', 'Period 1'),
    (910005, 910001, '2026-2027', 'Period 10'),
    (910006, 910002, '2027-2028', 'Period 1');
