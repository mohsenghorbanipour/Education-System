package com.edu.com.common.config;

import com.edu.com.common.exception.ApiErrorHandler;
import com.edu.com.common.filter.JwtFilter;
import com.edu.com.common.util.JwtUtil;
import com.edu.com.user.dto.response.UserDto;
import com.edu.com.user.service.UserService;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FilterConfigTests {

    private final JwtUtil jwtUtil = mock(JwtUtil.class);
    private final UserService userService = mock(UserService.class);
    private final FilterConfig configuration = new FilterConfig(new JwtFilter(jwtUtil, userService));

    @Test
    void allowedPreflightCompletesBeforeAuthentication() throws Exception {
        MockHttpServletRequest request = preflight("http://localhost:5173", "PATCH", "authorization,content-type");
        MockHttpServletResponse response = execute(request, "http://localhost:5173");

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo("http://localhost:5173");
        assertThat(response.getHeader("Access-Control-Allow-Methods")).contains("PATCH");
        assertThat(response.getHeader("Access-Control-Allow-Headers")).containsIgnoringCase("authorization");
        assertThat(response.getHeader("Access-Control-Allow-Credentials")).isNull();
        verifyNoInteractions(jwtUtil, userService);
    }

    @Test
    void authenticationFailureIsReadableByAllowedFrontend() throws Exception {
        MockHttpServletRequest request = request("GET", "http://localhost:5173");
        MockHttpServletResponse response = execute(request, "http://localhost:5173");

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo("http://localhost:5173");
        assertThat(response.getHeader("Access-Control-Expose-Headers")).contains("X-Request-Id");
        assertThat(response.getContentAsString()).contains("AUTHENTICATION_REQUIRED");
        verifyNoInteractions(jwtUtil, userService);
    }

    @Test
    void bearerTokenStillAuthenticatesActualRequest() throws Exception {
        UUID userId = UUID.randomUUID();
        UserDto user = mock(UserDto.class);
        when(jwtUtil.getUserIdFromToken("test-token")).thenReturn(userId);
        when(userService.getUserById(userId)).thenReturn(user);
        MockHttpServletRequest request = request("GET", "http://localhost:5173");
        request.addHeader("Authorization", "Bearer test-token");

        MockHttpServletResponse response = execute(request, "http://localhost:5173");

        assertThat(response.getStatus()).isEqualTo(204);
        assertThat(request.getAttribute(JwtFilter.CURRENT_USER)).isSameAs(user);
        verify(userService).getUserById(userId);
    }

    @Test
    void differentOriginCannotPassPreflight() throws Exception {
        MockHttpServletResponse response = execute(
                preflight("http://localhost:5174", "GET", "authorization"), "http://localhost:5173");

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isNull();
        verifyNoInteractions(jwtUtil, userService);
    }

    @Test
    void unsupportedHeaderCannotPassPreflight() throws Exception {
        MockHttpServletResponse response = execute(
                preflight("http://localhost:5173", "GET", "x-unapproved-header"), "http://localhost:5173");

        assertThat(response.getStatus()).isEqualTo(403);
        verifyNoInteractions(jwtUtil, userService);
    }

    @Test
    void commaSeparatedOriginsAllowAnotherExplicitFrontend() throws Exception {
        MockHttpServletResponse response = execute(
                preflight("https://app.example.test", "POST", "content-type"),
                " http://localhost:5173, https://app.example.test ");

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo("https://app.example.test");
    }

    @Test
    void emptyOriginsDisableCrossOriginRequests() throws Exception {
        MockHttpServletResponse response = execute(
                preflight("http://localhost:5173", "GET", "authorization"), "");

        assertThat(response.getStatus()).isEqualTo(403);
        verifyNoInteractions(jwtUtil, userService);
    }

    @Test
    void requestsWithoutOriginStillRequireAuthentication() throws Exception {
        MockHttpServletResponse response = execute(request("GET", null), "http://localhost:5173");

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("AUTHENTICATION_REQUIRED");
    }

    private MockHttpServletRequest preflight(String origin, String method, String headers) {
        MockHttpServletRequest request = request("OPTIONS", origin);
        request.addHeader("Access-Control-Request-Method", method);
        request.addHeader("Access-Control-Request-Headers", headers);
        return request;
    }

    private MockHttpServletRequest request(String method, String origin) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/api/v1/courses");
        request.setScheme("https");
        request.setServerName("api.mohsendev20.ir");
        request.setServerPort(443);
        if (origin != null) {
            request.addHeader("Origin", origin);
        }
        return request;
    }

    private MockHttpServletResponse execute(MockHttpServletRequest request, String origins) throws Exception {
        List<FilterRegistrationBean<?>> registrations = List.of(
                configuration.jwtFilterRegistrationBean(),
                configuration.apiCorsFilterRegistrationBean(origins),
                configuration.apiErrorFilterRegistrationBean(new ApiErrorHandler(new ObjectMapper())));
        Filter[] filters = registrations.stream()
                .sorted(Comparator.comparingInt(FilterRegistrationBean::getOrder))
                .map(FilterRegistrationBean::getFilter)
                .toArray(Filter[]::new);
        HttpServlet endpoint = new HttpServlet() {
            @Override
            protected void service(HttpServletRequest request, HttpServletResponse response) {
                response.setStatus(204);
            }
        };
        MockHttpServletResponse response = new MockHttpServletResponse();
        new MockFilterChain(endpoint, filters).doFilter(request, response);
        return response;
    }
}
