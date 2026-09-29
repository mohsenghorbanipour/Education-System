package com.edu.com.semester.controller;

import com.edu.com.common.annotations.CheckPermission;
import com.edu.com.common.response.ApiResponse;
import com.edu.com.semester.dto.SemesterDto;
import com.edu.com.semester.dto.request.CreateSemesterRequest;
import com.edu.com.semester.dto.request.UpdateSemesterRequest;
import com.edu.com.semester.service.SemesterService;
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
@RequestMapping("/api/v1/semesters")
public class SemesterController {

    private final SemesterService service;

    @PostMapping
    @CheckPermission("SEMESTER_CREATE")
    public ResponseEntity<ApiResponse<SemesterDto>> createSemester(
            @Valid @RequestBody CreateSemesterRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<SemesterDto>builder()
                        .success(true)
                        .data(service.createSemester(request))
                        .build()
        );
    }

    @GetMapping
    @CheckPermission("SEMESTER_READ")
    public ResponseEntity<ApiResponse<List<SemesterDto>>> getSemesters(
            @RequestParam(name = "page", defaultValue = "0") @Min(0) Integer page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) Integer size
    ) {
        return ResponseEntity.ok(
                ApiResponse.<List<SemesterDto>>builder()
                        .success(true)
                        .data(service.getSemesters(page, size))
                        .build()
        );
    }

    @GetMapping("/{semesterId}")
    @CheckPermission("SEMESTER_READ")
    public ResponseEntity<ApiResponse<SemesterDto>> getSemester(
            @PathVariable("semesterId") UUID semesterId
    ) {
        return ResponseEntity.ok(
                ApiResponse.<SemesterDto>builder()
                        .success(true)
                        .data(service.getSemester(semesterId))
                        .build()
        );
    }

    @PatchMapping("/{semesterId}")
    @CheckPermission("SEMESTER_UPDATE")
    public ResponseEntity<ApiResponse<SemesterDto>> updateSemester(
            @PathVariable("semesterId") UUID semesterId,
            @Valid @RequestBody UpdateSemesterRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.<SemesterDto>builder()
                        .success(true)
                        .data(service.updateSemester(semesterId, request))
                        .build()
        );
    }

    @DeleteMapping("/{semesterId}")
    @CheckPermission("SEMESTER_DELETE")
    public ResponseEntity<ApiResponse<Void>> deleteSemester(
            @PathVariable("semesterId") UUID semesterId
    ) {
        service.deleteSemester(semesterId);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Semester deleted successfully")
                        .build()
        );
    }
}
