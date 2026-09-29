package com.edu.com.course.domain;

import com.edu.com.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "courses",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_courses_code",
                        columnNames = "code"
                )
        }
)
public class Course extends BaseEntity {

    @Column(
            name = "name",
            nullable = false,
            length = 150
    )
    private String name;

    @Column(
            name = "code",
            nullable = false,
            length = 20
    )
    private String code;

    @Column(
            name = "credit_units",
            nullable = false
    )
    private Integer creditUnits;
}
