package com.edu.com.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(

        @NotBlank(
                message = "University number is required"
        )
        @Pattern(
                regexp = "\\d{10}",
                message = "University number must contain exactly 10 digits"
        )
        String universityNumber,

        @NotBlank(
                message = "Password is required"
        )
        String password

) {
}
