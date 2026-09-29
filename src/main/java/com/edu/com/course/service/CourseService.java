package com.edu.com.course.service;

import com.edu.com.common.exception.ApiException;
import com.edu.com.enrollment.service.EnrollmentConsistencyLock;
import com.edu.com.enrollment.service.EnrollmentIntegrityService;
import com.edu.com.common.exception.ErrorCode;
import com.edu.com.course.domain.Course;
import com.edu.com.course.domain.CourseMajor;
import com.edu.com.course.dto.CourseDto;
import com.edu.com.course.dto.request.CreateCourseRequest;
import com.edu.com.course.dto.request.UpdateCourseRequest;
import com.edu.com.course.repository.CourseMajorRepository;
import com.edu.com.course.repository.CourseOfferingRepository;
import com.edu.com.course.repository.CourseRepository;
import com.edu.com.major.domain.Major;
import com.edu.com.major.dto.MajorDto;
import com.edu.com.major.repository.MajorRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository repository;
    private final CourseMajorRepository courseMajorRepository;
    private final MajorRepository majorRepository;
    private final CourseOfferingRepository courseOfferingRepository;

    private final EnrollmentConsistencyLock consistencyLock;
    private final EnrollmentIntegrityService integrityService;

    private final ModelMapper mapper;

    @Transactional
    public CourseDto createCourse(CreateCourseRequest request) {
        String name = request.name().strip();
        String code = request.code().strip();

        if (repository.existsByCode(code)) {
            throw new ApiException(ErrorCode.COURSE_CODE_ALREADY_EXISTS);
        }

        List<Major> majors = majorRepository.findAllById(request.majorIds());
        if (majors.size() != request.majorIds().size()) {
            throw new ApiException(ErrorCode.MAJOR_NOT_FOUND);
        }

        Course course;
        try {
            course = repository.saveAndFlush(
                    Course.builder()
                            .name(name)
                            .code(code)
                            .creditUnits(request.creditUnits())
                            .build()
            );
            courseMajorRepository.saveAllAndFlush(majors.stream()
                    .map(major -> CourseMajor.builder()
                            .course(course)
                            .major(major)
                            .build()).toList());
        } catch (DataIntegrityViolationException exception) {
            throw translateWriteException(exception);
        }

        return toDto(course, majors);
    }

    @Transactional(readOnly = true)
    public List<CourseDto> getCourses(int page, int size) {
        List<Course> courses = repository.findAll(Pageable.ofSize(size).withPage(page)).getContent();
        if (courses.isEmpty()) {
            return List.of();
        }

        List<UUID> courseIds = courses.stream().map(Course::getId).toList();
        Map<UUID, List<Major>> majorsByCourseId = courseMajorRepository.findAllByCourseIdIn(courseIds)
                .stream()
                .collect(Collectors.groupingBy(
                        assignment -> assignment.getCourse().getId(),
                        Collectors.mapping(CourseMajor::getMajor, Collectors.toList())));

        return courses.stream()
                .map(course -> toDto(course, majorsByCourseId.getOrDefault(course.getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public CourseDto getCourse(UUID courseId) {
        Course course = repository.findById(courseId)
                .orElseThrow(() -> new ApiException(ErrorCode.COURSE_NOT_FOUND));
        List<Major> majors = courseMajorRepository.findAllByCourseIdIn(List.of(courseId)).stream()
                .map(CourseMajor::getMajor)
                .toList();
        return toDto(course, majors);
    }

    @Transactional
    public CourseDto updateCourse(UUID courseId, UpdateCourseRequest request) {
        consistencyLock.forCatalogChange();
        Course course = findCourseForUpdate(courseId);
        String code = request.code() == null ? null : request.code().strip();
        if (code != null && repository.existsByCodeAndIdNot(code, courseId)) {
            throw new ApiException(ErrorCode.COURSE_CODE_ALREADY_EXISTS);
        }

        List<CourseMajor> assignments = courseMajorRepository.findAllByCourseIdIn(List.of(courseId));
        List<Major> majors = request.majorIds() == null
                ? assignments.stream().map(CourseMajor::getMajor).toList()
                : majorRepository.findAllById(request.majorIds());
        if (request.majorIds() != null && majors.size() != request.majorIds().size()) {
            throw new ApiException(ErrorCode.MAJOR_NOT_FOUND);
        }

        if (request.name() != null) {
            course.setName(request.name().strip());
        }
        if (code != null) {
            course.setCode(code);
        }
        if (request.creditUnits() != null) {
            course.setCreditUnits(request.creditUnits());
        }

        try {
            if (request.majorIds() != null) {
                List<CourseMajor> removed = assignments.stream()
                        .filter(assignment -> !request.majorIds().contains(assignment.getMajor().getId()))
                        .toList();
                if (!removed.isEmpty()) {
                    courseMajorRepository.deleteAll(removed);
                }
            }
            repository.saveAndFlush(course);

            if (request.majorIds() != null) {
                Set<UUID> existingMajorIds = assignments.stream()
                        .map(assignment -> assignment.getMajor().getId())
                        .collect(Collectors.toSet());
                List<CourseMajor> added = majors.stream()
                        .filter(major -> !existingMajorIds.contains(major.getId()))
                        .map(major -> CourseMajor.builder().course(course).major(major).build())
                        .toList();
                if (!added.isEmpty()) {
                    courseMajorRepository.saveAllAndFlush(added);
                }
            }
        } catch (DataIntegrityViolationException exception) {
            throw translateWriteException(exception);
        }

        if (request.creditUnits() != null || request.majorIds() != null) {
            integrityService.validateCourseChange(courseId);
        }

        return toDto(course, majors);
    }

    @Transactional
    public void deleteCourse(UUID courseId) {
        Course course = findCourseForUpdate(courseId);
        if (courseOfferingRepository.existsByCourseId(courseId)) {
            throw new ApiException(ErrorCode.COURSE_IN_USE);
        }

        try {
            List<CourseMajor> assignments = courseMajorRepository.findAllByCourseIdIn(List.of(courseId));
            if (!assignments.isEmpty()) {
                courseMajorRepository.deleteAll(assignments);
                courseMajorRepository.flush();
            }
            repository.delete(course);
            repository.flush();
        } catch (DataIntegrityViolationException exception) {
            if (isConstraintViolation(exception, "23503", "fk_course_offerings_course")
                    || isConstraintViolation(exception, "23503", "fk_course_majors_course")) {
                throw new ApiException(ErrorCode.COURSE_IN_USE);
            }
            throw exception;
        }
    }

    private Course findCourseForUpdate(UUID courseId) {
        return repository.findByIdForUpdate(courseId)
                .orElseThrow(() -> new ApiException(ErrorCode.COURSE_NOT_FOUND));
    }

    private RuntimeException translateWriteException(DataIntegrityViolationException exception) {
        if (isConstraintViolation(exception, "23505", "uk_courses_code")) {
            return new ApiException(ErrorCode.COURSE_CODE_ALREADY_EXISTS);
        }
        if (isConstraintViolation(exception, "23503", "fk_course_majors_major")) {
            return new ApiException(ErrorCode.MAJOR_NOT_FOUND);
        }
        return exception;
    }

    private CourseDto toDto(Course course, List<Major> majors) {
        CourseDto dto = mapper.map(course, CourseDto.class);
        dto.setMajors(majors.stream()
                .map(major -> mapper.map(major, MajorDto.class))
                .toList());
        return dto;
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
