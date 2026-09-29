package com.edu.com.course.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public record UpdateCourseRequest(
        @Size(max = 150, message = "Course name must not exceed 150 characters")
        @Pattern(regexp = "(?sU).*\\S.*", message = "Course name must not be blank")
        String name,

        @Size(max = 20, message = "Course code must not exceed 20 characters")
        @Pattern(regexp = "(?sU).*\\S.*", message = "Course code must not be blank")
        String code,

        @Positive(message = "Credit units must be greater than zero")
        Integer creditUnits,

        @Size(min = 1, message = "At least one major is required")
        Set<@NotNull(message = "Major ID is required") UUID> majorIds
) {
}
