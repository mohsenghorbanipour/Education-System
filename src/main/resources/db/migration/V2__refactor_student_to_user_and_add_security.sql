-- ============================================================
-- V2__refactor_student_to_user_and_add_security.sql
-- Education System
-- ============================================================

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM students LIMIT 1) THEN
        RAISE EXCEPTION
            'V2 expects the students table to be empty. Backfill user security fields before applying this migration.';
    END IF;
END
$$;

-- ============================================================
-- 1. REFACTOR STUDENTS -> USERS
-- ============================================================

ALTER TABLE students RENAME TO users;
ALTER TABLE users RENAME COLUMN student_number TO university_number;
ALTER TABLE users ALTER COLUMN university_number TYPE VARCHAR(30);
ALTER TABLE users ALTER COLUMN major_id DROP NOT NULL;

ALTER TABLE users RENAME CONSTRAINT pk_students TO pk_users;
ALTER TABLE users RENAME CONSTRAINT uk_students_student_number TO uk_users_university_number;
ALTER TABLE users RENAME CONSTRAINT fk_students_major TO fk_users_major;

ALTER INDEX IF EXISTS idx_students_major_id RENAME TO idx_users_major_id;

ALTER TABLE users
    ADD COLUMN national_code VARCHAR(10) NOT NULL,
    ADD COLUMN mobile_number VARCHAR(11) NOT NULL,
    ADD COLUMN password_hash VARCHAR(500) NOT NULL,
    ADD COLUMN user_type VARCHAR(30) NOT NULL,
    ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE users
    ADD CONSTRAINT uk_users_national_code UNIQUE (national_code),
    ADD CONSTRAINT uk_users_mobile_number UNIQUE (mobile_number),
    ADD CONSTRAINT chk_users_national_code CHECK (national_code ~ '^[0-9]{10}$'),
    ADD CONSTRAINT chk_users_mobile_number CHECK (mobile_number ~ '^[0-9]{11}$'),
    ADD CONSTRAINT chk_users_user_type CHECK (
        user_type IN ('STUDENT', 'EMPLOYEE', 'PROFESSOR')
    ),
    ADD CONSTRAINT chk_users_student_requires_major CHECK (
        user_type <> 'STUDENT' OR major_id IS NOT NULL
    );

CREATE INDEX idx_users_user_type ON users (user_type);

-- ============================================================
-- 2. REFACTOR ENROLLMENTS
-- ============================================================

ALTER TABLE enrollments RENAME COLUMN student_id TO user_id;
ALTER TABLE enrollments
    RENAME CONSTRAINT uk_enrollment_student_semester
    TO uk_enrollment_user_semester;

ALTER TABLE enrollments
    RENAME CONSTRAINT fk_enrollments_student
    TO fk_enrollments_user;

-- ============================================================
-- 3. ROLES
-- ============================================================

CREATE TABLE roles
(
    id          UUID         NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    version     BIGINT       NOT NULL,

    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT uk_roles_name UNIQUE (name)
);

-- ============================================================
-- 4. PERMISSIONS
-- ============================================================

CREATE TABLE permissions
(
    id          UUID         NOT NULL,
    name        VARCHAR(120) NOT NULL,
    description TEXT,
    parent_id   UUID,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    version     BIGINT       NOT NULL,

    CONSTRAINT pk_permissions PRIMARY KEY (id),
    CONSTRAINT uk_permissions_name UNIQUE (name),

    CONSTRAINT fk_permissions_parent
        FOREIGN KEY (parent_id)
        REFERENCES permissions (id)
        ON DELETE RESTRICT
);

CREATE INDEX idx_permissions_parent_id ON permissions (parent_id);

-- ============================================================
-- 5. USER <-> ROLE
-- ============================================================

