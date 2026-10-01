package com.edu.com.common.config;

import com.edu.com.common.annotations.CheckPermission;
import com.edu.com.user.controller.AuthController;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI educationOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Education System API")
                        .description("Log in using /api/v1/auth/login, copy data.token, and paste it into Authorize. "
                                + "Enter the token without the Bearer prefix. Requests run against the selected server "
                                + "and can change its data. Permissions and student ownership rules still apply."))
                .servers(List.of(new Server().url("/").description("Current server")))
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    @Bean
    public OperationCustomizer educationOperationCustomizer() {
        return (operation, handlerMethod) -> {
            if (operation.getSummary() == null) {
                String summary = handlerMethod.getMethod().getName().replaceAll("([a-z])([A-Z])", "$1 $2");
                operation.setSummary(Character.toUpperCase(summary.charAt(0)) + summary.substring(1));
            }
            if (AuthController.class.isAssignableFrom(handlerMethod.getBeanType())) {
                operation.setSecurity(List.of());
            }
            CheckPermission permission = handlerMethod.getMethodAnnotation(CheckPermission.class);
            if (permission != null) {
                String description = operation.getDescription();
                operation.setDescription((description == null ? "" : description + "\n\n")
                        + "Required permission: `" + permission.value() + "`. "
                        + "Ownership and other business rules are also checked by the server.");
            }
            return operation;
        };
    }
}
