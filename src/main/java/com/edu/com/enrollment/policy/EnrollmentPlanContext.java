package com.edu.com.enrollment.policy;

import com.edu.com.course.domain.CourseOffering;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record EnrollmentPlanContext(
        UUID studentMajorId,
        List<CourseOffering> offerings,
        Map<UUID, Set<UUID>> majorIdsByCourseId
) {
    public long countOutsideMajorCourses() {
        return offerings.stream()
                .filter(offering -> !majorIdsByCourseId.getOrDefault(offering.getCourse().getId(), Set.of())
                        .contains(studentMajorId))
                .count();
    }
}
