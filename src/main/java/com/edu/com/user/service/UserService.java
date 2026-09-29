package com.edu.com.user.service;

import com.edu.com.common.exception.ApiException;
import com.edu.com.enrollment.service.EnrollmentConsistencyLock;
import com.edu.com.enrollment.service.EnrollmentIntegrityService;
import com.edu.com.common.exception.ErrorCode;
import com.edu.com.common.util.HashUtil;
import com.edu.com.common.util.JwtUtil;
import com.edu.com.enrollment.repository.EnrollmentRepository;
import com.edu.com.major.domain.Major;
import com.edu.com.major.repository.MajorRepository;
import com.edu.com.user.domain.Role;
import com.edu.com.user.domain.User;
import com.edu.com.user.domain.UserType;
import com.edu.com.user.dto.request.LoginRequest;
import com.edu.com.user.dto.request.RegisterStudentRequest;
import com.edu.com.user.dto.request.UpdateProfileRequest;
import com.edu.com.user.dto.request.UpdateUserRequest;
import com.edu.com.user.dto.response.UserDto;
import com.edu.com.user.repository.UserRepository;
import com.edu.com.user.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;
    private final MajorRepository majorRepository;
    private final RoleRepository roleRepository;
    private final EnrollmentRepository enrollmentRepository;

    private final JwtUtil jwtUtil;

    private final EnrollmentConsistencyLock consistencyLock;
    private final EnrollmentIntegrityService integrityService;

    private final ModelMapper mapper;

    public UserDto getUser(LoginRequest request) {
        User user = repository
                .findByUniversityNumber(
                        request.universityNumber()
                )
                .orElseThrow(
                        () -> new ApiException(ErrorCode.INVALID_CREDENTIALS)
                );

        String requestPasswordHash =
                HashUtil.md5(request.password());

        if (!requestPasswordHash.equals(user.getPasswordHash())) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }

        if (!user.isEnabled()) {
            throw new ApiException(ErrorCode.ACCOUNT_DISABLED);
        }
        UserDto dto = mapper.map(
                user,
                UserDto.class
        );

        dto.setToken(jwtUtil.generateToken(user.getId()));

        return dto;
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(UUID userId) {
        User user = repository
                .findById(userId)
                .orElseThrow(
                        () -> new ApiException(ErrorCode.INVALID_TOKEN)
                );

        if (!user.isEnabled()) {
            throw new ApiException(ErrorCode.ACCOUNT_DISABLED);
        }

        return mapper.map(
                user,
                UserDto.class
        );
    }

    @Transactional
    public UserDto registerStudent(RegisterStudentRequest request) {
        if (repository.existsByUniversityNumber(request.universityNumber())) {
            throw new ApiException(ErrorCode.UNIVERSITY_NUMBER_ALREADY_EXISTS);
        }
        if (repository.existsByNationalCode(request.nationalCode())) {
            throw new ApiException(ErrorCode.NATIONAL_CODE_ALREADY_EXISTS);
        }
        if (repository.existsByMobileNumber(request.mobileNumber())) {
            throw new ApiException(ErrorCode.MOBILE_NUMBER_ALREADY_EXISTS);
        }

        Major major = majorRepository.findById(request.majorId())
                .orElseThrow(() -> new ApiException(ErrorCode.MAJOR_NOT_FOUND));
        Role studentRole = roleRepository.findByName("STUDENT")
                .orElseThrow(() -> new IllegalStateException("The default STUDENT role is not configured"));

        Set<Role> roles = new HashSet<>();
        roles.add(studentRole);
        return mapper.map(repository.saveAndFlush(
                User.builder()
                        .firstName(request.firstName())
                        .lastName(request.lastName())
                        .universityNumber(request.universityNumber())
                        .nationalCode(request.nationalCode())
                        .mobileNumber(request.mobileNumber())
                        .passwordHash(HashUtil.md5(request.password()))
                        .major(major)
                        .roles(roles)
                        .userType(UserType.STUDENT)
                        .enabled(true)
                        .build()
        ), UserDto.class);
    }

    @Transactional(readOnly = true)
    public List<UserDto> getUsers(Integer page, Integer size) {
        return repository.findAll(Pageable.ofSize(size).withPage(page))
                .stream().map(user -> mapper.map(user, UserDto.class)
        ).toList();
    }

    @Transactional(readOnly = true)
    public UserDto getUserDetails(UUID userId) {
        User user = repository.findDetailedById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        return mapper.map(user, UserDto.class);
    }

    @Transactional
    public UserDto updateOwnUser(
            UUID currentUserId,
            UpdateProfileRequest request
    ) {
        User user = repository.findById(currentUserId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        applyProfileChanges(user, request.firstName(), request.lastName(),
                request.nationalCode(), request.mobileNumber());

        return mapper.map(repository.saveAndFlush(user), UserDto.class);
    }

    @Transactional
    public UserDto updateUser(UUID userId, UpdateUserRequest request) {
        consistencyLock.forCatalogChange();
        User user = repository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        if (request.universityNumber() != null
                && repository.existsByUniversityNumberAndIdNot(request.universityNumber(), userId)) {
            throw new ApiException(ErrorCode.UNIVERSITY_NUMBER_ALREADY_EXISTS);
        }

        Major major = request.majorId() == null ? null
                : majorRepository.findById(request.majorId())
                        .orElseThrow(() -> new ApiException(ErrorCode.MAJOR_NOT_FOUND));

        applyProfileChanges(user, request.firstName(), request.lastName(),
                request.nationalCode(), request.mobileNumber());

        if (request.universityNumber() != null) {
            user.setUniversityNumber(request.universityNumber());
        }

        if (major != null) {
            user.setMajor(major);
        }

        user = repository.saveAndFlush(user);
        if (major != null) {
            integrityService.validateStudentChange(userId);
        }
        return mapper.map(user, UserDto.class);
    }

    @Transactional
    public void deleteUser(UUID userId) {
        User user = repository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        if (enrollmentRepository.existsByUserId(userId)) {
            throw new ApiException(ErrorCode.USER_IN_USE);
        }

        try {
            repository.delete(user);
            repository.flush();
        } catch (DataIntegrityViolationException exception) {
            if (isUserReferenced(exception)) {
                throw new ApiException(ErrorCode.USER_IN_USE);
            }
            throw exception;
        }
    }

    private boolean isUserReferenced(DataIntegrityViolationException exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException violation
                    && "23503".equals(violation.getSQLState())
                    && "fk_enrollments_user".equals(violation.getConstraintName())) {
                return true;
            }
        }
        return false;
    }

    private void applyProfileChanges(User user, String firstName, String lastName,
            String nationalCode, String mobileNumber) {
        if (nationalCode != null
                && repository.existsByNationalCodeAndIdNot(nationalCode, user.getId())) {
            throw new ApiException(ErrorCode.NATIONAL_CODE_ALREADY_EXISTS);
        }

        if (mobileNumber != null
                && repository.existsByMobileNumberAndIdNot(mobileNumber, user.getId())) {
            throw new ApiException(ErrorCode.MOBILE_NUMBER_ALREADY_EXISTS);
        }

        if (firstName != null) {
            user.setFirstName(firstName.strip());
        }

        if (lastName != null) {
            user.setLastName(lastName.strip());
        }

        if (nationalCode != null) {
            user.setNationalCode(nationalCode);
        }

        if (mobileNumber != null) {
            user.setMobileNumber(mobileNumber);
        }
    }

}
