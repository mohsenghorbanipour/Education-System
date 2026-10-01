package com.edu.com.user.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.edu.com.common.filter.JwtFilter;
import com.edu.com.common.response.ApiResponse;
import com.edu.com.common.annotations.CheckPermission;
import com.edu.com.user.dto.request.RegisterStudentRequest;
import com.edu.com.user.dto.request.UpdateProfileRequest;
import com.edu.com.user.dto.request.UpdateUserRequest;
import com.edu.com.user.dto.response.UserDto;
import com.edu.com.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@Tag(name = "Users")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService service;

    @PostMapping("/register-student")
    @CheckPermission("STUDENT_USER_CREATE")
    public ResponseEntity<ApiResponse<UserDto>> registerStudent(
            @Valid @RequestBody RegisterStudentRequest request
    ) {

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<UserDto>builder()
                        .success(true)
                        .data(service.registerStudent(request))
                        .build()
        );
    }

    @GetMapping("")
    @CheckPermission("USER_READ")
    public ResponseEntity<ApiResponse<List<UserDto>>> getUsers(
            @RequestParam(name = "page", defaultValue = "0") @Min(0) Integer page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) Integer size
    ) {
        return ResponseEntity.ok(
                ApiResponse.<List<UserDto>>builder()
                        .success(true)
                        .data(service.getUsers(page, size))
                        .build()
        );
    }

    @PatchMapping("/{userId}/profile")
    @CheckPermission("USER_UPDATE")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(
            @PathVariable("userId") UUID userId,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.<UserDto>builder()
                        .success(true)
                        .data(service.updateUser(userId, request))
                        .build()
        );
    }

    @GetMapping("/{userId}")
    @CheckPermission("USER_READ")
    public ResponseEntity<ApiResponse<UserDto>> getUserDetails(@PathVariable("userId") UUID userId) {
        return ResponseEntity.ok(ApiResponse.<UserDto>builder()
                .success(true)
                .data(service.getUserDetails(userId))
                .build());
    }

    @DeleteMapping("/{userId}")
    @CheckPermission("USER_DELETE")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable("userId") UUID userId
    ) {
        service.deleteUser(userId);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("User deleted successfully")
                        .build()
        );
    }

    @PatchMapping("/me/profile")
    @CheckPermission("PROFILE_UPDATE_SELF")
    public ResponseEntity<ApiResponse<UserDto>> updateOwnUser(
            @Parameter(hidden = true) @RequestAttribute(JwtFilter.CURRENT_USER) UserDto currentUser,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.<UserDto>builder()
                        .success(true)
                        .data(service.updateOwnUser(currentUser.getId(), request))
                        .build()
        );
    }


}
