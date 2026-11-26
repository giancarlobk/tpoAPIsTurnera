package com.grupo1.turnera.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

/**
 * Cookie que guarda el JWT de la sesión del navegador.
 * <ul>
 *   <li>HttpOnly: el JavaScript de la página no puede leerla, así un ataque XSS no se lleva el token.</li>
 *   <li>SameSite=Strict: el navegador no la envía en pedidos que salen de otro sitio (mitiga CSRF).</li>
 *   <li>Secure: solo viaja por HTTPS; se puede desactivar para desarrollo con HTTP en otro host que no sea localhost.</li>
 *   <li>Path=/api y Max-Age igual al vencimiento del JWT.</li>
 * </ul>
 */
@Component
public class SesionCookie {

    private final String nombre;
    private final boolean secure;
    private final long maxAgeSegundos;

    public SesionCookie(@Value("${turnera.sesion.cookie.nombre:turnera_sesion}") String nombre,
                        @Value("${turnera.sesion.cookie.secure:true}") boolean secure,
                        JwtUtil jwtUtil) {
        this.nombre = nombre;
        this.secure = secure;
        this.maxAgeSegundos = jwtUtil.getExpirationSeconds();
    }

    public ResponseCookie crear(String token) {
        return base(token).maxAge(maxAgeSegundos).build();
    }

    public ResponseCookie borrar() {
        return base("").maxAge(0).build();
    }

    public Optional<String> leer(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> nombre.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(valor -> !valor.isBlank())
                .findFirst();
    }

    public String getNombre() {
        return nombre;
    }

    public boolean isSecure() {
        return secure;
    }

    private ResponseCookie.ResponseCookieBuilder base(String valor) {
        return ResponseCookie.from(nombre, valor)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/api");
    }
}
