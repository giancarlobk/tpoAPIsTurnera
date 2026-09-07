package com.grupo1.turnera.service;

import com.grupo1.turnera.dto.paciente.PacienteCreateRequest;
import com.grupo1.turnera.dto.paciente.PacienteResponse;
import com.grupo1.turnera.exception.DniDuplicadoException;
import com.grupo1.turnera.exception.EmailDuplicadoException;
import com.grupo1.turnera.exception.NumAfiliadoDuplicadoException;
import com.grupo1.turnera.exception.TelefonoDuplicadoException;
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

    private static final String DNI = "30111222";
    private static final String NOMBRE = "Ana";
    private static final String APELLIDO = "Pérez";
    private static final String EMAIL = "ana.perez@example.com";
    private static final String PASSWORD = "ClaveSegura123";
    private static final String PASSWORD_HASH = "$2a$10$hashDePrueba";
    private static final String TELEFONO = "1122334455";
    private static final LocalDate FECHA_NACIMIENTO = LocalDate.of(1995, 4, 18);

    @Mock
    private PacienteRepository pacienteRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PacienteService pacienteService;

    @Test
    void deberiaRegistrarUnPacienteValidoConRolYEstadoPorDefecto() {
        givenSinDuplicados();
        when(passwordEncoder.encode(PASSWORD)).thenReturn(PASSWORD_HASH);
        when(pacienteRepository.save(any(Paciente.class))).thenAnswer(invocation -> {
            Paciente paciente = invocation.getArgument(0);
            paciente.setId(1L);
            return paciente;
        });

        PacienteResponse response = pacienteService.registrarPaciente(requestValido());

        ArgumentCaptor<Paciente> captor = ArgumentCaptor.forClass(Paciente.class);
        verify(pacienteRepository).save(captor.capture());
        Paciente pacienteGuardado = captor.getValue();

        assertThat(pacienteGuardado.getRol()).isEqualTo(Rol.PACIENTE);
        assertThat(pacienteGuardado.getActivo()).isTrue();
        assertThat(pacienteGuardado.getPassword()).isEqualTo(PASSWORD_HASH);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.dni()).isEqualTo(DNI);
        assertThat(response.email()).isEqualTo(EMAIL);
        assertThat(response.rol()).isEqualTo(Rol.PACIENTE);
        assertThat(response.activo()).isTrue();
    }

    @Test
    void deberiaRechazarEmailDuplicado() {
        when(pacienteRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(new Paciente()));

        assertThatThrownBy(() -> pacienteService.registrarPaciente(requestValido()))
                .isInstanceOf(EmailDuplicadoException.class);

        verify(pacienteRepository, never()).save(any());
    }

    @Test
    void deberiaRechazarDniDuplicado() {
        when(pacienteRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());
        when(pacienteRepository.findByDni(DNI)).thenReturn(Optional.of(new Paciente()));

        assertThatThrownBy(() -> pacienteService.registrarPaciente(requestValido()))
                .isInstanceOf(DniDuplicadoException.class);

        verify(pacienteRepository, never()).save(any());
    }

    @Test
    void deberiaRechazarTelefonoDuplicadoCuandoSeInformaTelefono() {
        when(pacienteRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());
        when(pacienteRepository.findByDni(DNI)).thenReturn(Optional.empty());
        when(pacienteRepository.findByTelefono(TELEFONO)).thenReturn(Optional.of(new Paciente()));

        assertThatThrownBy(() -> pacienteService.registrarPaciente(requestValido()))
                .isInstanceOf(TelefonoDuplicadoException.class);

        verify(pacienteRepository, never()).save(any());
    }

    @Test
    void deberiaRechazarNumeroAfiliadoDuplicadoCuandoSeInforma() {
        when(pacienteRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());
        when(pacienteRepository.findByDni(DNI)).thenReturn(Optional.empty());
        when(pacienteRepository.findByTelefono(TELEFONO)).thenReturn(Optional.empty());
        when(pacienteRepository.findByNumeroAfiliado("123456789")).thenReturn(Optional.of(new Paciente()));

        assertThatThrownBy(() -> pacienteService.registrarPaciente(requestValido()))
                .isInstanceOf(NumAfiliadoDuplicadoException.class);

        verify(pacienteRepository, never()).save(any());
    }

    @Test
    void noDeberiaConsultarTelefonoNiNumeroAfiliadoCuandoNoSeInforman() {
        PacienteCreateRequest request = new PacienteCreateRequest(
                DNI, NOMBRE, APELLIDO, EMAIL, PASSWORD,
                null, FECHA_NACIMIENTO, null, null
        );
        when(pacienteRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());
        when(pacienteRepository.findByDni(DNI)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(PASSWORD)).thenReturn(PASSWORD_HASH);
        when(pacienteRepository.save(any(Paciente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        pacienteService.registrarPaciente(request);

        verify(pacienteRepository, never()).findByTelefono(any());
        verify(pacienteRepository, never()).findByNumeroAfiliado(any());
    }

    private void givenSinDuplicados() {
        when(pacienteRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());
        when(pacienteRepository.findByDni(DNI)).thenReturn(Optional.empty());
        when(pacienteRepository.findByTelefono(TELEFONO)).thenReturn(Optional.empty());
        when(pacienteRepository.findByNumeroAfiliado("123456789")).thenReturn(Optional.empty());
    }

    private PacienteCreateRequest requestValido() {
        return new PacienteCreateRequest(
                DNI, NOMBRE, APELLIDO, EMAIL, PASSWORD,
                TELEFONO, FECHA_NACIMIENTO, "OSDE", "123456789"
        );
    }
}
