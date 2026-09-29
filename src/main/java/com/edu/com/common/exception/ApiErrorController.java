package com.edu.com.common.exception;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.ServletWebRequest;

@RestController
@RequiredArgsConstructor
public class ApiErrorController implements ErrorController {
    private final ApiErrorHandler errorHandler;

    @RequestMapping("${server.error.path:${error.path:/error}}")
    public ResponseEntity<Object> error(HttpServletRequest request) {
        Object statusAttribute = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int status = statusAttribute instanceof Integer value && value >= 400 && value <= 599 ? value : 404;
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        return errorHandler.handle(exception instanceof Exception cause ? cause : null,
                HttpStatusCode.valueOf(status), HttpHeaders.EMPTY, new ServletWebRequest(request));
    }
}
