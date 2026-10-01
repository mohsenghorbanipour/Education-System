package com.edu.com.major.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.edu.com.common.annotations.CheckPermission;
import com.edu.com.common.response.ApiResponse;
import com.edu.com.major.dto.MajorDto;
import com.edu.com.major.dto.request.CreateMajorRequest;
import com.edu.com.major.dto.request.UpdateMajorRequest;
import com.edu.com.major.service.MajorService;
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

@Tag(name = "Majors")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/majors")
public class MajorController {

    private final MajorService service;

    @PostMapping
    @CheckPermission("MAJOR_CREATE")
    public ResponseEntity<ApiResponse<MajorDto>> createMajor(
            @Valid @RequestBody CreateMajorRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<MajorDto>builder()
                        .success(true)
                        .data(service.createMajor(request))
                        .build()
        );
    }

    @GetMapping
    @CheckPermission("MAJOR_READ")
    public ResponseEntity<ApiResponse<List<MajorDto>>> getMajors(
            @RequestParam(name = "page", defaultValue = "0") @Min(0) Integer page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) Integer size
    ) {
        return ResponseEntity.ok(
                ApiResponse.<List<MajorDto>>builder()
                        .success(true)
                        .data(service.getMajors(page, size))
                        .build()
        );
    }

    @GetMapping("/{majorId}")
    @CheckPermission("MAJOR_READ")
    public ResponseEntity<ApiResponse<MajorDto>> getMajor(
            @PathVariable("majorId") UUID majorId
    ) {
        return ResponseEntity.ok(
                ApiResponse.<MajorDto>builder()
                        .success(true)
                        .data(service.getMajor(majorId))
                        .build()
        );
    }

    @PatchMapping("/{majorId}")
    @CheckPermission("MAJOR_UPDATE")
    public ResponseEntity<ApiResponse<MajorDto>> updateMajor(
            @PathVariable("majorId") UUID majorId,
            @Valid @RequestBody UpdateMajorRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.<MajorDto>builder()
                        .success(true)
                        .data(service.updateMajor(majorId, request))
                        .build()
        );
    }

    @DeleteMapping("/{majorId}")
    @CheckPermission("MAJOR_DELETE")
    public ResponseEntity<ApiResponse<Void>> deleteMajor(
            @PathVariable("majorId") UUID majorId
    ) {
        service.deleteMajor(majorId);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Major deleted successfully")
                        .build()
        );
    }
}
