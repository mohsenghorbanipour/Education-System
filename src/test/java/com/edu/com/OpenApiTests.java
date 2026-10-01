package com.edu.com;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OpenApiTests {

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private ObjectMapper mapper;

    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @Test
    void documentsBusinessEndpointsWithBearerSecurityAndPublicLogin() throws Exception {
        HttpResponse<String> response = get("/v3/api-docs");
        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode document = mapper.readTree(response.body());
        assertThat(document.at("/servers/0/url").asText()).isEqualTo("/");
        assertThat(document.at("/components/securitySchemes/bearerAuth/scheme").asText()).isEqualTo("bearer");
        assertThat(document.at("/security/0/bearerAuth").isArray()).isTrue();
        JsonNode paths = document.path("paths");
        assertThat(paths.path("/api/v1/auth/login").path("post").path("security").isArray()).isTrue();
        assertThat(paths.path("/api/v1/auth/login").path("post").path("security").size()).isZero();
        for (String resource : new String[]{"users", "majors", "courses", "semesters", "course-offerings", "enrollments"}) {
            assertThat(paths.has("/api/v1/" + resource)).isTrue();
        }
        assertThat(paths.path("/api/v1/courses").path("get").path("description").asText()).contains("COURSE_READ");
        assertThat(paths.has("/actuator/health")).isFalse();
        assertThat(paths.has("/error")).isFalse();
        assertThat(paths.toString()).doesNotContain("CURRENT_USER", "\"currentUser\"");
        JsonNode login = document.at("/components/schemas/LoginRequest");
        assertThat(login.path("required").toString()).contains("universityNumber", "password");
        assertThat(login.at("/properties/universityNumber/pattern").asText()).isEqualTo("\\d{10}");
    }

    @Test
    void servesUiAssetsAndConfigurationWithoutAuthentication() throws Exception {
        assertThat(get("/swagger-ui.html").statusCode()).isBetween(300, 399);
        HttpResponse<String> ui = get("/swagger-ui/index.html");
        assertThat(ui.statusCode()).isEqualTo(200);
        assertThat(ui.body()).contains("swagger-ui");
        HttpResponse<String> initializer = get("/swagger-ui/swagger-initializer.js");
        assertThat(initializer.statusCode()).isEqualTo(200);
        assertThat(initializer.body()).contains("/v3/api-docs/swagger-config");
        JsonNode config = mapper.readTree(get("/v3/api-docs/swagger-config").body());
        assertThat(config.path("url").asText()).isEqualTo("/v3/api-docs");
    }

    @Test
    void documentationDoesNotBypassApiAuthentication() throws Exception {
        HttpResponse<String> response = get("/api/v1/courses");
        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(mapper.readTree(response.body()).path("code").asText()).isEqualTo("AUTHENTICATION_REQUIRED");
        HttpResponse<String> login = client.send(HttpRequest.newBuilder(uri("/api/v1/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .timeout(Duration.ofSeconds(20)).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(login.statusCode()).isEqualTo(400);
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(20)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }
}
