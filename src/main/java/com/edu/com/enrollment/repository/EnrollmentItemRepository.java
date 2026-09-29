package com.edu.com.enrollment.repository;

import com.edu.com.enrollment.domain.EnrollmentItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public interface EnrollmentItemRepository extends JpaRepository<EnrollmentItem, UUID> {
    boolean existsByCourseOfferingId(UUID courseOfferingId);

    boolean existsByEnrollmentId(UUID enrollmentId);

    long countByCourseOfferingId(UUID courseOfferingId);

    @Query("select i.courseOffering.id as courseOfferingId, count(i) as enrolledCount from EnrollmentItem i "
            + "where i.courseOffering.id in :offeringIds group by i.courseOffering.id")
    List<CourseOfferingEnrollmentCount> findEnrollmentCounts(@Param("offeringIds") List<UUID> offeringIds);

    default Map<UUID, Long> countByCourseOfferingIds(List<UUID> offeringIds) {
        if (offeringIds.isEmpty()) {
            return Map.of();
        }
        return findEnrollmentCounts(offeringIds).stream().collect(Collectors.toMap(
                CourseOfferingEnrollmentCount::getCourseOfferingId, CourseOfferingEnrollmentCount::getEnrolledCount));
    }

    interface CourseOfferingEnrollmentCount {
        UUID getCourseOfferingId();
        long getEnrolledCount();
    }

    Optional<EnrollmentItem> findByIdAndEnrollmentId(UUID id, UUID enrollmentId);

    @EntityGraph(attributePaths = {"courseOffering.course", "courseOffering.semester"})
    List<EnrollmentItem> findAllByEnrollmentId(UUID enrollmentId);
}
