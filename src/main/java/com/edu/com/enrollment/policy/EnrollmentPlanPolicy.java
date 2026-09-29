package com.edu.com.enrollment.policy;

import com.edu.com.enrollment.domain.EnrollmentPlanType;

public interface EnrollmentPlanPolicy {
    EnrollmentPlanType getPlanType();

    int getMaxCreditUnits();

    void validate(EnrollmentPlanContext context);
}
