package com.edu.com.user.repository;

import com.edu.com.user.domain.Role;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {
    @EntityGraph(attributePaths = "permissions")
    Optional<Role> findByName(String name);
}
