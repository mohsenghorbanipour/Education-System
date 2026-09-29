package com.edu.com.course.repository;

import com.edu.com.course.domain.CourseOffering;
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
public interface CourseOfferingRepository extends JpaRepository<CourseOffering, UUID> {
    boolean existsByCourseId(UUID courseId);

    boolean existsBySemesterId(UUID semesterId);

    boolean existsByCourseIdAndSemesterId(UUID courseId, UUID semesterId);

    boolean existsByCourseIdAndSemesterIdAndIdNot(UUID courseId, UUID semesterId, UUID id);

    @EntityGraph(attributePaths = {"course", "semester"})
    List<CourseOffering> findAllBy(Pageable pageable);

    @EntityGraph(attributePaths = {"course", "semester"})
    List<CourseOffering> findAllBySemesterId(UUID semesterId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"course", "semester"})
    Optional<CourseOffering> findById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select offering from CourseOffering offering where offering.id = :courseOfferingId")
    Optional<CourseOffering> findByIdForUpdate(@Param("courseOfferingId") UUID courseOfferingId);
}
