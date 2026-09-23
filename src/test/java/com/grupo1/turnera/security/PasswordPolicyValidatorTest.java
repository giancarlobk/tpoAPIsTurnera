package com.grupo1.turnera.security;

import com.grupo1.turnera.exception.ArgumentoInvalidoException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyValidatorTest {

    private final PasswordPolicyValidator validator = new PasswordPolicyValidator();

    @ParameterizedTest(name = "acepta {0} de {1} bytes")
    @MethodSource("passwordsValidos")
    void deberiaAceptarHasta72Bytes(String tipo, int bytes, String password) {
        assertThat(password.getBytes(StandardCharsets.UTF_8)).hasSize(bytes);
        assertThatCode(() -> validator.validateForEncoding(password)).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "rechaza {0} de {1} bytes")
    @MethodSource("passwordsInvalidos")
    void deberiaRechazarMasDe72Bytes(String tipo, int bytes, String password) {
        assertThat(password.getBytes(StandardCharsets.UTF_8)).hasSize(bytes);
        assertThatThrownBy(() -> validator.validateForEncoding(password))
                .isInstanceOf(ArgumentoInvalidoException.class)
                .hasMessage(PasswordPolicyValidator.BCRYPT_LIMIT_MESSAGE);
    }

    private static Stream<Arguments> passwordsValidos() {
        return Stream.of(
                Arguments.of("ASCII", 71, "a".repeat(71)),
                Arguments.of("ASCII", 72, "a".repeat(72)),
                Arguments.of("UTF-8 multibyte", 71, passwordMultibyte(71)),
                Arguments.of("UTF-8 multibyte", 72, passwordMultibyte(72))
        );
    }

    private static Stream<Arguments> passwordsInvalidos() {
        return Stream.of(
                Arguments.of("ASCII", 73, "a".repeat(73)),
                Arguments.of("UTF-8 multibyte", 73, passwordMultibyte(73))
        );
    }

    private static String passwordMultibyte(int bytes) {
        return "a".repeat(bytes - 2) + "ñ";
    }
}
