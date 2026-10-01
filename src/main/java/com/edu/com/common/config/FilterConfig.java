package com.edu.com.common.config;

import com.edu.com.common.filter.JwtFilter;
import com.edu.com.common.filter.ApiErrorFilter;
import com.edu.com.common.exception.ApiErrorHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
public class FilterConfig {

    private final JwtFilter jwtFilter;

    @Autowired
    public FilterConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public FilterRegistrationBean<CorsFilter> apiCorsFilterRegistrationBean(
            @Value("${app.cors.allowed-origins:}") String allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList());
        configuration.setAllowedMethods(List.of("GET", "HEAD", "POST", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setExposedHeaders(List.of("X-Request-Id"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        FilterRegistrationBean<CorsFilter> registrationBean =
                new FilterRegistrationBean<>(new CorsFilter(source));
        registrationBean.addUrlPatterns("/api/*");
        registrationBean.setName("apiCorsFilter");
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<JwtFilter> jwtFilterRegistrationBean() {
        FilterRegistrationBean<JwtFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(jwtFilter);
        registrationBean.addUrlPatterns("/api/*");
        registrationBean.setName("jwtFilter");
        registrationBean.setOrder(1);
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<ApiErrorFilter> apiErrorFilterRegistrationBean(ApiErrorHandler errorHandler) {
        FilterRegistrationBean<ApiErrorFilter> registrationBean =
                new FilterRegistrationBean<>(new ApiErrorFilter(errorHandler));
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registrationBean;
    }

}
