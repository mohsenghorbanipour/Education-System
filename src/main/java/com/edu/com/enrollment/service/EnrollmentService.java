package com.edu.com.enrollment.service;

import com.edu.com.common.exception.ApiException;
import com.edu.com.common.exception.ErrorCode;
import com.edu.com.course.domain.CourseOffering;
import com.edu.com.course.dto.CourseDto;
import com.edu.com.course.dto.CourseOfferingDto;
import com.edu.com.course.repository.CourseMajorRepository;
import com.edu.com.course.repository.CourseOfferingRepository;
import com.edu.com.enrollment.domain.Enrollment;
import com.edu.com.enrollment.domain.EnrollmentItem;
import com.edu.com.enrollment.dto.EnrollmentDetailDto;
import com.edu.com.enrollment.dto.EnrollmentDto;
import com.edu.com.enrollment.dto.EnrollmentItemDto;
import com.edu.com.enrollment.dto.EnrollmentStudentDto;
import com.edu.com.enrollment.dto.request.CreateEnrollmentRequest;
import com.edu.com.enrollment.dto.request.AddEnrollmentItemRequest;
import com.edu.com.enrollment.dto.request.UpdateEnrollmentRequest;
import com.edu.com.enrollment.repository.EnrollmentItemRepository;
import com.edu.com.enrollment.repository.EnrollmentRepository;
import com.edu.com.major.dto.MajorDto;
import com.edu.com.semester.domain.Semester;
import com.edu.com.semester.dto.SemesterDto;
import com.edu.com.semester.repository.SemesterRepository;
import com.edu.com.user.domain.User;
import com.edu.com.user.domain.UserType;
import com.edu.com.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository repository;
    private final UserRepository userRepository;
    private final SemesterRepository semesterRepository;
    private final CourseMajorRepository courseMajorRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final EnrollmentItemRepository enrollmentItemRepository;

    private final EnrollmentPlanValidator planValidator;

    private final EnrollmentConsistencyLock consistencyLock;

    private final ModelMapper mapper;

    @Transactional
    public EnrollmentDto createEnrollment(CreateEnrollmentRequest request) {
        consistencyLock.forEnrollmentChange();
        User student = userRepository.findById(request.userId())
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        if (student.getUserType() != UserType.STUDENT) {
            throw new ApiException(ErrorCode.ENROLLMENT_STUDENT_REQUIRED);
        }
        if (!student.isEnabled()) {
            throw new ApiException(ErrorCode.ACCOUNT_DISABLED);
        }
        if (student.getMajor() == null) {
            throw new ApiException(ErrorCode.STUDENT_MAJOR_REQUIRED);
        }

        Semester semester = semesterRepository.findById(request.semesterId())
                .orElseThrow(() -> new ApiException(ErrorCode.SEMESTER_NOT_FOUND));
        if (repository.existsByUserIdAndSemesterId(request.userId(), request.semesterId())) {
            throw new ApiException(ErrorCode.ENROLLMENT_ALREADY_EXISTS);
        }

        Enrollment enrollment = Enrollment.builder()
                .user(student)
                .semester(semester)
                .planType(request.planType())
                .build();
        try {
            enrollment = repository.saveAndFlush(enrollment);
        } catch (DataIntegrityViolationException exception) {
            if (isConstraintViolation(exception, "23505", "uk_enrollment_user_semester")) {
                throw new ApiException(ErrorCode.ENROLLMENT_ALREADY_EXISTS);
            }
            if (isConstraintViolation(exception, "23503", "fk_enrollments_user")) {
                throw new ApiException(ErrorCode.USER_NOT_FOUND);
            }
            if (isConstraintViolation(exception, "23503", "fk_enrollments_semester")) {
                throw new ApiException(ErrorCode.SEMESTER_NOT_FOUND);
            }
            throw exception;
        }

        return toDto(enrollment);
    }

    @Transactional(readOnly = true)
    public List<EnrollmentDto> getEnrollments(UUID userId, UUID semesterId, Integer page, Integer size) {
        if (userId != null && !userRepository.existsById(userId)) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
        validateSemesterFilter(semesterId);
        return findEnrollments(userId, semesterId, page, size);
    }

    @Transactional(readOnly = true)
    public List<EnrollmentDto> getOwnEnrollments(UUID currentUserId, UUID semesterId, Integer page, Integer size) {
        validateSemesterFilter(semesterId);
        return findEnrollments(currentUserId, semesterId, page, size);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public EnrollmentDetailDto getEnrollment(UUID enrollmentId) {
        Enrollment enrollment = repository.findDetailedById(enrollmentId)
                .orElseThrow(() -> new ApiException(ErrorCode.ENROLLMENT_NOT_FOUND));
        return toDetailDto(enrollment);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public EnrollmentDetailDto getOwnEnrollment(UUID currentUserId, UUID enrollmentId) {
        Enrollment enrollment = repository.findDetailedByIdAndUserId(enrollmentId, currentUserId)
                .orElseThrow(() -> new ApiException(ErrorCode.ENROLLMENT_NOT_FOUND));
        return toDetailDto(enrollment);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public List<CourseOfferingDto> getAvailableCourseOfferings(UUID currentUserId, UUID enrollmentId,
            int page, int size) {
        Enrollment enrollment = repository.findDetailedByIdAndUserId(enrollmentId, currentUserId)
                .orElseThrow(() -> new ApiException(ErrorCode.ENROLLMENT_NOT_FOUND));
        if (!enrollment.getUser().isEnabled()) {
            throw new ApiException(ErrorCode.ACCOUNT_DISABLED);
        }
        if (enrollment.getUser().getMajor() == null) {
            throw new ApiException(ErrorCode.STUDENT_MAJOR_REQUIRED);
        }
        UUID majorId = enrollment.getUser().getMajor().getId();
        UUID semesterId = enrollment.getSemester().getId();
        List<CourseOffering> selected = enrollment.getItems().stream().map(EnrollmentItem::getCourseOffering).toList();
        List<CourseOffering> available = new ArrayList<>();
        Map<UUID, Long> enrolledCounts = new HashMap<>();
        long skip = (long) page * size;
        int batchSize = 100;
        for (int batch = 0; available.size() < size; batch++) {
            List<CourseOffering> candidates = courseOfferingRepository.findAllBySemesterId(semesterId,
                    PageRequest.of(batch, batchSize, Sort.by("course.code", "id")));
            if (candidates.isEmpty()) {
                break;
            }
            List<CourseOffering> combined = new ArrayList<>(selected);
            combined.addAll(candidates);
            var majorIds = planValidator.loadMajorIds(combined);
            enrolledCounts.putAll(enrollmentItemRepository.countByCourseOfferingIds(
                    candidates.stream().map(CourseOffering::getId).toList()));
            for (CourseOffering candidate : candidates) {
                if (enrolledCounts.getOrDefault(candidate.getId(), 0L) < candidate.getCapacity()
                        && planValidator.canAdd(majorId, semesterId, enrollment.getPlanType(), selected, candidate, majorIds)) {
                    if (skip > 0) {
                        skip--;
                    } else {
                        available.add(candidate);
                        if (available.size() == size) {
                            break;
                        }
                    }
                }
            }
            if (candidates.size() < batchSize) {
                break;
            }
        }
        Map<UUID, List<MajorDto>> majors = loadOfferingMajors(available);
        return available.stream().map(offering -> toOfferingDto(offering, majors,
                enrolledCounts.getOrDefault(offering.getId(), 0L))).toList();
    }

    @Transactional
    public EnrollmentDto updateEnrollment(UUID enrollmentId, UpdateEnrollmentRequest request) {
        consistencyLock.forEnrollmentChange();
        Enrollment enrollment = findEnrollmentForUpdate(enrollmentId);
        if (enrollment.getPlanType() == request.planType()) {
            return toDto(enrollment);
        }

        List<CourseOffering> offerings = enrollmentItemRepository.findAllByEnrollmentId(enrollmentId).stream()
                .map(EnrollmentItem::getCourseOffering).toList();
        UUID majorId = enrollment.getUser().getMajor() == null ? null : enrollment.getUser().getMajor().getId();
        planValidator.validate(majorId, enrollment.getSemester().getId(), request.planType(), offerings);
        enrollment.setPlanType(request.planType());
        return toDto(repository.saveAndFlush(enrollment));
    }

    @Transactional
    public void deleteEnrollment(UUID enrollmentId) {
        consistencyLock.forEnrollmentChange();
        Enrollment enrollment = findEnrollmentForUpdate(enrollmentId);
        if (enrollmentItemRepository.existsByEnrollmentId(enrollmentId)) {
            throw new ApiException(ErrorCode.ENROLLMENT_IN_USE);
        }
        repository.delete(enrollment);
        repository.flush();
    }

    @Transactional
    public EnrollmentItemDto addOwnEnrollmentItem(UUID currentUserId, UUID enrollmentId,
            AddEnrollmentItemRequest request) {
        consistencyLock.forEnrollmentChange();
        Enrollment enrollment = findOwnEnrollmentForUpdate(currentUserId, enrollmentId);
        CourseOffering offering = courseOfferingRepository.findByIdForUpdate(request.courseOfferingId())
                .orElseThrow(() -> new ApiException(ErrorCode.COURSE_OFFERING_NOT_FOUND));
        List<CourseOffering> offerings = new ArrayList<>(enrollmentItemRepository.findAllByEnrollmentId(enrollmentId)
                .stream().map(EnrollmentItem::getCourseOffering).toList());
        offerings.add(offering);
        UUID majorId = enrollment.getUser().getMajor() == null ? null : enrollment.getUser().getMajor().getId();
        planValidator.validate(majorId, enrollment.getSemester().getId(), enrollment.getPlanType(), offerings);

        long enrolledCount = enrollmentItemRepository.countByCourseOfferingId(offering.getId());
        if (enrolledCount >= offering.getCapacity()) {
            throw new ApiException(ErrorCode.COURSE_OFFERING_CAPACITY_FULL);
        }

        EnrollmentItem item;
        try {
            item = enrollmentItemRepository.saveAndFlush(new EnrollmentItem(enrollment, offering));
        } catch (DataIntegrityViolationException exception) {
            if (isConstraintViolation(exception, "23505", "uk_enrollment_item_offering")) {
                throw new ApiException(ErrorCode.ENROLLMENT_DUPLICATE_COURSE);
            }
            if (isConstraintViolation(exception, "23503", "fk_enrollment_items_enrollment")) {
                throw new ApiException(ErrorCode.ENROLLMENT_NOT_FOUND);
            }
            if (isConstraintViolation(exception, "23503", "fk_enrollment_items_course_offering")) {
                throw new ApiException(ErrorCode.COURSE_OFFERING_NOT_FOUND);
            }
            throw exception;
        }
        return toItemDto(item, loadMajors(List.of(item)), enrolledCount + 1);
    }

    @Transactional
    public void removeOwnEnrollmentItem(UUID currentUserId, UUID enrollmentId, UUID itemId) {
        consistencyLock.forEnrollmentChange();
        findOwnEnrollmentForUpdate(currentUserId, enrollmentId);
        EnrollmentItem item = enrollmentItemRepository.findByIdAndEnrollmentId(itemId, enrollmentId)
                .orElseThrow(() -> new ApiException(ErrorCode.ENROLLMENT_ITEM_NOT_FOUND));
        courseOfferingRepository.findByIdForUpdate(item.getCourseOffering().getId())
                .orElseThrow(() -> new ApiException(ErrorCode.COURSE_OFFERING_NOT_FOUND));
        enrollmentItemRepository.delete(item);
        enrollmentItemRepository.flush();
    }

    private Enrollment findOwnEnrollmentForUpdate(UUID currentUserId, UUID enrollmentId) {
        Enrollment enrollment = repository.findByIdAndUserIdForUpdate(enrollmentId, currentUserId)
                .orElseThrow(() -> new ApiException(ErrorCode.ENROLLMENT_NOT_FOUND));
        if (!enrollment.getUser().isEnabled()) {
            throw new ApiException(ErrorCode.ACCOUNT_DISABLED);
        }
        return enrollment;
    }

    private Enrollment findEnrollmentForUpdate(UUID enrollmentId) {
        return repository.findByIdForUpdate(enrollmentId)
                .orElseThrow(() -> new ApiException(ErrorCode.ENROLLMENT_NOT_FOUND));
    }

    private void validateSemesterFilter(UUID semesterId) {
        if (semesterId != null && !semesterRepository.existsById(semesterId)) {
            throw new ApiException(ErrorCode.SEMESTER_NOT_FOUND);
        }
    }

    private List<EnrollmentDto> findEnrollments(UUID userId, UUID semesterId, Integer page, Integer size) {
        PageRequest pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("semester.startDate"), Sort.Order.asc("user.universityNumber"), Sort.Order.asc("id")));
        List<Enrollment> enrollments;
        if (userId != null && semesterId != null) {
            enrollments = repository.findAllByUserIdAndSemesterId(userId, semesterId, pageable);
        } else if (userId != null) {
            enrollments = repository.findAllByUserId(userId, pageable);
        } else if (semesterId != null) {
            enrollments = repository.findAllBySemesterId(semesterId, pageable);
        } else {
            enrollments = repository.findAllBy(pageable);
        }
        return enrollments.stream().map(this::toDto).toList();
    }

    private EnrollmentDto toDto(Enrollment enrollment) {
        return new EnrollmentDto(enrollment.getId(),
                mapper.map(enrollment.getUser(), EnrollmentStudentDto.class),
                mapper.map(enrollment.getSemester(), SemesterDto.class), enrollment.getPlanType());
    }

    private EnrollmentDetailDto toDetailDto(Enrollment enrollment) {
        EnrollmentDto details = toDto(enrollment);
        List<EnrollmentItem> items = enrollment.getItems().stream()
                .sorted(Comparator.comparing((EnrollmentItem item) -> item.getCourseOffering().getCourse().getCode())
                        .thenComparing(EnrollmentItem::getId))
                .toList();
        Map<UUID, List<MajorDto>> majorsByCourseId = loadMajors(items);
        Map<UUID, Long> enrolledCounts = enrollmentItemRepository.countByCourseOfferingIds(
                items.stream().map(item -> item.getCourseOffering().getId()).distinct().toList());
        List<EnrollmentItemDto> itemDtos = items.stream().map(item -> toItemDto(item, majorsByCourseId,
                enrolledCounts.getOrDefault(item.getCourseOffering().getId(), 0L))).toList();
        int totalCreditUnits = items.stream()
                .mapToInt(item -> item.getCourseOffering().getCourse().getCreditUnits()).sum();

        return new EnrollmentDetailDto(details.getId(), details.getStudent(), details.getSemester(),
                details.getPlanType(), itemDtos, totalCreditUnits);
    }

    private EnrollmentItemDto toItemDto(EnrollmentItem item, Map<UUID, List<MajorDto>> majorsByCourseId, long enrolledCount) {
        return new EnrollmentItemDto(item.getId(), toOfferingDto(item.getCourseOffering(), majorsByCourseId, enrolledCount));
    }

    private CourseOfferingDto toOfferingDto(CourseOffering offering, Map<UUID, List<MajorDto>> majorsByCourseId, long enrolledCount) {
        CourseDto course = mapper.map(offering.getCourse(), CourseDto.class);
        course.setMajors(majorsByCourseId.getOrDefault(course.getId(), List.of()));
        return new CourseOfferingDto(offering.getId(), course,
                mapper.map(offering.getSemester(), SemesterDto.class), offering.isGuestAllowed(),
                offering.getCapacity(), enrolledCount, offering.getCapacity() - enrolledCount);
    }

    private Map<UUID, List<MajorDto>> loadMajors(List<EnrollmentItem> items) {
        return loadOfferingMajors(items.stream().map(EnrollmentItem::getCourseOffering).toList());
    }

    private Map<UUID, List<MajorDto>> loadOfferingMajors(List<CourseOffering> offerings) {
        if (offerings.isEmpty()) {
            return Map.of();
        }

        List<UUID> courseIds = offerings.stream()
                .map(offering -> offering.getCourse().getId()).distinct().toList();
        return courseMajorRepository.findAllByCourseIdIn(courseIds).stream()
                .collect(Collectors.groupingBy(assignment -> assignment.getCourse().getId(),
                        Collectors.mapping(assignment -> mapper.map(assignment.getMajor(), MajorDto.class),
                                Collectors.toList())));
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
