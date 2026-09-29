package com.edu.com.user.dto.request;

public record UpdateProfileRequest(
        String firstName,
        String lastName,
        String nationalCode,
        String mobileNumber
) { }
