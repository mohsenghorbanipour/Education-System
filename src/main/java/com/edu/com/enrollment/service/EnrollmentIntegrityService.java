package com.edu.com.enrollment.service;

import com.edu.com.common.exception.ApiException;
import com.edu.com.common.exception.ErrorCode;
import com.edu.com.enrollment.domain.Enrollment;
import com.edu.com.enrollment.domain.EnrollmentItem;
import com.edu.com.enrollment.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EnrollmentIntegrityService {
    private final EnrollmentRepository repository;
    private final EnrollmentPlanValidator validator;

    public void validateCourseChange(UUID courseId) {
        validate(repository.findAllAffectedByCourse(courseId));
    }

    public void validateOfferingChange(UUID offeringId) {
        validate(repository.findAllAffectedByOffering(offeringId));
    }

    public void validateStudentChange(UUID userId) {
        validate(repository.findAllDetailedByUserId(userId));
    }

    private void validate(List<Enrollment> enrollments) {
        for (Enrollment enrollment : enrollments) {
            UUID majorId = enrollment.getUser().getMajor() == null ? null : enrollment.getUser().getMajor().getId();
            try {
                validator.validate(majorId, enrollment.getSemester().getId(), enrollment.getPlanType(),
                        enrollment.getItems().stream().map(EnrollmentItem::getCourseOffering).toList());
            } catch (ApiException exception) {
                throw new ApiException(ErrorCode.ENROLLMENT_CHANGE_CONFLICT,
                        "The change would invalidate an existing enrollment: " + exception.getMessage());
            }
        }
    }
}
