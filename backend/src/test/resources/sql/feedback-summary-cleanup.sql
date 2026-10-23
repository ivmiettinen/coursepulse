-- Remove only the data created by feedback-summary-data.sql, children first.
DELETE FROM course_feedback WHERE id IN (
    910001, 910002, 910003, 910004, 910005, 910006, 910007
);
DELETE FROM course_offering WHERE id IN (910001, 910002, 910003);
DELETE FROM course WHERE id = 910001;
