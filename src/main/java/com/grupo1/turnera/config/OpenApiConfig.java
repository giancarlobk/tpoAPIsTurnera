package com.grupo1.turnera.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Set;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI turneraOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Turnero Médico API").version("v1")
                .description("Registro y login públicos. JWT Bearer para operaciones protegidas. "
                        + "PACIENTE reserva para sí mismo; MEDICO crea sobreturnos en su agenda; "
                        + "ADMIN crea sobreturnos para cualquier médico y accede a /api/admin/**. "
                        + "Las consultas de médicos, especialidades y disponibilidad son públicas.")
                .contact(new Contact().name("Grupo 1")))
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                        .description("Ingresar el token devuelto por POST /api/auth/login.")));
    }

    @Bean
    public OpenApiCustomizer permisosOpenApi() {
        return api -> api.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, operation) -> {
            boolean publico = path.startsWith("/api/auth/")
                    || (method == PathItem.HttpMethod.POST && path.equals("/api/pacientes"))
                    || (method == PathItem.HttpMethod.GET
                        && Set.of("/api/doctores", "/api/especialidades", "/api/turnos/disponibles").contains(path));
            operation.setSecurity(publico ? List.of() : List.of(new SecurityRequirement().addList("bearerAuth")));
            if (!publico) {
                operation.getResponses().addApiResponse("401", error("Token ausente, inválido o vencido"));
                operation.getResponses().addApiResponse("403", error("Rol o identidad sin permiso"));
            }
        }));
    }

    private ApiResponse error(String description) {
        return new ApiResponse().description(description).content(new Content().addMediaType("application/json",
                new MediaType().schema(new Schema<>().$ref("#/components/schemas/ApiErrorResponse"))));
    }
}
