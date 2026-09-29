package com.edu.com.enrollment.policy;

import com.edu.com.common.exception.ApiException;
import com.edu.com.common.exception.ErrorCode;
import com.edu.com.enrollment.domain.EnrollmentPlanType;
import org.springframework.stereotype.Component;

@Component
public class HonorsEnrollmentPlanPolicy implements EnrollmentPlanPolicy {
    @Override
    public EnrollmentPlanType getPlanType() {
        return EnrollmentPlanType.HONORS;
    }

    @Override
    public int getMaxCreditUnits() {
        return 28;
    }

    @Override
    public void validate(EnrollmentPlanContext context) {
        if (context.countOutsideMajorCourses() > 1) {
            throw new ApiException(ErrorCode.ENROLLMENT_OUTSIDE_MAJOR_LIMIT_EXCEEDED);
        }
    }
}
