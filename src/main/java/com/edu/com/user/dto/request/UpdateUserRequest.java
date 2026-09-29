package com.edu.com.user.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateUserRequest(
        @Size(max = 100, message = "First name must not exceed 100 characters")
        @Pattern(regexp = "(?s).*\\S.*", message = "First name must not be blank")
        String firstName,

        @Size(max = 100, message = "Last name must not exceed 100 characters")
        @Pattern(regexp = "(?s).*\\S.*", message = "Last name must not be blank")
        String lastName,

        @Pattern(regexp = "[0-9]{10}", message = "National code must contain exactly 10 digits")
        String nationalCode,

        @Pattern(regexp = "[0-9]{11}", message = "Mobile number must contain exactly 11 digits")
        String mobileNumber,

        @Pattern(regexp = "[0-9]{10}", message = "University number must contain exactly 10 digits")
        String universityNumber,

        UUID majorId
) {
}
