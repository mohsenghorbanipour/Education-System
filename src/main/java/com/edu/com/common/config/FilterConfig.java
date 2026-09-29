package com.edu.com.common.config;

import com.edu.com.common.filter.JwtFilter;
import com.edu.com.common.filter.ApiErrorFilter;
import com.edu.com.common.exception.ApiErrorHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class FilterConfig {

    private final JwtFilter jwtFilter;

    @Autowired
    public FilterConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
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
