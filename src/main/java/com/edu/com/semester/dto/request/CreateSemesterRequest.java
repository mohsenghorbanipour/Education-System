package com.edu.com.semester.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateSemesterRequest(
        @NotBlank(message = "Semester title is required")
        @Size(max = 10, message = "Semester title must not exceed 10 characters")
        String title,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        LocalDate endDate
) {
}
