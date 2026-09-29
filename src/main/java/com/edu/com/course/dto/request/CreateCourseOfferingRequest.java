package com.edu.com.course.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record CreateCourseOfferingRequest(
        @NotNull(message = "Course ID is required")
        UUID courseId,

        @NotNull(message = "Semester ID is required")
        UUID semesterId,

        @NotNull(message = "Guest allowance is required")
        Boolean guestAllowed,

        @NotNull(message = "Capacity is required")
        @Positive(message = "Capacity must be greater than zero")
        Integer capacity
) {
}
