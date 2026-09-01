package com.grupo1.turnera.service;

import com.grupo1.turnera.dto.auth.LoginRequest;
import com.grupo1.turnera.dto.auth.LoginResponse;
import com.grupo1.turnera.exception.CredencialesInvalidasException;
import com.grupo1.turnera.model.Administrador;
import com.grupo1.turnera.model.Doctor;
import com.grupo1.turnera.model.Paciente;
import com.grupo1.turnera.model.enums.Rol;
import com.grupo1.turnera.repository.AdministradorRepository;
import com.grupo1.turnera.repository.DoctorRepository;
import com.grupo1.turnera.repository.PacienteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String EMAIL = "usuario@turnera.com";
    private static final String PASSWORD = "ClaveSegura123";
    private static final String PASSWORD_HASH = "$2a$10$hashDePrueba";

    @Mock
    private PacienteRepository pacienteRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private AdministradorRepository administradorRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void deberiaAutenticarPacienteActivoConPasswordValida() {
        Paciente paciente = pacienteActivo();
        givenOnlyPaciente(paciente);
        when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);

        LoginResponse response = authService.login(new LoginRequest("  " + EMAIL + "  ", PASSWORD));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo(EMAIL);
        assertThat(response.rol()).isEqualTo(Rol.PACIENTE);
        verify(passwordEncoder).matches(PASSWORD, PASSWORD_HASH);
    }

    @Test
    void deberiaAutenticarDoctor() {
        Doctor doctor = Doctor.builder()
                .id(2L)
                .dni("22222222")
                .nombre("Ana")
                .apellido("Médica")
                .email(EMAIL)
                .password(PASSWORD_HASH)
                .rol(Rol.MEDICO)
                .activo(true)
                .matriculaNacional("MN-123")
                .build();
        givenOnlyDoctor(doctor);
        when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);

        LoginResponse response = authService.login(new LoginRequest(EMAIL, PASSWORD));

        assertThat(response.id()).isEqualTo(2L);
        assertThat(response.rol()).isEqualTo(Rol.MEDICO);
    }

    @Test
    void deberiaAutenticarAdministrador() {
        Administrador administrador = Administrador.builder()
                .id(3L)
                .dni("33333333")
                .nombre("Ada")
                .apellido("Admin")
                .email(EMAIL)
                .password(PASSWORD_HASH)
                .rol(Rol.ADMIN)
                .activo(true)
                .build();
        givenOnlyAdministrador(administrador);
        when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);

        LoginResponse response = authService.login(new LoginRequest(EMAIL, PASSWORD));

        assertThat(response.id()).isEqualTo(3L);
        assertThat(response.rol()).isEqualTo(Rol.ADMIN);
    }

    @Test
    void deberiaRechazarPasswordIncorrecta() {
        givenOnlyPaciente(pacienteActivo());
        when(passwordEncoder.matches("incorrecta", PASSWORD_HASH)).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, "incorrecta")))
                .isInstanceOf(CredencialesInvalidasException.class)
                .hasMessage("Email o contraseña incorrectos");
    }

    @Test
    void deberiaRechazarUsuarioInactivoSinCompararPassword() {
        Paciente paciente = pacienteActivo();
        paciente.setActivo(false);
        givenOnlyPaciente(paciente);

        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, PASSWORD)))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    void deberiaRechazarEmailInexistente() {
        givenNoUsers();

        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, PASSWORD)))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    void deberiaRechazarEmailDuplicadoEntreTiposDeUsuario() {
        Paciente paciente = pacienteActivo();
        Administrador administrador = Administrador.builder()
                .id(3L)
                .email(EMAIL)
                .password(PASSWORD_HASH)
                .rol(Rol.ADMIN)
                .activo(true)
                .build();
        when(pacienteRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(paciente));
        when(doctorRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());
        when(administradorRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(administrador));

        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, PASSWORD)))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    private Paciente pacienteActivo() {
        return Paciente.builder()
                .id(1L)
                .dni("11111111")
                .nombre("Pablo")
                .apellido("Paciente")
                .email(EMAIL)
                .password(PASSWORD_HASH)
                .rol(Rol.PACIENTE)
                .activo(true)
                .fechaNacimiento(LocalDate.of(1990, 1, 1))
                .build();
    }

    private void givenOnlyPaciente(Paciente paciente) {
        when(pacienteRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(paciente));
        when(doctorRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());
        when(administradorRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());
    }

    private void givenOnlyDoctor(Doctor doctor) {
        when(pacienteRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());
        when(doctorRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(doctor));
        when(administradorRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());
    }

    private void givenOnlyAdministrador(Administrador administrador) {
        when(pacienteRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());
        when(doctorRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());
        when(administradorRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(administrador));
    }

    private void givenNoUsers() {
        when(pacienteRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());
        when(doctorRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());
        when(administradorRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());
    }
}
