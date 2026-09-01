package com.grupo1.turnera;

import com.grupo1.turnera.model.Doctor;
import com.grupo1.turnera.model.Especialidad;
import com.grupo1.turnera.model.enums.Rol;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DoctorSearchIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    private Long cardiologiaId;
    private Long traumatologiaId;

    @BeforeEach
    void setUp() {
        Especialidad cardiologia = guardarEspecialidad("Cardiología");
        Especialidad traumatologia = guardarEspecialidad("Traumatología");
        cardiologiaId = cardiologia.getId();
        traumatologiaId = traumatologia.getId();

        guardarDoctor("Ana", "Alvarez", "MN-100", "11111111", "ana@turnera.com", cardiologia, true);
        guardarDoctor("Bruno", "Alvarez", "MN-200", "22222222", "bruno@turnera.com", traumatologia, true);
        guardarDoctor("Carla", "Zeta", "MN-300", "33333333", "carla@turnera.com", cardiologia, true);
        guardarDoctor("Dario", "Inactivo", "MN-400", "44444444", "dario@turnera.com", cardiologia, false);
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void deberiaListarActivosOrdenadosSinExponerRelacionesOSensibles() throws Exception {
        mockMvc.perform(get("/api/doctores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].nombre").value("Ana"))
                .andExpect(jsonPath("$[1].nombre").value("Bruno"))
                .andExpect(jsonPath("$[2].nombre").value("Carla"))
                .andExpect(jsonPath("$[0].especialidadNombre").value("Cardiología"))
                .andExpect(jsonPath("$[0].password").doesNotExist())
                .andExpect(jsonPath("$[0].horariosAtencion").doesNotExist())
                .andExpect(jsonPath("$[0].especialidad").doesNotExist());
    }

    @Test
    void deberiaFiltrarPorEspecialidad() throws Exception {
        mockMvc.perform(get("/api/doctores").param("especialidadId", cardiologiaId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nombre").value("Ana"))
                .andExpect(jsonPath("$[1].nombre").value("Carla"));
    }

    @Test
    void deberiaFiltrarNombreParcialSinDistinguirMayusculas() throws Exception {
        mockMvc.perform(get("/api/doctores").param("nombre", "aN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Ana"));
    }

    @Test
    void deberiaCombinarFiltrosYDevolverListaVaciaSinCoincidencias() throws Exception {
        mockMvc.perform(get("/api/doctores")
                        .param("especialidadId", traumatologiaId.toString())
                        .param("nombre", "Ana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void deberiaResponder400ConEspecialidadInvalida() throws Exception {
        mockMvc.perform(get("/api/doctores").param("especialidadId", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deberiaResponder400ConNombreVacio() throws Exception {
        mockMvc.perform(get("/api/doctores").param("nombre", "   "))
                .andExpect(status().isBadRequest());
    }

    private Especialidad guardarEspecialidad(String nombre) {
        Especialidad especialidad = Especialidad.builder()
                .nombre(nombre)
                .descripcion("Especialidad de prueba")
                .build();
        entityManager.persist(especialidad);
        return especialidad;
    }

    private void guardarDoctor(
            String nombre,
            String apellido,
            String matricula,
            String dni,
            String email,
            Especialidad especialidad,
            boolean activo
    ) {
        Doctor doctor = Doctor.builder()
                .nombre(nombre)
                .apellido(apellido)
                .matriculaNacional(matricula)
                .dni(dni)
                .email(email)
                .password("hash-no-serializado")
                .telefono("1122334455")
                .rol(Rol.MEDICO)
                .activo(activo)
                .especialidad(especialidad)
                .build();
        entityManager.persist(doctor);
    }
}
