package com.edu.com.common.aspect;

import com.edu.com.common.annotations.CheckPermission;
import com.edu.com.common.filter.JwtFilter;
import com.edu.com.common.exception.ApiException;
import com.edu.com.common.exception.ErrorCode;
import com.edu.com.user.dto.response.PermissionDto;
import com.edu.com.user.dto.response.UserDto;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PermissionAspect {

    private final HttpServletRequest httpServletRequest;

    @Autowired
    public PermissionAspect(HttpServletRequest httpServletRequest) {
        this.httpServletRequest = httpServletRequest;
    }

    @Around("@annotation(permission)")
    public Object checkUserPermission(ProceedingJoinPoint joinPoint, CheckPermission permission) throws Throwable {
        UserDto userDto = (UserDto) httpServletRequest.getAttribute(JwtFilter.CURRENT_USER);
        if (userDto == null) {
            throw new ApiException(ErrorCode.AUTHENTICATION_REQUIRED);
        }
        boolean permitted = userDto.getRoles() != null && userDto.getRoles().stream()
                .filter(role -> role.getPermissions() != null)
                .flatMap(role -> role.getPermissions().stream())
                .map(PermissionDto::getName)
                .anyMatch(permission.value()::equals);
        if (!permitted) {
            throw new ApiException(ErrorCode.ACCESS_DENIED);
        }
        return joinPoint.proceed();
    }
}
