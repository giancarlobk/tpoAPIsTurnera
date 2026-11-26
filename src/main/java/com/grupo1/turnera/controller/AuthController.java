package com.grupo1.turnera.controller;

import com.grupo1.turnera.dto.auth.LoginRequest;
import com.grupo1.turnera.dto.paciente.PacienteCreateRequest;
import com.grupo1.turnera.dto.paciente.PacienteResponse;
import org.springframework.http.HttpStatus;
import com.grupo1.turnera.dto.auth.LoginResponse;
import com.grupo1.turnera.exception.ApiErrorResponse;
import com.grupo1.turnera.dto.auth.SesionResponse;
import com.grupo1.turnera.model.BaseUsuario;
import com.grupo1.turnera.security.SesionCookie;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import java.time.Instant;
import com.grupo1.turnera.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Inicio de sesión de usuarios")
public class AuthController {

    private final AuthenticationService authService;
    private final SesionCookie sesionCookie;

    @PostMapping("/register")
    @Operation(summary = "Registrar usuario", description = "Registro público de pacientes con BCrypt; rol PACIENTE asignado por el servidor.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Paciente registrado",
                    content = @Content(schema = @Schema(implementation = PacienteResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Datos ya registrados",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<PacienteResponse> register(@Valid @RequestBody PacienteCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Valida mediante AuthenticationManager y devuelve un JWT firmado. "
            + "El navegador lo recibe en una cookie HttpOnly (SameSite=Strict, Path=/api); "
            + "otros clientes pueden usar el token del cuerpo con Authorization: Bearer <token>.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credenciales válidas",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Credenciales inválidas",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest,
                                               HttpServletResponse httpResponse) {
        LoginResponse response = authService.login(request);
        // Se agrega a la respuesta del servlet (no al ResponseEntity) para no pisar la cookie XSRF-TOKEN.
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, sesionCookie.crear(response.token()).toString());
        emitirTokenCsrf(httpRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Sesión actual",
            description = "Devuelve el usuario autenticado por la cookie de sesión (o el header Bearer), sin el token. "
                    + "El front lo usa para recuperar la sesión al recargar la página.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sesión válida",
                    content = @Content(schema = @Schema(implementation = SesionResponse.class))),
            @ApiResponse(responseCode = "401", description = "No hay sesión o venció",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<SesionResponse> sesionActual(@AuthenticationPrincipal BaseUsuario actor,
                                                       Authentication authentication,
                                                       HttpServletRequest httpRequest) {
        emitirTokenCsrf(httpRequest);
        return ResponseEntity.ok(authService.sesionActual(actor, (Instant) authentication.getDetails()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión", description = "Borra la cookie de sesión del navegador. Responde 204 aunque no haya sesión.")
    @ApiResponse(responseCode = "204", description = "Cookie de sesión borrada")
    public ResponseEntity<Void> logout(HttpServletResponse httpResponse) {
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, sesionCookie.borrar().toString());
        return ResponseEntity.noContent().build();
    }

    /**
     * El token CSRF se genera recién cuando alguien lo pide; al pedirlo acá, la respuesta trae la cookie XSRF-TOKEN
     * que el front devuelve en el header X-XSRF-TOKEN de los pedidos que modifican datos.
     */
    private static void emitirTokenCsrf(HttpServletRequest request) {
        if (request.getAttribute(CsrfToken.class.getName()) instanceof CsrfToken token) {
            token.getToken();
        }
    }
}
