package com.edu.com.user.domain;

import com.edu.com.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "roles",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_roles_name",
                        columnNames = "name"
                )
        }
)
public class Role extends BaseEntity {

    @Column(
            name = "name",
            nullable = false,
            length = 100
    )
    private String name;

    @Column(
            name = "description",
            length = 500
    )
    private String description;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "role_permissions",

            joinColumns = {
                    @JoinColumn(
                            name = "role_id",
                            nullable = false
                    )
            },

            inverseJoinColumns = {
                    @JoinColumn(
                            name = "permission_id",
                            nullable = false
                    )
            }
    )
    private Set<Permission> permissions = new HashSet<>();
}
