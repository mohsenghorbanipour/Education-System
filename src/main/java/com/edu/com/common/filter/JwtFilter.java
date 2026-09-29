package com.edu.com.common.filter;

import com.edu.com.common.util.JwtUtil;
import com.edu.com.common.exception.ApiException;
import com.edu.com.common.exception.ErrorCode;
import com.edu.com.user.service.UserService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
public class JwtFilter implements Filter {

    public static final String CURRENT_USER = "CURRENT_USER";
    private static final String PUBLIC_USER_API_PATH = "/api/v1/auth";

    private final JwtUtil jwtUtil;
    private final UserService userService;

    @Autowired
    public JwtFilter(JwtUtil jwtUtil, UserService userService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;

        if (isPublicUserApi(httpServletRequest)) {
            chain.doFilter(request, response);
            return;
        }

        String token = extractTokenFromHeader(httpServletRequest);
        if (token == null || token.isBlank()) {
            throw new ApiException(ErrorCode.AUTHENTICATION_REQUIRED);
        }
        UUID userId;
        try {
            userId = jwtUtil.getUserIdFromToken(token);
        } catch (ExpiredJwtException exception) {
            throw new ApiException(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.INVALID_TOKEN);
        }
        httpServletRequest.setAttribute(CURRENT_USER, userService.getUserById(userId));
        chain.doFilter(request, response);
    }

    private boolean isPublicUserApi(HttpServletRequest request) {
        String requestPath = request.getRequestURI().substring(request.getContextPath().length());
        return requestPath.equals(PUBLIC_USER_API_PATH)
                || requestPath.startsWith(PUBLIC_USER_API_PATH + "/");
    }

    public String extractTokenFromHeader(HttpServletRequest httpServletRequest) {
        String authorization = httpServletRequest.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }
}
