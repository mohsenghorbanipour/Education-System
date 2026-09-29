package com.edu.com.course.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public record CreateCourseRequest(
        @NotBlank(message = "Course name is required")
        @Size(max = 150, message = "Course name must not exceed 150 characters")
        String name,

        @NotBlank(message = "Course code is required")
        @Size(max = 20, message = "Course code must not exceed 20 characters")
        String code,

        @NotNull(message = "Credit units are required")
        @Positive(message = "Credit units must be greater than zero")
        Integer creditUnits,

        @NotEmpty(message = "At least one major is required")
        Set<@NotNull(message = "Major ID is required") UUID> majorIds
) {
}
