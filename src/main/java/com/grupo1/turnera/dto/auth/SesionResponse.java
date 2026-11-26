package com.grupo1.turnera.dto.auth;

import com.grupo1.turnera.model.enums.Rol;

/**
 * Usuario de la sesión actual, sin el token. Lo usa el front para recuperar la sesión al recargar la página.
 * {@code expiresIn} son los segundos que le quedan al JWT.
 */
public record SesionResponse(
        Long id,
        String nombre,
        String apellido,
        String email,
        Rol rol,
        long expiresIn
) {
}
