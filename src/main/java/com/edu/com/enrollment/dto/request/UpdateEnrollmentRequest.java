package com.edu.com.enrollment.dto.request;

import com.edu.com.enrollment.domain.EnrollmentPlanType;
import jakarta.validation.constraints.NotNull;

public record UpdateEnrollmentRequest(
        @NotNull(message = "Enrollment plan type is required")
        EnrollmentPlanType planType
) {
}
