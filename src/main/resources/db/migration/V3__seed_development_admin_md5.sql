-- ============================================================
-- V3__seed_development_admin.sql
-- Development / Test bootstrap admin
--
-- Login:
--   university_number: 9999999999
--   password: Admin@123456
--
-- WARNING:
-- Password is stored as MD5 because explicitly requested.
-- MD5 is NOT suitable for password storage in real systems.
-- ============================================================

INSERT INTO users (
    id,
    first_name,
    last_name,
    university_number,
    national_code,
    mobile_number,
    password_hash,
    user_type,
    enabled,
    major_id,
    created_at,
    updated_at,
    version
)
VALUES (
    gen_random_uuid(),
    'System',
    'Administrator',
    '9999999999',
    '0000000001',
    '09000000000',
    '0f2797f2182804d0cc7f0b85d254c146',
    'EMPLOYEE',
    TRUE,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
)
ON CONFLICT (university_number) DO NOTHING;


INSERT INTO user_roles (
    user_id,
    role_id
)
SELECT
    u.id,
    r.id
FROM users u
JOIN roles r
    ON r.name = 'ADMIN'
WHERE u.university_number = '9999999999'
ON CONFLICT DO NOTHING;
