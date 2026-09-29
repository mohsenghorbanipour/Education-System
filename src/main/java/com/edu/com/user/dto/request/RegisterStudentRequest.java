package com.edu.com.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record RegisterStudentRequest(
        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        @NotBlank(message = "University number is required")
        @Pattern(regexp = "[0-9]{10}", message = "University number must contain exactly 10 digits")
        String universityNumber,

        @NotBlank(message = "National code is required")
        @Pattern(regexp = "[0-9]{10}", message = "National code must contain exactly 10 digits")
        String nationalCode,

        @NotBlank(message = "Mobile number is required")
        @Pattern(regexp = "[0-9]{11}", message = "Mobile number must contain exactly 11 digits")
        String mobileNumber,

        @NotBlank(message = "Password is required")
        String password,

        @NotNull(message = "Major is required")
        UUID majorId
) {
}
