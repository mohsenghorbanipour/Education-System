package com.edu.com.enrollment.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddEnrollmentItemRequest(
        @NotNull(message = "Course offering id is required")
        UUID courseOfferingId
) {
}
