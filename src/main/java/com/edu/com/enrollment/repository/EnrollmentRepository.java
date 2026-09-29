package com.edu.com.enrollment.repository;

import com.edu.com.enrollment.domain.Enrollment;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {
    boolean existsByUserId(UUID userId);

    boolean existsBySemesterId(UUID semesterId);

    boolean existsByUserIdAndSemesterId(UUID userId, UUID semesterId);

    @EntityGraph(attributePaths = {"user", "user.major", "semester"})
    List<Enrollment> findAllBy(Pageable pageable);

    @EntityGraph(attributePaths = {"user", "user.major", "semester"})
    List<Enrollment> findAllByUserId(UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "user.major", "semester"})
    List<Enrollment> findAllBySemesterId(UUID semesterId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "user.major", "semester"})
    List<Enrollment> findAllByUserIdAndSemesterId(UUID userId, UUID semesterId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select enrollment from Enrollment enrollment where enrollment.id = :enrollmentId")
    Optional<Enrollment> findByIdForUpdate(@Param("enrollmentId") UUID enrollmentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select enrollment from Enrollment enrollment where enrollment.id = :enrollmentId and enrollment.user.id = :userId")
    Optional<Enrollment> findByIdAndUserIdForUpdate(@Param("enrollmentId") UUID enrollmentId,
            @Param("userId") UUID userId);

    @EntityGraph(attributePaths = {"user", "user.major", "semester",
            "items.courseOffering.course", "items.courseOffering.semester"})
    Optional<Enrollment> findDetailedById(UUID id);

    @EntityGraph(attributePaths = {"user", "user.major", "semester",
            "items.courseOffering.course", "items.courseOffering.semester"})
    Optional<Enrollment> findDetailedByIdAndUserId(UUID id, UUID userId);

    @EntityGraph(attributePaths = {"user", "user.major", "semester",
            "items.courseOffering.course", "items.courseOffering.semester"})
    @Query("select e from Enrollment e where exists (select i.id from EnrollmentItem i where i.enrollment = e and i.courseOffering.course.id = :courseId)")
    List<Enrollment> findAllAffectedByCourse(@Param("courseId") UUID courseId);

    @EntityGraph(attributePaths = {"user", "user.major", "semester",
            "items.courseOffering.course", "items.courseOffering.semester"})
    @Query("select e from Enrollment e where exists (select i.id from EnrollmentItem i where i.enrollment = e and i.courseOffering.id = :offeringId)")
    List<Enrollment> findAllAffectedByOffering(@Param("offeringId") UUID offeringId);

    @EntityGraph(attributePaths = {"user", "user.major", "semester",
            "items.courseOffering.course", "items.courseOffering.semester"})
    List<Enrollment> findAllDetailedByUserId(UUID userId);
}
