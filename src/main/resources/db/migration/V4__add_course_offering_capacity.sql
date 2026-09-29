ALTER TABLE course_offerings ADD COLUMN capacity INTEGER;

UPDATE course_offerings offering
SET capacity = GREATEST(50, (
    SELECT COUNT(*)
    FROM enrollment_items item
    WHERE item.course_offering_id = offering.id
));

ALTER TABLE course_offerings
    ALTER COLUMN capacity SET NOT NULL,
    ADD CONSTRAINT chk_course_offerings_capacity CHECK (capacity > 0);
