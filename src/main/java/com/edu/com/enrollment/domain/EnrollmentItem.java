package com.edu.com.enrollment.domain;

import com.edu.com.common.domain.BaseEntity;
import com.edu.com.course.domain.CourseOffering;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "enrollment_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_enrollment_item_offering",
                        columnNames = {
                                "enrollment_id",
                                "course_offering_id"
                        }
                )
        }
)
public class EnrollmentItem extends BaseEntity {

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "enrollment_id",
            nullable = false
    )
    private Enrollment enrollment;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "course_offering_id",
            nullable = false
    )
    private CourseOffering courseOffering;
}
