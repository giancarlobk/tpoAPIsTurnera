package com.grupo1.turnera.service;

import com.grupo1.turnera.dto.paciente.PacienteCreateRequest;
import com.grupo1.turnera.dto.paciente.PacienteResponse;
import com.grupo1.turnera.exception.DniDuplicadoException;
import com.grupo1.turnera.exception.EmailDuplicadoException;
import com.grupo1.turnera.model.Paciente;
import com.grupo1.turnera.model.enums.Rol;
import com.grupo1.turnera.repository.PacienteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PacienteServiceTest {

    private static final String EMAIL = "ana.perez@example.com";
    private static final String DNI = "30111222";
    private static final String PASSWORD = "ClaveSegura123";
    private static final String PASSWORD_HASH = "$2a$10$hashDePrueba";

    @Mock
    private PacienteRepository pacienteRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PacienteService pacienteService;

    @Test
    void deberiaRegistrarPacienteValidoSinExponerPassword() {
        when(pacienteRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());
        when(pacienteRepository.findByDni(DNI)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(PASSWORD)).thenReturn(PASSWORD_HASH);
        when(pacienteRepository.save(any(Paciente.class))).thenAnswer(invocation -> {
            Paciente paciente = invocation.getArgument(0);
            paciente.setId(10L);
            return paciente;
        });

        PacienteResponse response = pacienteService.registrarPaciente(requestValido());

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.dni()).isEqualTo(DNI);
        assertThat(response.email()).isEqualTo(EMAIL);
        assertThat(response.rol()).isEqualTo(Rol.PACIENTE);
        assertThat(response.activo()).isTrue();

        ArgumentCaptor<Paciente> captor = ArgumentCaptor.forClass(Paciente.class);
        verify(pacienteRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo(PASSWORD_HASH);
        assertThat(captor.getValue().getPassword()).isNotEqualTo(PASSWORD);
    }

    @Test
    void deberiaRechazarEmailDuplicado() {
        when(pacienteRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(Paciente.builder().build()));

        assertThatThrownBy(() -> pacienteService.registrarPaciente(requestValido()))
                .isInstanceOf(EmailDuplicadoException.class);

        verify(pacienteRepository, never()).save(any());
    }

    @Test
    void deberiaRechazarDniDuplicado() {
        when(pacienteRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());
        when(pacienteRepository.findByDni(DNI)).thenReturn(Optional.of(Paciente.builder().build()));

        assertThatThrownBy(() -> pacienteService.registrarPaciente(requestValido()))
                .isInstanceOf(DniDuplicadoException.class);

        verify(pacienteRepository, never()).save(any());
    }

    private PacienteCreateRequest requestValido() {
        return new PacienteCreateRequest(
                DNI,
                "Ana",
                "Pérez",
                EMAIL,
                PASSWORD,
                "1122334455",
                LocalDate.of(1995, 4, 18),
                "OSDE",
                "123456789"
        );
    }
}
