package com.grupo1.turnera.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler =
            new GlobalExceptionHandler();

    @Test
    void deberiaSanitizarErroresInternos() {

        HttpServletRequest request =
                mock(HttpServletRequest.class);

        when(request.getRequestURI())
                .thenReturn("/api/prueba");

        RuntimeException exception =
                new RuntimeException(
                        "SELECT * FROM usuarios password=secreto"
                );

        ResponseEntity<ApiErrorResponse> response =
                handler.handleUnexpected(
                        exception,
                        request
                );

        assertThat(response.getStatusCode())
                .isEqualTo(
                        HttpStatus.INTERNAL_SERVER_ERROR
                );

        ApiErrorResponse body =
                response.getBody();

        assertThat(body).isNotNull();

        assertThat(body.status())
                .isEqualTo(500);

        assertThat(body.message())
                .isEqualTo(
                        "Ocurrió un error interno inesperado"
                );

        assertThat(body.path())
                .isEqualTo("/api/prueba");

        assertThat(body.message())
                .doesNotContain(
                        "SELECT",
                        "usuarios",
                        "password",
                        "secreto"
                );
    }
}
