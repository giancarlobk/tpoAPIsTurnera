package com.grupo1.turnera.controller;

import com.grupo1.turnera.dto.carrito.AgregarItemCarritoRequest;
import com.grupo1.turnera.dto.carrito.CarritoResponse;
import com.grupo1.turnera.exception.ApiErrorResponse;
import com.grupo1.turnera.model.BaseUsuario;
import com.grupo1.turnera.service.CarritoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/carrito")
@RequiredArgsConstructor
@Tag(name = "Carrito", description = "Turnos que el paciente junta antes de reservar")
@ApiResponse(responseCode = "401", description = "Falta un JWT válido",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
@ApiResponse(responseCode = "403", description = "El rol no está autorizado o el carrito es de otro paciente",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
public class CarritoController {

    private final CarritoService carritoService;

    @GetMapping
    @ApiResponse(responseCode = "200", description = "Carritos visibles para el usuario",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = CarritoResponse.class))))
    @Operation(summary = "Listar carritos",
            description = "PACIENTE: una lista con su carrito (vacía si todavía no agregó turnos). ADMIN: todos.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<CarritoResponse>> obtenerCarritos(@AuthenticationPrincipal BaseUsuario actor) {
        return ResponseEntity.ok(carritoService.obtenerCarritos(actor));
    }

    @GetMapping("/{carritoId}")
    @ApiResponse(responseCode = "200", description = "Carrito con sus turnos",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CarritoResponse.class)))
    @ApiResponse(responseCode = "404", description = "Carrito no encontrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    @Operation(summary = "Obtener un carrito",
            description = "PACIENTE: solo el propio. ADMIN: cualquiera.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<CarritoResponse> obtenerCarrito(@PathVariable Long carritoId,
            @AuthenticationPrincipal BaseUsuario actor) {
        return ResponseEntity.ok(carritoService.obtenerCarrito(carritoId, actor));
    }

    @PostMapping("/items")
    @ApiResponse(responseCode = "201", description = "Turno agregado; devuelve el carrito actualizado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CarritoResponse.class)))
    @ApiResponse(responseCode = "400", description = "Datos inválidos o horario fuera de la agenda del médico",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Médico activo no encontrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "El horario ya está ocupado o el turno ya está en el carrito",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    @Operation(summary = "Agregar un turno al carrito",
            description = "PACIENTE: el carrito es el del usuario autenticado y se crea con el primer turno. "
                    + "Aplica las mismas reglas que la reserva, pero no ocupa el horario.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<CarritoResponse> agregarItem(@Valid @RequestBody AgregarItemCarritoRequest request,
            @AuthenticationPrincipal BaseUsuario actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(carritoService.agregarItem(request, actor));
    }

    @DeleteMapping("/items/{itemId}")
    @ApiResponse(responseCode = "200", description = "Turno quitado; devuelve el carrito actualizado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CarritoResponse.class)))
    @ApiResponse(responseCode = "404", description = "Ítem no encontrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    @Operation(summary = "Quitar un turno del carrito",
            description = "PACIENTE: solo de su carrito. ADMIN: de cualquiera.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<CarritoResponse> quitarItem(@PathVariable Long itemId,
            @AuthenticationPrincipal BaseUsuario actor) {
        return ResponseEntity.ok(carritoService.quitarItem(itemId, actor));
    }
}
