package com.edu.com.enrollment.service;

import com.edu.com.common.exception.ApiException;
import com.edu.com.common.exception.ErrorCode;
import com.edu.com.course.domain.CourseOffering;
import com.edu.com.course.repository.CourseMajorRepository;
import com.edu.com.enrollment.domain.EnrollmentPlanType;
import com.edu.com.enrollment.policy.EnrollmentPlanContext;
import com.edu.com.enrollment.policy.EnrollmentPlanPolicy;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Component
public class EnrollmentPlanValidator {
    private final Map<EnrollmentPlanType, EnrollmentPlanPolicy> policies;
    private final CourseMajorRepository courseMajorRepository;

    public EnrollmentPlanValidator(List<EnrollmentPlanPolicy> policies, CourseMajorRepository courseMajorRepository) {
        this.policies = policies.stream().collect(Collectors.toUnmodifiableMap(
                EnrollmentPlanPolicy::getPlanType, Function.identity()));
        if (this.policies.size() != EnrollmentPlanType.values().length) {
            throw new IllegalStateException("Every enrollment plan must have a policy");
        }
        this.courseMajorRepository = courseMajorRepository;
    }

    public void validate(UUID studentMajorId, UUID semesterId, EnrollmentPlanType planType,
            List<CourseOffering> offerings) {
        validate(studentMajorId, semesterId, planType, offerings, () -> loadMajorIds(offerings));
    }

    public boolean canAdd(UUID studentMajorId, UUID semesterId, EnrollmentPlanType planType,
            List<CourseOffering> selected, CourseOffering candidate, Map<UUID, Set<UUID>> majorIds) {
        List<CourseOffering> combined = new ArrayList<>(selected);
        combined.add(candidate);
        try {
            validate(studentMajorId, semesterId, planType, combined, () -> majorIds);
            return true;
        } catch (ApiException exception) {
            return false;
        }
    }

    public Map<UUID, Set<UUID>> loadMajorIds(List<CourseOffering> offerings) {
        List<UUID> selectedCourseIds = offerings.stream().map(offering -> offering.getCourse().getId()).distinct().toList();
        if (selectedCourseIds.isEmpty()) {
            return Map.of();
        }
        return courseMajorRepository.findAllByCourseIdIn(selectedCourseIds).stream()
                .collect(Collectors.groupingBy(assignment -> assignment.getCourse().getId(),
                        Collectors.mapping(assignment -> assignment.getMajor().getId(), Collectors.toSet())));
    }

    private void validate(UUID studentMajorId, UUID semesterId, EnrollmentPlanType planType,
            List<CourseOffering> offerings, Supplier<Map<UUID, Set<UUID>>> majorIds) {
        if (offerings.isEmpty()) {
            return;
        }
        if (studentMajorId == null) {
            throw new ApiException(ErrorCode.STUDENT_MAJOR_REQUIRED);
        }

        Set<UUID> courseIds = new HashSet<>();
        long totalCreditUnits = 0;
        for (CourseOffering offering : offerings) {
            if (!semesterId.equals(offering.getSemester().getId())) {
                throw new ApiException(ErrorCode.ENROLLMENT_COURSE_SEMESTER_MISMATCH);
            }
            if (!courseIds.add(offering.getCourse().getId())) {
                throw new ApiException(ErrorCode.ENROLLMENT_DUPLICATE_COURSE);
            }
            totalCreditUnits += offering.getCourse().getCreditUnits();
        }

        EnrollmentPlanPolicy policy = policies.get(planType);
        if (totalCreditUnits > policy.getMaxCreditUnits()) {
            throw new ApiException(ErrorCode.ENROLLMENT_CREDIT_LIMIT_EXCEEDED);
        }

        policy.validate(new EnrollmentPlanContext(studentMajorId, offerings, majorIds.get()));
    }
}
