package com.edu.com.common.filter;

import com.edu.com.common.exception.ApiErrorHandler;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class ApiErrorFilter extends OncePerRequestFilter {
    private final ApiErrorHandler errorHandler;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        try {
            chain.doFilter(request, response);
        } catch (Exception exception) {
            if (response.isCommitted()) {
                throw exception;
            }
            errorHandler.write(exception, request, response);
        }
    }
}
