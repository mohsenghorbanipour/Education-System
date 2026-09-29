package com.edu.com.user.dto.response;

import com.edu.com.major.dto.MajorDto;
import com.edu.com.user.domain.UserType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private UUID id;
    private String universityNumber;
    private String firstName;
    private String lastName;
    private UserType userType;
    private Set<RoleDto> roles;
    private String token;
    private MajorDto major;
}
