package com.edu.com.enrollment.policy;

import com.edu.com.common.exception.ApiException;
import com.edu.com.common.exception.ErrorCode;
import com.edu.com.enrollment.domain.EnrollmentPlanType;
import org.springframework.stereotype.Component;

@Component
public class RegularEnrollmentPlanPolicy implements EnrollmentPlanPolicy {
    @Override
    public EnrollmentPlanType getPlanType() {
        return EnrollmentPlanType.REGULAR;
    }

    @Override
    public int getMaxCreditUnits() {
        return 24;
    }

    @Override
    public void validate(EnrollmentPlanContext context) {
        if (context.countOutsideMajorCourses() > 0) {
            throw new ApiException(ErrorCode.ENROLLMENT_COURSE_MAJOR_MISMATCH);
        }
    }
}
