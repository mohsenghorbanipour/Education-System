package com.edu.com.enrollment.dto.request;

import com.edu.com.enrollment.domain.EnrollmentPlanType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateEnrollmentRequest(
        @NotNull(message = "Student user ID is required")
        UUID userId,

        @NotNull(message = "Semester ID is required")
        UUID semesterId,

        @NotNull(message = "Enrollment plan type is required")
        EnrollmentPlanType planType
) {
}
