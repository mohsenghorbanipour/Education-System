package com.edu.com.common.exception;

import com.edu.com.common.response.ApiFieldError;
import com.edu.com.common.response.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiErrorHandler {
    private static final String TRACE_ATTRIBUTE = ApiErrorHandler.class.getName() + ".traceId";
    private final ObjectMapper objectMapper;

    public ResponseEntity<Object> handle(Exception exception, WebRequest request) {
        exception = unwrap(exception);
        if (exception instanceof ApiException apiException) {
            return handle(exception, apiException.getErrorCode().getStatus(), HttpHeaders.EMPTY, request);
        }
        if (exception instanceof ErrorResponse errorResponse) {
            return handle(exception, errorResponse.getStatusCode(), errorResponse.getHeaders(), request);
        }
        ResponseStatus annotation = AnnotatedElementUtils.findMergedAnnotation(
                exception.getClass(), ResponseStatus.class);
        HttpStatus status = annotation != null ? annotation.code()
                : isDataConflict(exception) ? HttpStatus.CONFLICT : HttpStatus.INTERNAL_SERVER_ERROR;
        return handle(exception, status, HttpHeaders.EMPTY, request);
    }

    public ResponseEntity<Object> handle(Exception exception, HttpStatusCode status,
            HttpHeaders originalHeaders, WebRequest request) {
        if (request instanceof ServletWebRequest servletRequest
                && servletRequest.getResponse() != null && servletRequest.getResponse().isCommitted()) {
            return null;
        }

        exception = unwrap(exception);
        ErrorCode code = ErrorCode.forStatus(status.value());
        String message = code.getMessage();
        List<ApiFieldError> errors = List.of();

        if (!status.is5xxServerError()) {
            if (exception instanceof ApiException apiException) {
                code = apiException.getErrorCode();
                message = apiException.getMessage();
            } else if (status.value() == HttpStatus.CONFLICT.value() && isMajorNameConflict(exception)) {
                code = ErrorCode.MAJOR_NAME_ALREADY_EXISTS;
                message = code.getMessage();
            } else if (exception instanceof MethodArgumentNotValidException validation) {
                code = ErrorCode.VALIDATION_FAILED;
                message = code.getMessage();
                errors = validation.getBindingResult().getAllErrors().stream()
                        .map(this::fieldError).toList();
            } else if (exception instanceof HandlerMethodValidationException validation) {
                code = ErrorCode.VALIDATION_FAILED;
                message = code.getMessage();
                errors = methodValidationErrors(validation);
            }
        }

        String traceId = (String) request.getAttribute(TRACE_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
            request.setAttribute(TRACE_ATTRIBUTE, traceId, RequestAttributes.SCOPE_REQUEST);
        }
        if (status.is5xxServerError()) {
            log.error("API error [{}] {}", traceId, request.getDescription(false), exception);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.putAll(originalHeaders);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setCacheControl("no-store");
        headers.set("X-Request-Id", traceId);
        if (status.value() == HttpStatus.UNAUTHORIZED.value()
                && !headers.containsHeader(HttpHeaders.WWW_AUTHENTICATE)) {
            headers.set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        }

        ApiResponse<Void> body = ApiResponse.<Void>builder()
                .success(false)
                .code(code.name())
                .message(message)
                .errors(errors.stream().distinct()
                        .sorted(Comparator.comparing(ApiFieldError::field).thenComparing(ApiFieldError::message))
                        .toList())
                .traceId(traceId)
                .build();
        return new ResponseEntity<>(body, headers, status);
    }

    public void write(Exception exception, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        ResponseEntity<Object> error = handle(exception, new ServletWebRequest(request, response));
        if (error == null) {
            return;
        }
        response.resetBuffer();
        response.setStatus(error.getStatusCode().value());
        error.getHeaders().forEach((name, values) -> {
            response.setHeader(name, values.getFirst());
            values.stream().skip(1).forEach(value -> response.addHeader(name, value));
        });
        objectMapper.writeValue(response.getOutputStream(), error.getBody());
    }

    private ApiFieldError fieldError(ObjectError error) {
        if (error instanceof FieldError field) {
            return new ApiFieldError(field.getField(), field.isBindingFailure()
                    ? "Invalid value" : validationMessage(error.getDefaultMessage()));
        }
        return new ApiFieldError("request", validationMessage(error.getDefaultMessage()));
    }

    private List<ApiFieldError> methodValidationErrors(HandlerMethodValidationException exception) {
        List<ApiFieldError> errors = new ArrayList<>();
        exception.getParameterValidationResults().forEach(result -> {
            String name = result.getMethodParameter().getParameterName();
            String field = name != null ? name : "argument" + result.getMethodParameter().getParameterIndex();
            result.getResolvableErrors().forEach(error -> errors.add(error instanceof ObjectError objectError
                    ? fieldError(objectError)
                    : new ApiFieldError(field, validationMessage(error.getDefaultMessage()))));
        });
        exception.getCrossParameterValidationResults().forEach(error ->
                errors.add(new ApiFieldError("request", validationMessage(error.getDefaultMessage()))));
        return errors;
    }

    private String validationMessage(String message) {
        return message != null ? message : "Invalid value";
    }

    private Exception unwrap(Exception exception) {
        while (exception instanceof ServletException && exception.getCause() instanceof Exception cause) {
            exception = cause;
        }
        return exception;
    }

    private boolean isMajorNameConflict(Exception exception) {
        if (!(exception instanceof DataIntegrityViolationException)) {
            return false;
        }
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException violation
                    && "23505".equals(violation.getSQLState())
                    && "uk_majors_name".equals(violation.getConstraintName())) {
                return true;
            }
        }
        return false;
    }

    private boolean isDataConflict(Exception exception) {
        if (exception instanceof DuplicateKeyException || exception instanceof OptimisticLockingFailureException) {
            return true;
        }
        if (exception instanceof DataIntegrityViolationException) {
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof SQLException sql
                        && ("23505".equals(sql.getSQLState()) || "23503".equals(sql.getSQLState()))) {
                    return true;
                }
            }
        }
        return false;
    }
}
