package com.grupo1.turnera.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // =========================================================
    // 400 - ERRORES DE VALIDACIÓN DE DTO
    // =========================================================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        fieldErrors.putIfAbsent(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "La solicitud contiene datos inválidos",
                request.getRequestURI(),
                fieldErrors
        );
    }


    // =========================================================
    // 400 - VALIDACIONES DE @RequestParam, @Positive, @Size, ETC.
    // =========================================================

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();

        for (ConstraintViolation<?> violation :
                exception.getConstraintViolations()) {

            String property =
                    violation.getPropertyPath().toString();

            int lastDot = property.lastIndexOf('.');

            String field = lastDot >= 0
                    ? property.substring(lastDot + 1)
                    : property;

            fieldErrors.putIfAbsent(
                    field,
                    violation.getMessage()
            );
        }

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "La solicitud contiene datos inválidos",
                request.getRequestURI(),
                fieldErrors
        );
    }


    // =========================================================
    // 400 - ARGUMENTOS / REGLAS DE NEGOCIO INVÁLIDAS
    // =========================================================

    @ExceptionHandler({
            ArgumentoInvalidoException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiErrorResponse> handleBadRequest(
            Exception exception,
            HttpServletRequest request
    ) {

        String message;

        if (exception instanceof ArgumentoInvalidoException) {
            message = exception.getMessage();
        } else {
            message = "La solicitud contiene datos inválidos";
        }

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                message,
                request.getRequestURI(),
                Map.of()
        );
    }


    // =========================================================
    // 401 - CREDENCIALES INVÁLIDAS
    // =========================================================

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


    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(
            AuthenticationException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                "Credenciales inválidas",
                request.getRequestURI(),
                Map.of()
        );
    }


    // =========================================================
    // 403 - ACCESO DENEGADO
    // =========================================================

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.FORBIDDEN,
                "No tiene permisos para esta operación",
                request.getRequestURI(),
                Map.of()
        );
    }


    // =========================================================
    // 404 - RECURSO INEXISTENTE
    // =========================================================

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ApiErrorResponse> handleRecursoNoEncontrado(
            RecursoNoEncontradoException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResourceFound(
            NoResourceFoundException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                "Recurso no encontrado",
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


    // =========================================================
    // 409 - CONFLICTOS / DUPLICADOS
    // =========================================================

    @ExceptionHandler({
            EmailDuplicadoException.class,
            DniDuplicadoException.class,
            NumAfiliadoDuplicadoException.class,
            TelefonoDuplicadoException.class,
            MatriculaDuplicadaException.class,
            EspecialidadDuplicadaException.class,
            TurnoNoDisponibleException.class
    })
    public ResponseEntity<ApiErrorResponse> handleConflict(
            RuntimeException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );
    }


    // =========================================================
    // 400 - TURNO FUERA DEL HORARIO DEL MÉDICO
    // =========================================================

    @ExceptionHandler(TurnoFueraDeHorarioException.class)
    public ResponseEntity<ApiErrorResponse> handleTurnoFueraDeHorario(
            TurnoFueraDeHorarioException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );
    }


    // =========================================================
    // 409 - CONFLICTO DE BASE DE DATOS
    // =========================================================

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.CONFLICT,
                "Los datos entran en conflicto con un registro existente",
                request.getRequestURI(),
                Map.of()
        );
    }


    // =========================================================
    // 500 - ERROR INESPERADO
    // =========================================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error interno inesperado",
                request.getRequestURI(),
                Map.of()
        );
    }


    // =========================================================
    // CONSTRUCCIÓN UNIFORME DE ApiErrorResponse
    // =========================================================

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            String message,
            String path,
            Map<String, String> fieldErrors
    ) {

        ApiErrorResponse response =
                new ApiErrorResponse(
                        Instant.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        message,
                        path,
                        fieldErrors
                );

        return ResponseEntity
                .status(status)
                .body(response);
    }
}
