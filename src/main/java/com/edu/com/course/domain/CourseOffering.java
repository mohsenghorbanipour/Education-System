package com.edu.com.course.domain;

import com.edu.com.common.domain.BaseEntity;
import com.edu.com.semester.domain.Semester;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(
        name = "course_offerings",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_course_offering_semester_course",
                        columnNames = {
                                "semester_id",
                                "course_id"
                        }
                )
        }
)
public class CourseOffering extends BaseEntity {

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
            name = "semester_id",
            nullable = false
    )
    private Semester semester;

    @Column(
            name = "guest_allowed",
            nullable = false
    )
    private boolean guestAllowed;

    @Column(nullable = false)
    private int capacity;
}