CREATE TABLE user_roles
(
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,

    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),

    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id)
        REFERENCES roles (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_user_roles_role_id ON user_roles (role_id);

-- ============================================================
-- 6. ROLE <-> PERMISSION
-- ============================================================

CREATE TABLE role_permissions
(
    role_id       UUID NOT NULL,
    permission_id UUID NOT NULL,

    CONSTRAINT pk_role_permissions PRIMARY KEY (role_id, permission_id),

    CONSTRAINT fk_role_permissions_role
        FOREIGN KEY (role_id)
        REFERENCES roles (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_role_permissions_permission
        FOREIGN KEY (permission_id)
        REFERENCES permissions (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_role_permissions_permission_id
    ON role_permissions (permission_id);

-- ============================================================
-- 7. SEED ROLES
-- ============================================================

INSERT INTO roles (id, name, description, created_at, updated_at, version)
VALUES
    (gen_random_uuid(), 'ADMIN', 'Full system administrator', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (gen_random_uuid(), 'EMPLOYEE', 'University employee responsible for academic administration', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (gen_random_uuid(), 'PROFESSOR', 'University professor', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (gen_random_uuid(), 'STUDENT', 'University student', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- ============================================================
-- 8. SEED PERMISSIONS
-- ============================================================

INSERT INTO permissions (
    id, name, description, parent_id, created_at, updated_at, version
)
SELECT
    gen_random_uuid(),
    p.name,
    p.description,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
FROM (
    VALUES
    ('USER_CREATE', 'Create any type of university user'),
    ('USER_READ', 'Read any type of university user'),
    ('USER_UPDATE', 'Update any type of university user'),
    ('USER_DELETE', 'Delete any type of university user'),

    ('STUDENT_USER_CREATE', 'Create users whose user type is STUDENT'),
    ('STUDENT_USER_READ', 'Read users whose user type is STUDENT'),
    ('STUDENT_USER_UPDATE', 'Update users whose user type is STUDENT'),
    ('STUDENT_USER_DELETE', 'Delete users whose user type is STUDENT'),

    ('MAJOR_CREATE', 'Create majors'),
    ('MAJOR_READ', 'Read majors'),
    ('MAJOR_UPDATE', 'Update majors'),
    ('MAJOR_DELETE', 'Delete majors'),

    ('COURSE_CREATE', 'Create courses'),
    ('COURSE_READ', 'Read courses'),
    ('COURSE_UPDATE', 'Update courses'),
    ('COURSE_DELETE', 'Delete courses'),

    ('COURSE_MAJOR_CREATE', 'Assign a course to a major'),
    ('COURSE_MAJOR_READ', 'Read course-major assignments'),
    ('COURSE_MAJOR_UPDATE', 'Update course-major assignments'),
    ('COURSE_MAJOR_DELETE', 'Remove a course-major assignment'),

    ('SEMESTER_CREATE', 'Create semesters'),
    ('SEMESTER_READ', 'Read semesters'),
    ('SEMESTER_UPDATE', 'Update semesters'),
    ('SEMESTER_DELETE', 'Delete semesters'),

    ('COURSE_OFFERING_CREATE', 'Create course offerings'),
    ('COURSE_OFFERING_READ', 'Read course offerings'),
    ('COURSE_OFFERING_UPDATE', 'Update course offerings'),
    ('COURSE_OFFERING_DELETE', 'Delete course offerings'),

    ('ENROLLMENT_CREATE', 'Create any student enrollment'),
    ('ENROLLMENT_READ', 'Read any student enrollment'),
    ('ENROLLMENT_UPDATE', 'Update any student enrollment'),
    ('ENROLLMENT_DELETE', 'Delete any student enrollment'),

    ('ENROLLMENT_ITEM_CREATE', 'Add an item to any enrollment'),
    ('ENROLLMENT_ITEM_READ', 'Read enrollment items'),
    ('ENROLLMENT_ITEM_UPDATE', 'Update enrollment items'),
    ('ENROLLMENT_ITEM_DELETE', 'Remove an item from any enrollment'),

    ('ROLE_CREATE', 'Create roles'),
    ('ROLE_READ', 'Read roles'),
    ('ROLE_UPDATE', 'Update roles'),
    ('ROLE_DELETE', 'Delete roles'),

    ('PERMISSION_CREATE', 'Create permissions'),
    ('PERMISSION_READ', 'Read permissions'),
    ('PERMISSION_UPDATE', 'Update permissions'),
    ('PERMISSION_DELETE', 'Delete permissions'),

    ('PROFILE_READ_SELF', 'Read the authenticated user profile'),
    ('PROFILE_UPDATE_SELF', 'Update allowed fields of the authenticated user profile'),
    ('PASSWORD_CHANGE_SELF', 'Change the authenticated user password'),

    ('ENROLLMENT_READ_SELF', 'Read the authenticated student enrollment'),
    ('ENROLLMENT_CREATE_SELF', 'Create an enrollment for the authenticated student'),
    ('ENROLLMENT_ADD_COURSE_SELF', 'Add a course to the authenticated student enrollment'),
    ('ENROLLMENT_REMOVE_COURSE_SELF', 'Remove a course from the authenticated student enrollment')
) AS p(name, description);

-- ============================================================
-- 9. ADMIN -> ALL PERMISSIONS
-- ============================================================

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN';

-- ============================================================
-- 10. EMPLOYEE PERMISSIONS
-- ============================================================

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name IN (
    'STUDENT_USER_CREATE',
    'STUDENT_USER_READ',
    'STUDENT_USER_UPDATE',
    'STUDENT_USER_DELETE',

    'MAJOR_CREATE',
    'MAJOR_READ',
    'MAJOR_UPDATE',
    'MAJOR_DELETE',

    'COURSE_CREATE',
    'COURSE_READ',
    'COURSE_UPDATE',
    'COURSE_DELETE',

    'COURSE_MAJOR_CREATE',
    'COURSE_MAJOR_READ',
    'COURSE_MAJOR_UPDATE',
    'COURSE_MAJOR_DELETE',

    'SEMESTER_CREATE',
    'SEMESTER_READ',
    'SEMESTER_UPDATE',
    'SEMESTER_DELETE',

    'COURSE_OFFERING_CREATE',
    'COURSE_OFFERING_READ',
    'COURSE_OFFERING_UPDATE',
    'COURSE_OFFERING_DELETE',

    'ENROLLMENT_CREATE',
    'ENROLLMENT_READ',
    'ENROLLMENT_UPDATE',
    'ENROLLMENT_DELETE',

    'ENROLLMENT_ITEM_CREATE',
    'ENROLLMENT_ITEM_READ',
    'ENROLLMENT_ITEM_UPDATE',
    'ENROLLMENT_ITEM_DELETE',

    'PROFILE_READ_SELF',
    'PROFILE_UPDATE_SELF',
    'PASSWORD_CHANGE_SELF'
)
WHERE r.name = 'EMPLOYEE';

-- ============================================================
-- 11. PROFESSOR PERMISSIONS
-- ============================================================

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name IN (
    'PROFILE_READ_SELF',
    'PROFILE_UPDATE_SELF',
    'PASSWORD_CHANGE_SELF',
    'MAJOR_READ',
    'COURSE_READ',
    'SEMESTER_READ',
    'COURSE_OFFERING_READ'
)
WHERE r.name = 'PROFESSOR';

-- ============================================================
-- 12. STUDENT PERMISSIONS
-- ============================================================

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name IN (
    'PROFILE_READ_SELF',
    'PROFILE_UPDATE_SELF',
    'PASSWORD_CHANGE_SELF',
    'MAJOR_READ',
    'COURSE_READ',
    'SEMESTER_READ',
    'COURSE_OFFERING_READ',
    'ENROLLMENT_READ_SELF',
    'ENROLLMENT_CREATE_SELF',
    'ENROLLMENT_ADD_COURSE_SELF',
    'ENROLLMENT_REMOVE_COURSE_SELF'
)
WHERE r.name = 'STUDENT';
