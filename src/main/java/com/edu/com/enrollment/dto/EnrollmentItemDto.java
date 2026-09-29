package com.edu.com.enrollment.dto;

import com.edu.com.course.dto.CourseOfferingDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentItemDto {
    private UUID id;
    private CourseOfferingDto courseOffering;
}
