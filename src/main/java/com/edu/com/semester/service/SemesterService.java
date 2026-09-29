package com.edu.com.semester.service;

import com.edu.com.common.exception.ApiException;
import com.edu.com.common.exception.ErrorCode;
import com.edu.com.course.repository.CourseOfferingRepository;
import com.edu.com.enrollment.repository.EnrollmentRepository;
import com.edu.com.semester.domain.Semester;
import com.edu.com.semester.dto.SemesterDto;
import com.edu.com.semester.dto.request.CreateSemesterRequest;
import com.edu.com.semester.dto.request.UpdateSemesterRequest;
import com.edu.com.semester.repository.SemesterRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SemesterService {

    private final SemesterRepository repository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ModelMapper mapper;

    @Transactional
    public SemesterDto createSemester(CreateSemesterRequest request) {
        if (!request.endDate().isAfter(request.startDate())) {
            throw new ApiException(ErrorCode.INVALID_SEMESTER_DATES);
        }

        String title = request.title().strip();
        if (repository.existsByTitle(title)) {
            throw new ApiException(ErrorCode.SEMESTER_TITLE_ALREADY_EXISTS);
        }

        Semester savedSemester;
        try {
            savedSemester = repository.saveAndFlush(Semester.builder()
                            .title(title)
                            .startDate(request.startDate())
                            .endDate(request.endDate())
                    .build());
        } catch (DataIntegrityViolationException exception) {
            if (isConstraintViolation(exception, "23505", "uk_semesters_title")) {
                throw new ApiException(ErrorCode.SEMESTER_TITLE_ALREADY_EXISTS);
            }
            throw exception;
        }

        return mapper.map(savedSemester, SemesterDto.class);
    }

    public List<SemesterDto> getSemesters(Integer page, Integer size) {
        PageRequest pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("startDate"), Sort.Order.asc("id")));

        return repository.findAllBy(pageable).stream()
                .map(semester -> mapper.map(semester, SemesterDto.class))
                .toList();
    }

    public SemesterDto getSemester(UUID semesterId) {
        Semester semester = repository.findById(semesterId)
                .orElseThrow(() -> new ApiException(ErrorCode.SEMESTER_NOT_FOUND));
        return mapper.map(semester, SemesterDto.class);
    }

    @Transactional
    public SemesterDto updateSemester(UUID semesterId, UpdateSemesterRequest request) {
        Semester semester = findSemesterForUpdate(semesterId);
        String title = request.title() == null ? semester.getTitle() : request.title().strip();
        LocalDate startDate = request.startDate() == null ? semester.getStartDate() : request.startDate();
        LocalDate endDate = request.endDate() == null ? semester.getEndDate() : request.endDate();

        if (!endDate.isAfter(startDate)) {
            throw new ApiException(ErrorCode.INVALID_SEMESTER_DATES);
        }
        if (request.title() != null && repository.existsByTitleAndIdNot(title, semesterId)) {
            throw new ApiException(ErrorCode.SEMESTER_TITLE_ALREADY_EXISTS);
        }

        semester.setTitle(title);
        semester.setStartDate(startDate);
        semester.setEndDate(endDate);

        try {
            semester = repository.saveAndFlush(semester);
        } catch (DataIntegrityViolationException exception) {
            if (isConstraintViolation(exception, "23505", "uk_semesters_title")) {
                throw new ApiException(ErrorCode.SEMESTER_TITLE_ALREADY_EXISTS);
            }
            throw exception;
        }

        return mapper.map(semester, SemesterDto.class);
    }

    @Transactional
    public void deleteSemester(UUID semesterId) {
        Semester semester = findSemesterForUpdate(semesterId);
        if (courseOfferingRepository.existsBySemesterId(semesterId)
                || enrollmentRepository.existsBySemesterId(semesterId)) {
            throw new ApiException(ErrorCode.SEMESTER_IN_USE);
        }

        try {
            repository.delete(semester);
            repository.flush();
        } catch (DataIntegrityViolationException exception) {
            if (isConstraintViolation(exception, "23503", "fk_course_offerings_semester")
                    || isConstraintViolation(exception, "23503", "fk_enrollments_semester")) {
                throw new ApiException(ErrorCode.SEMESTER_IN_USE);
            }
            throw exception;
        }
    }

    private Semester findSemesterForUpdate(UUID semesterId) {
        return repository.findByIdForUpdate(semesterId)
                .orElseThrow(() -> new ApiException(ErrorCode.SEMESTER_NOT_FOUND));
    }

    private boolean isConstraintViolation(DataIntegrityViolationException exception,
            String sqlState, String constraintName) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException violation
                    && sqlState.equals(violation.getSQLState())
                    && constraintName.equals(violation.getConstraintName())) {
                return true;
            }
        }
        return false;
    }
}
