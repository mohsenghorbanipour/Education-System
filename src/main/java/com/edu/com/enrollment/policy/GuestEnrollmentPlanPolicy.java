package com.edu.com.enrollment.policy;

import com.edu.com.common.exception.ApiException;
import com.edu.com.common.exception.ErrorCode;
import com.edu.com.enrollment.domain.EnrollmentPlanType;
import org.springframework.stereotype.Component;

@Component
public class GuestEnrollmentPlanPolicy implements EnrollmentPlanPolicy {
    @Override
    public EnrollmentPlanType getPlanType() {
        return EnrollmentPlanType.GUEST;
    }

    @Override
    public int getMaxCreditUnits() {
        return 20;
    }

    @Override
    public void validate(EnrollmentPlanContext context) {
        if (context.offerings().stream().anyMatch(offering -> !offering.isGuestAllowed())) {
            throw new ApiException(ErrorCode.ENROLLMENT_GUEST_COURSE_NOT_ALLOWED);
        }
    }
}
