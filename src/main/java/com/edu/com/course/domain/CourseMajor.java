package com.edu.com.course.domain;

import com.edu.com.common.domain.BaseEntity;
import com.edu.com.major.domain.Major;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "course_majors",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_course_major",
                        columnNames = {
                                "course_id",
                                "major_id"
                        }
                )
        }
)
public class CourseMajor extends BaseEntity {

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "course_id",
            nullable = false
    )
    private Course course;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "major_id",
            nullable = false
    )
    private Major major;
}
