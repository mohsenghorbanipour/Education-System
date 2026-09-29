package com.edu.com.semester.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateSemesterRequest(
        @Size(max = 10, message = "Semester title must not exceed 10 characters")
        @Pattern(regexp = "(?sU).*\\S.*", message = "Semester title must not be blank")
        String title,

        LocalDate startDate,

        LocalDate endDate
) {
}
