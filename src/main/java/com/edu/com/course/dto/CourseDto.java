package com.edu.com.course.dto;

import com.edu.com.major.dto.MajorDto;
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
public class CourseDto {
    private UUID id;
    private String name;
    private String code;
    private Integer creditUnits;
    private List<MajorDto> majors;
}
