package com.hu01.estudiantes.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadatos que Swagger UI muestra en {@code /swagger-ui/index.html}.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI documentacionApi() {
        return new OpenAPI().info(new Info()
                .title("API de Estudiantes - HU-01")
                .version("v1")
                .description("Microservicio RESTful CRUD de estudiantes sobre MongoDB"));
    }
}
