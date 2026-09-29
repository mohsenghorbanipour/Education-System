package com.edu.com.semester.domain;

import com.edu.com.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "semesters",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_semesters_title",
                        columnNames = "title"
                )
        }
)
public class Semester extends BaseEntity {

    @Column(
            name = "title",
            nullable = false,
            length = 10
    )
    private String title;

    @Column(
            name = "start_date",
            nullable = false
    )
    private LocalDate startDate;

    @Column(
            name = "end_date",
            nullable = false
    )
    private LocalDate endDate;
}
