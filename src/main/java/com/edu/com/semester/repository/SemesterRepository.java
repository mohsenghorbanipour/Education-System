package com.edu.com.semester.repository;

import com.edu.com.semester.domain.Semester;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SemesterRepository extends JpaRepository<Semester, UUID> {
    boolean existsByTitle(String title);

    boolean existsByTitleAndIdNot(String title, UUID id);

    List<Semester> findAllBy(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Semester s where s.id = :semesterId")
    Optional<Semester> findByIdForUpdate(@Param("semesterId") UUID semesterId);
}
