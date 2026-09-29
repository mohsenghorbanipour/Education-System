package com.edu.com.course.service;

import com.edu.com.common.exception.ApiException;
import com.edu.com.enrollment.service.EnrollmentConsistencyLock;
import com.edu.com.enrollment.service.EnrollmentIntegrityService;
import com.edu.com.common.exception.ErrorCode;
import com.edu.com.course.domain.Course;
import com.edu.com.course.domain.CourseOffering;
import com.edu.com.course.dto.CourseDto;
import com.edu.com.course.dto.CourseOfferingDto;
import com.edu.com.course.dto.request.CreateCourseOfferingRequest;
import com.edu.com.course.dto.request.UpdateCourseOfferingRequest;
import com.edu.com.course.repository.CourseMajorRepository;
import com.edu.com.course.repository.CourseOfferingRepository;
import com.edu.com.course.repository.CourseRepository;
import com.edu.com.enrollment.repository.EnrollmentItemRepository;
import com.edu.com.major.dto.MajorDto;
import com.edu.com.semester.domain.Semester;
import com.edu.com.semester.dto.SemesterDto;
import com.edu.com.semester.repository.SemesterRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseOfferingService {

    private final CourseOfferingRepository repository;
    private final CourseRepository courseRepository;
    private final SemesterRepository semesterRepository;
    private final CourseMajorRepository courseMajorRepository;
    private final EnrollmentItemRepository enrollmentItemRepository;
    private final EnrollmentConsistencyLock consistencyLock;
    private final EnrollmentIntegrityService integrityService;

    private final ModelMapper mapper;

    @Transactional
    public CourseOfferingDto createCourseOffering(CreateCourseOfferingRequest request) {
        Course course = findCourse(request.courseId());
        Semester semester = findSemester(request.semesterId());
        if (repository.existsByCourseIdAndSemesterId(request.courseId(), request.semesterId())) {
            throw new ApiException(ErrorCode.COURSE_OFFERING_ALREADY_EXISTS);
        }

        CourseOffering offering = CourseOffering.builder()
                .course(course)
                .semester(semester)
                .guestAllowed(request.guestAllowed())
                .capacity(request.capacity())
                .build();

        return toDtos(List.of(saveOffering(offering))).getFirst();
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public List<CourseOfferingDto> getCourseOfferings(UUID semesterId, Integer page, Integer size) {
        if (semesterId != null && !semesterRepository.existsById(semesterId)) {
            throw new ApiException(ErrorCode.SEMESTER_NOT_FOUND);
        }

        PageRequest pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("semester.startDate"), Sort.Order.asc("course.code"), Sort.Order.asc("id")));
        List<CourseOffering> offerings = semesterId == null
                ? repository.findAllBy(pageable)
                : repository.findAllBySemesterId(semesterId, pageable);

        return toDtos(offerings);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public CourseOfferingDto getCourseOffering(UUID courseOfferingId) {
        CourseOffering offering = repository.findById(courseOfferingId)
                .orElseThrow(() -> new ApiException(ErrorCode.COURSE_OFFERING_NOT_FOUND));
        return toDtos(List.of(offering)).getFirst();
    }

    @Transactional
    public CourseOfferingDto updateCourseOffering(UUID courseOfferingId, UpdateCourseOfferingRequest request) {
        consistencyLock.forCatalogChange();
        CourseOffering offering = findOfferingForUpdate(courseOfferingId);
        UUID courseId = request.courseId() == null ? offering.getCourse().getId() : request.courseId();
        UUID semesterId = request.semesterId() == null ? offering.getSemester().getId() : request.semesterId();
        boolean courseChanged = !courseId.equals(offering.getCourse().getId());
        boolean semesterChanged = !semesterId.equals(offering.getSemester().getId());

        if ((courseChanged || semesterChanged) && enrollmentItemRepository.existsByCourseOfferingId(courseOfferingId)) {
            throw new ApiException(ErrorCode.COURSE_OFFERING_IN_USE);
        }

        Course course = courseChanged ? findCourse(courseId) : offering.getCourse();
        Semester semester = semesterChanged ? findSemester(semesterId) : offering.getSemester();
        if ((courseChanged || semesterChanged)
                && repository.existsByCourseIdAndSemesterIdAndIdNot(courseId, semesterId, courseOfferingId)) {
            throw new ApiException(ErrorCode.COURSE_OFFERING_ALREADY_EXISTS);
        }

        if (request.capacity() != null) {
            if (request.capacity() < enrollmentItemRepository.countByCourseOfferingId(courseOfferingId)) {
                throw new ApiException(ErrorCode.COURSE_OFFERING_CAPACITY_BELOW_ENROLLED_COUNT);
            }
            offering.setCapacity(request.capacity());
        }

        offering.setCourse(course);
        offering.setSemester(semester);
        if (request.guestAllowed() != null) {
            offering.setGuestAllowed(request.guestAllowed());
        }

        offering = saveOffering(offering);
        if (request.guestAllowed() != null) {
            integrityService.validateOfferingChange(courseOfferingId);
        }
        return toDtos(List.of(offering)).getFirst();
    }

    @Transactional
    public void deleteCourseOffering(UUID courseOfferingId) {
        CourseOffering offering = findOfferingForUpdate(courseOfferingId);
        if (enrollmentItemRepository.existsByCourseOfferingId(courseOfferingId)) {
            throw new ApiException(ErrorCode.COURSE_OFFERING_IN_USE);
        }

        try {
            repository.delete(offering);
            repository.flush();
        } catch (DataIntegrityViolationException exception) {
            if (isConstraintViolation(exception, "23503", "fk_enrollment_items_course_offering")) {
                throw new ApiException(ErrorCode.COURSE_OFFERING_IN_USE);
            }
            throw exception;
        }
    }

    private Course findCourse(UUID courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new ApiException(ErrorCode.COURSE_NOT_FOUND));
    }

    private Semester findSemester(UUID semesterId) {
        return semesterRepository.findById(semesterId)
                .orElseThrow(() -> new ApiException(ErrorCode.SEMESTER_NOT_FOUND));
    }

    private CourseOffering findOfferingForUpdate(UUID courseOfferingId) {
        return repository.findByIdForUpdate(courseOfferingId)
                .orElseThrow(() -> new ApiException(ErrorCode.COURSE_OFFERING_NOT_FOUND));
    }

    private CourseOffering saveOffering(CourseOffering offering) {
        try {
            return repository.saveAndFlush(offering);
        } catch (DataIntegrityViolationException exception) {
            if (isConstraintViolation(exception, "23505", "uk_course_offering_semester_course")) {
                throw new ApiException(ErrorCode.COURSE_OFFERING_ALREADY_EXISTS);
            }
            if (isConstraintViolation(exception, "23503", "fk_course_offerings_course")) {
                throw new ApiException(ErrorCode.COURSE_NOT_FOUND);
            }
            if (isConstraintViolation(exception, "23503", "fk_course_offerings_semester")) {
                throw new ApiException(ErrorCode.SEMESTER_NOT_FOUND);
            }
            throw exception;
        }
    }

    private List<CourseOfferingDto> toDtos(List<CourseOffering> offerings) {
        if (offerings.isEmpty()) {
            return List.of();
        }

        List<UUID> courseIds = offerings.stream()
                .map(offering -> offering.getCourse().getId()).distinct().toList();
        Map<UUID, List<MajorDto>> majorsByCourseId = courseMajorRepository.findAllByCourseIdIn(courseIds).stream()
                .collect(Collectors.groupingBy(assignment -> assignment.getCourse().getId(),
                        Collectors.mapping(assignment -> mapper.map(assignment.getMajor(), MajorDto.class),
                                Collectors.toList())));
        Map<UUID, Long> enrolledCounts = enrollmentItemRepository.countByCourseOfferingIds(
                offerings.stream().map(CourseOffering::getId).toList());

        return offerings.stream().map(offering -> {
            CourseDto course = mapper.map(offering.getCourse(), CourseDto.class);
            course.setMajors(majorsByCourseId.getOrDefault(course.getId(), List.of()));
            long enrolledCount = enrolledCounts.getOrDefault(offering.getId(), 0L);
            return new CourseOfferingDto(offering.getId(), course,
                    mapper.map(offering.getSemester(), SemesterDto.class), offering.isGuestAllowed(),
                    offering.getCapacity(), enrolledCount, offering.getCapacity() - enrolledCount);
        }).toList();
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
