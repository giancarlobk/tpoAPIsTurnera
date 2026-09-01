package com.grupo1.turnera.service;

import com.grupo1.turnera.dto.doctor.DoctorSummaryResponse;
import com.grupo1.turnera.model.Doctor;
import com.grupo1.turnera.model.Especialidad;
import com.grupo1.turnera.repository.DoctorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorServiceTest {

    @Mock
    private DoctorRepository doctorRepository;

    @InjectMocks
    private DoctorService doctorService;

    @Test
    void deberiaBuscarSinFiltrosYMapearElResumen() {
        when(doctorRepository.buscar(null, null)).thenReturn(List.of(doctor()));

        List<DoctorSummaryResponse> resultado = doctorService.buscar(null, null);

        assertThat(resultado).containsExactly(new DoctorSummaryResponse(
                10L,
                "Ana",
                "Alvarez",
                "MN-100",
                5L,
                "Cardiología"
        ));
    }

    @Test
    void deberiaBuscarPorEspecialidad() {
        when(doctorRepository.buscar(5L, null)).thenReturn(List.of(doctor()));

        doctorService.buscar(5L, null);

        verify(doctorRepository).buscar(5L, null);
    }

    @Test
    void deberiaBuscarPorNombreNormalizado() {
        when(doctorRepository.buscar(null, "Ana")).thenReturn(List.of(doctor()));

        doctorService.buscar(null, "  Ana  ");

        verify(doctorRepository).buscar(null, "Ana");
    }

    @Test
    void deberiaCombinarAmbosFiltros() {
        when(doctorRepository.buscar(5L, "Ana")).thenReturn(List.of(doctor()));

        doctorService.buscar(5L, "Ana");

        verify(doctorRepository).buscar(5L, "Ana");
    }

    @Test
    void deberiaDevolverListaVaciaSinCoincidencias() {
        when(doctorRepository.buscar(99L, null)).thenReturn(List.of());

        List<DoctorSummaryResponse> resultado = doctorService.buscar(99L, null);

        assertThat(resultado).isEmpty();
    }

    private Doctor doctor() {
        Especialidad especialidad = Especialidad.builder()
                .id(5L)
                .nombre("Cardiología")
                .build();

        return Doctor.builder()
                .id(10L)
                .nombre("Ana")
                .apellido("Alvarez")
                .matriculaNacional("MN-100")
                .especialidad(especialidad)
                .build();
    }
}
