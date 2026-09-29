package com.edu.com.enrollment.dto;

import com.edu.com.major.dto.MajorDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentStudentDto {
    private UUID id;
    private String universityNumber;
    private String firstName;
    private String lastName;
    private MajorDto major;
}
