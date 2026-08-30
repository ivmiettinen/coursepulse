CREATE TABLE course (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE course_offering (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL,
    academic_year VARCHAR(20) NOT NULL,
    period VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_course_offering_course
        FOREIGN KEY (course_id)
        REFERENCES course(id)
);

CREATE TABLE feedback (
    id BIGSERIAL PRIMARY KEY,
    course_offering_id BIGINT NOT NULL,
    rating INTEGER NOT NULL,
    comment TEXT,
    submitted_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_feedback_course_offering
        FOREIGN KEY (course_offering_id)
        REFERENCES course_offering(id),

    CONSTRAINT chk_feedback_rating
        CHECK (rating BETWEEN 1 AND 5),

    CONSTRAINT chk_feedback_comment_length
        CHECK (char_length(comment) <= 2000)
);