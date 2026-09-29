package com.edu.com.user.repository;

import com.edu.com.user.domain.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    @EntityGraph(
            attributePaths = {
                    "roles",
                    "roles.permissions",
                    "major"
            }
    )
    Optional<User> findByUniversityNumber(String universityNumber);

    boolean existsByUniversityNumber(String universityNumber);

    boolean existsByUniversityNumberAndIdNot(String universityNumber, UUID id);

    boolean existsByNationalCode(String nationalCode);

    boolean existsByMobileNumber(String mobileNumber);

    boolean existsByNationalCodeAndIdNot(String nationalCode, UUID id);

    boolean existsByMobileNumberAndIdNot(String mobileNumber, UUID id);

    boolean existsByMajorId(UUID majorId);

    @EntityGraph(attributePaths = {"roles", "roles.permissions", "major"})
    Optional<User> findDetailedById(UUID id);
}
