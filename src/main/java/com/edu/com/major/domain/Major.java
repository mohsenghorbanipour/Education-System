package com.edu.com.major.domain;

import com.edu.com.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "majors",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_majors_name",
                        columnNames = "name"
                )
        }
)
public class Major extends BaseEntity {

    @Column(
            name = "name",
            nullable = false,
            length = 150
    )
    private String name;
}
