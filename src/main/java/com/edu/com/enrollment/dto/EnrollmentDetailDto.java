package com.edu.com.enrollment.dto;

import com.edu.com.enrollment.domain.EnrollmentPlanType;
import com.edu.com.semester.dto.SemesterDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentDetailDto {
    private UUID id;
    private EnrollmentStudentDto student;
    private SemesterDto semester;
    private EnrollmentPlanType planType;
    private List<EnrollmentItemDto> items;
    private int totalCreditUnits;
}
