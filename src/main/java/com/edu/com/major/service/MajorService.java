package com.edu.com.major.service;

import com.edu.com.common.exception.ApiException;
import com.edu.com.common.exception.ErrorCode;
import com.edu.com.course.repository.CourseMajorRepository;
import com.edu.com.major.domain.Major;
import com.edu.com.major.dto.MajorDto;
import com.edu.com.major.dto.request.CreateMajorRequest;
import com.edu.com.major.dto.request.UpdateMajorRequest;
import com.edu.com.major.repository.MajorRepository;
import com.edu.com.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MajorService {

    private final MajorRepository repository;
    private final UserRepository userRepository;
    private final CourseMajorRepository courseMajorRepository;

    private final ModelMapper mapper;

    public List<MajorDto> getMajors(Integer page, Integer size) {
        return repository.findAll(PageRequest.of(page, size, Sort.by("name", "id")))
                .map(major -> mapper.map(major, MajorDto.class))
                .getContent();
    }

    public MajorDto createMajor(CreateMajorRequest request) {
        String name = request.name().strip();

        if (repository.existsByName(name)) {
            throw new ApiException(ErrorCode.MAJOR_NAME_ALREADY_EXISTS);
        }

        return mapper.map(
                repository.saveAndFlush(new Major(name)),
                MajorDto.class);
    }

    public MajorDto getMajor(UUID majorId) {
        return mapper.map(findMajor(majorId), MajorDto.class);
    }

    public MajorDto updateMajor(UUID majorId, UpdateMajorRequest request) {
        Major major = findMajor(majorId);
        String name = request.name().strip();

        if (repository.existsByNameAndIdNot(name, majorId)) {
            throw new ApiException(ErrorCode.MAJOR_NAME_ALREADY_EXISTS);
        }

        major.setName(name);
        return mapper.map(repository.saveAndFlush(major), MajorDto.class);
    }

    @Transactional
    public void deleteMajor(UUID majorId) {
        Major major = findMajor(majorId);

        if (userRepository.existsByMajorId(majorId)
                || courseMajorRepository.existsByMajorId(majorId)) {
            throw new ApiException(ErrorCode.MAJOR_IN_USE);
        }

        try {
            repository.delete(major);
            repository.flush();
        } catch (DataIntegrityViolationException exception) {
            if (isMajorReferenced(exception)) {
                throw new ApiException(ErrorCode.MAJOR_IN_USE);
            }
            throw exception;
        }
    }

    private Major findMajor(UUID majorId) {
        return repository.findById(majorId)
                .orElseThrow(() -> new ApiException(ErrorCode.MAJOR_NOT_FOUND));
    }

    private boolean isMajorReferenced(DataIntegrityViolationException exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException violation
                    && "23503".equals(violation.getSQLState())
                    && ("fk_users_major".equals(violation.getConstraintName())
                    || "fk_course_majors_major".equals(violation.getConstraintName()))) {
                return true;
            }
        }
        return false;
    }
}
