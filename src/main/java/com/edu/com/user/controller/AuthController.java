package com.edu.com.user.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.edu.com.common.response.ApiResponse;
import com.edu.com.user.dto.request.LoginRequest;
import com.edu.com.user.dto.response.UserDto;
import com.edu.com.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService service;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserDto>> login(
            @Valid @RequestBody LoginRequest request
    ) {

        return ResponseEntity.ok(
                ApiResponse.<UserDto>builder()
                        .success(true)
                        .data(service.getUser(request))
                        .build()
        );
    }
}
