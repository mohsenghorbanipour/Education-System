package com.edu.com.course.dto;

import com.edu.com.semester.dto.SemesterDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CourseOfferingDto {
    private UUID id;
    private CourseDto course;
    private SemesterDto semester;
    private boolean guestAllowed;
    private int capacity;
    private long enrolledCount;
    private long remainingCapacity;
}
