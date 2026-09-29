-- ============================================================
-- V1__create_initial_schema.sql
-- University Enrollment System
-- ============================================================


-- ============================================================
-- 1. MAJORS
-- ============================================================

CREATE TABLE majors
(
    id          UUID         NOT NULL,
    name        VARCHAR(150) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    version     BIGINT       NOT NULL,

    CONSTRAINT pk_majors
        PRIMARY KEY (id),

    CONSTRAINT uk_majors_name
        UNIQUE (name)
);


-- ============================================================
-- 2. STUDENTS
-- ============================================================

CREATE TABLE students
(
    id              UUID         NOT NULL,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    student_number  VARCHAR(10)  NOT NULL,
    major_id        UUID         NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    version         BIGINT       NOT NULL,

    CONSTRAINT pk_students
        PRIMARY KEY (id),

    CONSTRAINT uk_students_student_number
        UNIQUE (student_number),

    CONSTRAINT fk_students_major
        FOREIGN KEY (major_id)
            REFERENCES majors (id)
            ON DELETE RESTRICT
);

CREATE INDEX idx_students_major_id
    ON students (major_id);


-- ============================================================
-- 3. COURSES
-- ============================================================

CREATE TABLE courses
(
    id            UUID         NOT NULL,
    name          VARCHAR(150) NOT NULL,
    code          VARCHAR(30)  NOT NULL,
    credit_units  INTEGER      NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    version       BIGINT       NOT NULL,

    CONSTRAINT pk_courses
        PRIMARY KEY (id),

    CONSTRAINT uk_courses_code
        UNIQUE (code),

    CONSTRAINT chk_courses_credit_units
        CHECK (credit_units > 0)
);


-- ============================================================
-- 4. COURSE <-> MAJOR
-- ============================================================

CREATE TABLE course_majors
(
    id          UUID        NOT NULL,
    course_id   UUID        NOT NULL,
    major_id    UUID        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,
    version     BIGINT      NOT NULL,

    CONSTRAINT pk_course_majors
        PRIMARY KEY (id),

    CONSTRAINT uk_course_major
        UNIQUE (course_id, major_id),

    CONSTRAINT fk_course_majors_course
        FOREIGN KEY (course_id)
            REFERENCES courses (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_course_majors_major
        FOREIGN KEY (major_id)
            REFERENCES majors (id)
            ON DELETE RESTRICT
);

CREATE INDEX idx_course_majors_major_id
    ON course_majors (major_id);


-- ============================================================
-- 5. SEMESTERS
-- ============================================================

CREATE TABLE semesters
(
    id          UUID        NOT NULL,
    title       VARCHAR(20) NOT NULL,
    start_date  DATE        NOT NULL,
    end_date    DATE        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,
    version     BIGINT      NOT NULL,

    CONSTRAINT pk_semesters
        PRIMARY KEY (id),

    CONSTRAINT uk_semesters_title
        UNIQUE (title),

    CONSTRAINT chk_semesters_dates
        CHECK (end_date > start_date)
);


-- ============================================================
-- 6. COURSE OFFERINGS
-- ============================================================

CREATE TABLE course_offerings
(
    id             UUID        NOT NULL,
    course_id      UUID        NOT NULL,
    semester_id    UUID        NOT NULL,
    guest_allowed  BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL,
    version        BIGINT      NOT NULL,

    CONSTRAINT pk_course_offerings
        PRIMARY KEY (id),

    CONSTRAINT uk_course_offering_semester_course
        UNIQUE (semester_id, course_id),

    CONSTRAINT fk_course_offerings_course
        FOREIGN KEY (course_id)
            REFERENCES courses (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_course_offerings_semester
        FOREIGN KEY (semester_id)
            REFERENCES semesters (id)
            ON DELETE RESTRICT
);

CREATE INDEX idx_course_offerings_course_id
    ON course_offerings (course_id);


-- ============================================================
-- 7. ENROLLMENTS
-- ============================================================

CREATE TABLE enrollments
(
    id           UUID        NOT NULL,
    student_id   UUID        NOT NULL,
    semester_id  UUID        NOT NULL,
    plan_type    VARCHAR(20) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL,
    version      BIGINT      NOT NULL,

    CONSTRAINT pk_enrollments
        PRIMARY KEY (id),

    CONSTRAINT uk_enrollment_student_semester
        UNIQUE (student_id, semester_id),

    CONSTRAINT chk_enrollment_plan_type
        CHECK (
            plan_type IN (
                          'REGULAR',
                          'PROBATION',
                          'HONORS',
                          'GUEST'
                )
            ),

    CONSTRAINT fk_enrollments_student
        FOREIGN KEY (student_id)
            REFERENCES students (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_enrollments_semester
        FOREIGN KEY (semester_id)
            REFERENCES semesters (id)
            ON DELETE RESTRICT
);

CREATE INDEX idx_enrollments_semester_id
    ON enrollments (semester_id);


-- ============================================================
-- 8. ENROLLMENT ITEMS
-- ============================================================

CREATE TABLE enrollment_items
(
    id                  UUID        NOT NULL,
    enrollment_id       UUID        NOT NULL,
    course_offering_id  UUID        NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL,
    version             BIGINT      NOT NULL,

    CONSTRAINT pk_enrollment_items
        PRIMARY KEY (id),

    CONSTRAINT uk_enrollment_item_offering
        UNIQUE (
                enrollment_id,
                course_offering_id
            ),

    CONSTRAINT fk_enrollment_items_enrollment
        FOREIGN KEY (enrollment_id)
            REFERENCES enrollments (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_enrollment_items_course_offering
        FOREIGN KEY (course_offering_id)
            REFERENCES course_offerings (id)
            ON DELETE RESTRICT
);

CREATE INDEX idx_enrollment_items_course_offering_id
    ON enrollment_items (course_offering_id);