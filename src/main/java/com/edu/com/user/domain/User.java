package com.edu.com.user.domain;

import com.edu.com.common.domain.BaseEntity;
import com.edu.com.major.domain.Major;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;


@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_users_university_number",
                        columnNames = "university_number"
                ),
                @UniqueConstraint(
                        name = "uk_users_national_code",
                        columnNames = "national_code"
                ),
                @UniqueConstraint(
                        name = "uk_users_mobile_number",
                        columnNames = "mobile_number"
                )
        }
)
public class User extends BaseEntity {

    @Column(
            name = "first_name",
            nullable = false,
            length = 100
    )
    private String firstName;

    @Column(
            name = "last_name",
            nullable = false,
            length = 100
    )
    private String lastName;

    @Column(
            name = "university_number",
            nullable = false,
            length = 10
    )
    private String universityNumber;

    @Column(
            name = "national_code",
            nullable = false,
            length = 10
    )
    private String nationalCode;

    @Column(
            name = "mobile_number",
            nullable = false,
            length = 11
    )
    private String mobileNumber;

    @Column(
            name = "password_hash",
            nullable = false,
            length = 500
    )
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "user_type",
            nullable = false,
            length = 30
    )
    private UserType userType;

    @Column(
            name = "enabled",
            nullable = false
    )
    private boolean enabled = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "major_id")
    private Major major;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = {
                    @JoinColumn(
                            name = "user_id",
                            nullable = false
                    )
            },
            inverseJoinColumns = {
                    @JoinColumn(
                            name = "role_id",
                            nullable = false
                    )
            },
            uniqueConstraints = {
                    @UniqueConstraint(
                            name = "uk_user_roles",
                            columnNames = {
                                    "user_id",
                                    "role_id"
                            }
                    )
            }
    )
    private Set<Role> roles = new HashSet<>();
}
