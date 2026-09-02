package com.grupo1.turnera.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI turneraOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Turnero Médico API")
                .version("v1")
                .description("Contratos HTTP disponibles para autenticación, médicos, pacientes y turnos.")
                .contact(new Contact().name("Grupo 1")));
    }
}
