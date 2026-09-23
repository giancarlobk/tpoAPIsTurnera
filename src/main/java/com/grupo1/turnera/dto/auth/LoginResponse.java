package com.grupo1.turnera.dto.auth;

import com.grupo1.turnera.model.enums.Rol;

public record LoginResponse(
        Long id,
        String nombre,
        String apellido,
        String email,
        Rol rol,
        String token,
        String tokenType,
        long expiresIn
) {
}
