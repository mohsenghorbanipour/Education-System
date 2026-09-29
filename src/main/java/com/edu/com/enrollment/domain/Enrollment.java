package com.edu.com.enrollment.domain;

import com.edu.com.common.domain.BaseEntity;
import com.edu.com.semester.domain.Semester;
import com.edu.com.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "enrollments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_enrollment_user_semester",
                        columnNames = {
                                "user_id",
                                "semester_id"
                        }
                )
        }
)
public class Enrollment extends BaseEntity {

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "semester_id",
            nullable = false
    )
    private Semester semester;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "plan_type",
            nullable = false,
            length = 20
    )
    @Setter
    private EnrollmentPlanType planType;

    @OneToMany(
            mappedBy = "enrollment",
            cascade = {
                    CascadeType.PERSIST,
                    CascadeType.MERGE
            },
            orphanRemoval = true
    )
    @Builder.Default
    private List<EnrollmentItem> items = new ArrayList<>();
}
