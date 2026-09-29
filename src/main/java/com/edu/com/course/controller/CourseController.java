package com.edu.com.course.controller;

import com.edu.com.common.annotations.CheckPermission;
import com.edu.com.common.filter.JwtFilter;
import com.edu.com.course.service.CatalogAccess;
import com.edu.com.user.dto.response.UserDto;
import org.springframework.web.bind.annotation.RequestAttribute;
import com.edu.com.common.response.ApiResponse;
import com.edu.com.course.dto.CourseDto;
import com.edu.com.course.dto.request.CreateCourseRequest;
import com.edu.com.course.dto.request.UpdateCourseRequest;
import com.edu.com.course.service.CourseService;
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
@RequestMapping("/api/v1/courses")
public class CourseController {

    private final CourseService service;

    @PostMapping
    @CheckPermission("COURSE_CREATE")
    public ResponseEntity<ApiResponse<CourseDto>> createCourse(
            @Valid @RequestBody CreateCourseRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<CourseDto>builder()
                        .success(true)
                        .data(service.createCourse(request))
                        .build()
        );
    }

    @GetMapping
    @CheckPermission("COURSE_READ")
    public ResponseEntity<ApiResponse<List<CourseDto>>> getCourses(
            @RequestAttribute(value = JwtFilter.CURRENT_USER, required = false) UserDto currentUser,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) Integer page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) Integer size
    ) {
        CatalogAccess.requireUnrestrictedAccess(currentUser);
        return ResponseEntity.ok(
                ApiResponse.<List<CourseDto>>builder()
                        .success(true)
                        .data(service.getCourses(page, size))
                        .build()
        );
    }

    @GetMapping("/{courseId}")
    @CheckPermission("COURSE_READ")
    public ResponseEntity<ApiResponse<CourseDto>> getCourse(
            @RequestAttribute(value = JwtFilter.CURRENT_USER, required = false) UserDto currentUser,
            @PathVariable("courseId") UUID courseId
    ) {
        CatalogAccess.requireUnrestrictedAccess(currentUser);
        return ResponseEntity.ok(
                ApiResponse.<CourseDto>builder()
                        .success(true)
                        .data(service.getCourse(courseId))
                        .build()
        );
    }

    @PatchMapping("/{courseId}")
    @CheckPermission("COURSE_UPDATE")
    public ResponseEntity<ApiResponse<CourseDto>> updateCourse(
            @PathVariable("courseId") UUID courseId,
            @Valid @RequestBody UpdateCourseRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.<CourseDto>builder()
                        .success(true)
                        .data(service.updateCourse(courseId, request))
                        .build()
        );
    }

    @DeleteMapping("/{courseId}")
    @CheckPermission("COURSE_DELETE")
    public ResponseEntity<ApiResponse<Void>> deleteCourse(
            @PathVariable("courseId") UUID courseId
    ) {
        service.deleteCourse(courseId);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Course deleted successfully")
                        .build()
        );
    }
}
