package com.edu.com.course.repository;

import com.edu.com.course.domain.CourseMajor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CourseMajorRepository extends JpaRepository<CourseMajor, UUID> {
    boolean existsByMajorId(UUID majorId);

    @EntityGraph(attributePaths = "major")
    List<CourseMajor> findAllByCourseIdIn(List<UUID> courseIds);
}
