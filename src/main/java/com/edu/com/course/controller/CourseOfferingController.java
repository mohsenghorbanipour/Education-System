package com.edu.com.course.controller;

import com.edu.com.common.annotations.CheckPermission;
import com.edu.com.common.filter.JwtFilter;
import com.edu.com.course.service.CatalogAccess;
import com.edu.com.user.dto.response.UserDto;
import org.springframework.web.bind.annotation.RequestAttribute;
import com.edu.com.common.response.ApiResponse;
import com.edu.com.course.dto.CourseOfferingDto;
import com.edu.com.course.dto.request.CreateCourseOfferingRequest;
import com.edu.com.course.dto.request.UpdateCourseOfferingRequest;
import com.edu.com.course.service.CourseOfferingService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/course-offerings")
public class CourseOfferingController {

    private final CourseOfferingService service;

    @PostMapping
    @CheckPermission("COURSE_OFFERING_CREATE")
    public ResponseEntity<ApiResponse<CourseOfferingDto>> createCourseOffering(
            @Valid @RequestBody CreateCourseOfferingRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<CourseOfferingDto>builder()
                        .success(true)
                        .data(service.createCourseOffering(request))
                        .build()
        );
    }

    @GetMapping
    @CheckPermission("COURSE_OFFERING_READ")
    public ResponseEntity<ApiResponse<List<CourseOfferingDto>>> getCourseOfferings(
            @RequestAttribute(value = JwtFilter.CURRENT_USER, required = false) UserDto currentUser,
            @RequestParam(name = "semesterId", required = false) UUID semesterId,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) Integer page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) Integer size
    ) {
        CatalogAccess.requireUnrestrictedAccess(currentUser);
        return ResponseEntity.ok(
                ApiResponse.<List<CourseOfferingDto>>builder()
                        .success(true)
                        .data(service.getCourseOfferings(semesterId, page, size))
                        .build()
        );
    }

    @GetMapping("/{courseOfferingId}")
    @CheckPermission("COURSE_OFFERING_READ")
    public ResponseEntity<ApiResponse<CourseOfferingDto>> getCourseOffering(
            @RequestAttribute(value = JwtFilter.CURRENT_USER, required = false) UserDto currentUser,
            @PathVariable("courseOfferingId") UUID courseOfferingId
    ) {
        CatalogAccess.requireUnrestrictedAccess(currentUser);
        return ResponseEntity.ok(
                ApiResponse.<CourseOfferingDto>builder()
                        .success(true)
                        .data(service.getCourseOffering(courseOfferingId))
                        .build()
        );
    }

    @PatchMapping("/{courseOfferingId}")
    @CheckPermission("COURSE_OFFERING_UPDATE")
    public ResponseEntity<ApiResponse<CourseOfferingDto>> updateCourseOffering(
            @PathVariable("courseOfferingId") UUID courseOfferingId,
            @Valid @RequestBody UpdateCourseOfferingRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.<CourseOfferingDto>builder()
                        .success(true)
                        .data(service.updateCourseOffering(courseOfferingId, request))
                        .build()
        );
    }

    @DeleteMapping("/{courseOfferingId}")
    @CheckPermission("COURSE_OFFERING_DELETE")
    public ResponseEntity<ApiResponse<Void>> deleteCourseOffering(
            @PathVariable("courseOfferingId") UUID courseOfferingId
    ) {
        service.deleteCourseOffering(courseOfferingId);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Course offering deleted successfully")
                        .build()
        );
    }
}
