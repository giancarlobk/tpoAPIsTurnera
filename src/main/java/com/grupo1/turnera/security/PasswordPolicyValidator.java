package com.grupo1.turnera.security;

import com.grupo1.turnera.exception.ArgumentoInvalidoException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class PasswordPolicyValidator {

    static final int BCRYPT_MAX_BYTES = 72;
    static final String BCRYPT_LIMIT_MESSAGE = "La contraseña no puede superar 72 bytes UTF-8";

    public void validateForEncoding(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            throw new ArgumentoInvalidoException(BCRYPT_LIMIT_MESSAGE);
        }
    }
}
