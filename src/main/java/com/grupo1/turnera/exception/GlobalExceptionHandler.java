package com.grupo1.turnera.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, "No tiene permisos para esta operación",
                request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(HttpServletRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Email o contraseña incorrectos",
                request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatus(
            ResponseStatusException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.valueOf(exception.getStatusCode().value()), exception.getReason(),
                request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleIntegrity(HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, "Los datos entran en conflicto con un registro existente",
                request.getRequestURI(), Map.of());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiErrorResponse> handleMalformedRequest(HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "La solicitud contiene datos inválidos",
                request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ApiErrorResponse> handleCredencialesInvalidas(
            CredencialesInvalidasException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage())
        );

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "La solicitud contiene datos inválidos",
                request.getRequestURI(),
                fieldErrors
        );
    }

    // Metodo errores de datos Duplicados
    @ExceptionHandler({EmailDuplicadoException.class,
                        DniDuplicadoException.class,
                        NumAfiliadoDuplicadoException.class,
                        TelefonoDuplicadoException.class,
                        MatriculaDuplicadaException.class,
                        EspecialidadDuplicadaException.class})

    public ResponseEntity<ApiErrorResponse> handleDuplicado(
        RuntimeException exception,
        HttpServletRequest request
    ){
        return buildResponse(
                HttpStatus.CONFLICT, // 409
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );
    }
    // Metodo errores de recurso no encontrado
     @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ApiErrorResponse> handleRecursoNoEncontrado(
            RecursoNoEncontradoException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.NOT_FOUND, // 404
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );
    }
    // Metodo errores de turno fuera de horario
        @ExceptionHandler(TurnoNoDisponibleException.class)
    public ResponseEntity<ApiErrorResponse> handleTurnoNoDisponible(
            TurnoNoDisponibleException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.CONFLICT, // 409
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );
    }
    // Metodo errores de turno fuera de horario
        @ExceptionHandler(TurnoFueraDeHorarioException.class)
    public ResponseEntity<ApiErrorResponse> handleTurnoFueraDeHorario(
            TurnoFueraDeHorarioException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST, // 400
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );
    }

        @ExceptionHandler(TransicionEstadoTurnoInvalidaException.class)
        public ResponseEntity<ApiErrorResponse> handleTransicionInvalida(
                        TransicionEstadoTurnoInvalidaException exception,
                        HttpServletRequest request) {
                return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), request.getRequestURI(), Map.of());
        }


    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            String message,
            String path,
            Map<String, String> fieldErrors
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                fieldErrors
        );
        return ResponseEntity.status(status).body(response);
    }
}
