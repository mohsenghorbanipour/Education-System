package com.edu.com.enrollment.controller;

import com.edu.com.common.annotations.CheckPermission;
import com.edu.com.common.filter.JwtFilter;
import com.edu.com.common.response.ApiResponse;
import com.edu.com.enrollment.dto.EnrollmentDto;
import com.edu.com.course.dto.CourseOfferingDto;
import com.edu.com.enrollment.dto.EnrollmentDetailDto;
import com.edu.com.enrollment.dto.EnrollmentItemDto;
import com.edu.com.enrollment.dto.request.AddEnrollmentItemRequest;
import com.edu.com.enrollment.dto.request.CreateEnrollmentRequest;
import com.edu.com.enrollment.dto.request.UpdateEnrollmentRequest;
import com.edu.com.enrollment.service.EnrollmentService;
import com.edu.com.user.dto.response.UserDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/enrollments")
public class EnrollmentController {

    private final EnrollmentService service;

    @PostMapping
    @CheckPermission("ENROLLMENT_CREATE")
    public ResponseEntity<ApiResponse<EnrollmentDto>> createEnrollment(
            @Valid @RequestBody CreateEnrollmentRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<EnrollmentDto>builder()
                        .success(true)
                        .data(service.createEnrollment(request))
                        .build()
        );
    }

    @GetMapping
    @CheckPermission("ENROLLMENT_READ")
    public ResponseEntity<ApiResponse<List<EnrollmentDto>>> getEnrollments(
            @RequestParam(name = "userId", required = false) UUID userId,
            @RequestParam(name = "semesterId", required = false) UUID semesterId,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) Integer page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) Integer size
    ) {
        return ResponseEntity.ok(
                ApiResponse.<List<EnrollmentDto>>builder()
                        .success(true)
                        .data(service.getEnrollments(userId, semesterId, page, size))
                        .build()
        );
    }

    @GetMapping("/{enrollmentId}")
    @CheckPermission("ENROLLMENT_READ")
    public ResponseEntity<ApiResponse<EnrollmentDetailDto>> getEnrollment(
            @PathVariable("enrollmentId") UUID enrollmentId
    ) {
        return ResponseEntity.ok(
                ApiResponse.<EnrollmentDetailDto>builder()
                        .success(true)
                        .data(service.getEnrollment(enrollmentId))
                        .build()
        );
    }

    @PatchMapping("/{enrollmentId}")
    @CheckPermission("ENROLLMENT_UPDATE")
    public ResponseEntity<ApiResponse<EnrollmentDto>> updateEnrollment(
            @PathVariable("enrollmentId") UUID enrollmentId,
            @Valid @RequestBody UpdateEnrollmentRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.<EnrollmentDto>builder()
                        .success(true)
                        .data(service.updateEnrollment(enrollmentId, request))
                        .build()
        );
    }

    @DeleteMapping("/{enrollmentId}")
    @CheckPermission("ENROLLMENT_DELETE")
    public ResponseEntity<ApiResponse<Void>> deleteEnrollment(
            @PathVariable("enrollmentId") UUID enrollmentId
    ) {
        service.deleteEnrollment(enrollmentId);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Enrollment deleted successfully")
                        .build()
        );
    }

    @GetMapping("/me")
    @CheckPermission("ENROLLMENT_READ_SELF")
    public ResponseEntity<ApiResponse<List<EnrollmentDto>>> getOwnEnrollments(
            @RequestAttribute(JwtFilter.CURRENT_USER) UserDto currentUser,
            @RequestParam(name = "semesterId", required = false) UUID semesterId,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) Integer page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) Integer size
    ) {
        return ResponseEntity.ok(
                ApiResponse.<List<EnrollmentDto>>builder()
                        .success(true)
                        .data(service.getOwnEnrollments(currentUser.getId(), semesterId, page, size))
                        .build()
        );
    }

    @GetMapping("/me/{enrollmentId}")
    @CheckPermission("ENROLLMENT_READ_SELF")
    public ResponseEntity<ApiResponse<EnrollmentDetailDto>> getOwnEnrollment(
            @RequestAttribute(JwtFilter.CURRENT_USER) UserDto currentUser,
            @PathVariable("enrollmentId") UUID enrollmentId
    ) {
        return ResponseEntity.ok(
                ApiResponse.<EnrollmentDetailDto>builder()
                        .success(true)
                        .data(service.getOwnEnrollment(currentUser.getId(), enrollmentId))
                        .build()
        );
    }

    @PostMapping("/me/{enrollmentId}/items")
    @CheckPermission("ENROLLMENT_ADD_COURSE_SELF")
    public ResponseEntity<ApiResponse<EnrollmentItemDto>> addOwnEnrollmentItem(
            @RequestAttribute(JwtFilter.CURRENT_USER) UserDto currentUser,
            @PathVariable("enrollmentId") UUID enrollmentId,
            @Valid @RequestBody AddEnrollmentItemRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<EnrollmentItemDto>builder()
                        .success(true)
                        .data(service.addOwnEnrollmentItem(currentUser.getId(), enrollmentId, request))
                        .build()
        );
    }

    @GetMapping("/me/{enrollmentId}/available-course-offerings")
    @CheckPermission("ENROLLMENT_READ_SELF")
    public ResponseEntity<ApiResponse<List<CourseOfferingDto>>> getAvailableCourseOfferings(
            @RequestAttribute(JwtFilter.CURRENT_USER) UserDto currentUser,
            @PathVariable("enrollmentId") UUID enrollmentId,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) Integer page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) Integer size
    ) {
        return ResponseEntity.ok(ApiResponse.<List<CourseOfferingDto>>builder()
                .success(true)
                .data(service.getAvailableCourseOfferings(currentUser.getId(), enrollmentId, page, size))
                .build());
    }

    @DeleteMapping("/me/{enrollmentId}/items/{itemId}")
    @CheckPermission("ENROLLMENT_REMOVE_COURSE_SELF")
    public ResponseEntity<ApiResponse<Void>> removeOwnEnrollmentItem(
            @RequestAttribute(JwtFilter.CURRENT_USER) UserDto currentUser,
            @PathVariable("enrollmentId") UUID enrollmentId,
            @PathVariable("itemId") UUID itemId
    ) {
        service.removeOwnEnrollmentItem(currentUser.getId(), enrollmentId, itemId);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Selected course removed successfully")
                        .build()
        );
    }
}
