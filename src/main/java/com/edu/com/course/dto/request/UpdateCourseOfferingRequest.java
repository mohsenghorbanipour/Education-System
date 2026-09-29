package com.edu.com.course.dto.request;

import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record UpdateCourseOfferingRequest(
        UUID courseId,

        UUID semesterId,

        Boolean guestAllowed,

        @Positive(message = "Capacity must be greater than zero")
        Integer capacity
) {
}
