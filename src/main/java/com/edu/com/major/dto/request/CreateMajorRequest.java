package com.edu.com.major.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMajorRequest(
        @NotBlank(message = "Major name is required")
        @Size(max = 150, message = "Major name must not exceed 150 characters")
        String name
) {
}
