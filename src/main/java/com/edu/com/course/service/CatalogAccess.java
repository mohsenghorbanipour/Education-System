package com.edu.com.course.service;

import com.edu.com.common.exception.ApiException;
import com.edu.com.common.exception.ErrorCode;
import com.edu.com.user.domain.UserType;
import com.edu.com.user.dto.response.UserDto;

public final class CatalogAccess {
    private CatalogAccess() {
    }

    public static void requireUnrestrictedAccess(UserDto currentUser) {
        if (currentUser == null) {
            throw new ApiException(ErrorCode.AUTHENTICATION_REQUIRED);
        }
        if (currentUser.getUserType() == null || currentUser.getUserType() == UserType.STUDENT) {
            throw new ApiException(ErrorCode.ACCESS_DENIED,
                    "Students must use their enrollment's available-course-offerings endpoint");
        }
    }
}
