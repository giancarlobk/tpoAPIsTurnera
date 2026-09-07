package com.grupo1.turnera.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TurnoNoEncontradoException.class)
    public ResponseEntity<ApiErrorResponse> handleTurnoNoEncontrado(
            TurnoNoEncontradoException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(CancelacionNoPermitidaException.class)
    public ResponseEntity<ApiErrorResponse> handleCancelacionNoPermitida(
            CancelacionNoPermitidaException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, exception.getMessage(), request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(TransicionTurnoInvalidaException.class)
    public ResponseEntity<ApiErrorResponse> handleTransicionInvalida(
            TransicionTurnoInvalidaException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), request.getRequestURI(), Map.of());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            HandlerMethodValidationException.class})
    public ResponseEntity<ApiErrorResponse> handleSolicitudInvalida(HttpServletRequest request) {
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
                        TelefonoDuplicadoException.class})

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
