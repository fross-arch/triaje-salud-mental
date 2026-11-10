package com.Cesde.triaje.Integracion;

import com.Cesde.triaje.Modelo.Enumeracion.EstadoCita;
import com.Cesde.triaje.Modelo.Enumeracion.ModalidadCita;
import com.Cesde.triaje.Modelo.Enumeracion.RolUsuario;
import com.Cesde.triaje.Modelo.MAutorizacion;
import com.Cesde.triaje.Modelo.MCita;
import com.Cesde.triaje.Modelo.MEvaluacion;
import com.Cesde.triaje.Modelo.MUsuario;
import com.Cesde.triaje.Repositorio.IAutorizacionRepositorio;
import com.Cesde.triaje.Repositorio.ICitaRepositorio;
import com.Cesde.triaje.Repositorio.IEvaluacionRepositorio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EliminacionClinicoTest extends PruebaIntegracionBase {

    @Autowired
    private IAutorizacionRepositorio autorizacionRepositorio;

    @Autowired
    private ICitaRepositorio citaRepositorio;

    @Autowired
    private IEvaluacionRepositorio evaluacionRepositorio;

    @Test
    @DisplayName("a) Eliminar clínico con citas asociadas responde 409 sugiriendo desactivar la cuenta")
    void eliminarClinicoConCitasResponde409YSugiereDesactivar() throws Exception {
        // 1. Crear admin, paciente y clínico
        String correoAdmin = correoUnico();
        crearUsuarioDirecto(correoAdmin, "Admin Principal", RolUsuario.ADMINISTRADOR, true);
        MvcResult loginAdmin = iniciarSesion(correoAdmin);
        String tokenAdmin = tokenAcceso(loginAdmin);

        String correoClinico = correoUnico();
        Long clinicoId = crearUsuarioDirecto(correoClinico, "Dr. Especialista", RolUsuario.CLINICO, true, "Psiquiatría");

        String correoPaciente = correoUnico();
        Long pacienteId = crearUsuarioDirecto(correoPaciente, "Paciente Consulta", RolUsuario.PACIENTE, true);

        MUsuario paciente = usuarioRepositorio.findById(pacienteId).orElseThrow();
        MUsuario clinico = usuarioRepositorio.findById(clinicoId).orElseThrow();

        // 2. Crear evaluación y autorización
        MEvaluacion evaluacion = new MEvaluacion(paciente);
        evaluacionRepositorio.save(evaluacion);

        MAutorizacion autorizacion = new MAutorizacion(
                UUID.randomUUID().toString(),
                evaluacion,
                paciente,
                clinico,
                "1.0",
                Instant.now().minus(1, ChronoUnit.HOURS),
                null
        );
        autorizacionRepositorio.save(autorizacion);

        // 3. Crear cita asociada al clínico
        MCita cita = new MCita();
        cita.setId(UUID.randomUUID().toString());
        cita.setAutorizacion(autorizacion);
        cita.setPaciente(paciente);
        cita.setClinico(clinico);
        cita.setFechaHora(Instant.now().plus(2, ChronoUnit.DAYS));
        cita.setModalidad(ModalidadCita.VIRTUAL);
        cita.setEstado(EstadoCita.PROPUESTA);
        citaRepositorio.save(cita);

        // 4. Intentar eliminar el clínico con DELETE /api/administracion/clinicos/{id}
        mockMvc.perform(delete("/api/administracion/clinicos/" + clinicoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("REGLA_DE_NEGOCIO"))
                .andExpect(jsonPath("$.mensaje").value("No es posible eliminar el profesional clínico porque cuenta con citas asociadas en su historial. Se recomienda desactivar la cuenta en su lugar."));

        // Verificar que el clínico sigue existiendo
        assertThat(usuarioRepositorio.findById(clinicoId)).isPresent();
    }

    @Test
    @DisplayName("b) Eliminar clínico con autorizaciones asociadas responde 409 sugiriendo desactivar la cuenta")
    void eliminarClinicoConAutorizacionesResponde409YSugiereDesactivar() throws Exception {
        // 1. Crear admin, paciente y clínico
        String correoAdmin = correoUnico();
        crearUsuarioDirecto(correoAdmin, "Admin Principal 2", RolUsuario.ADMINISTRADOR, true);
        MvcResult loginAdmin = iniciarSesion(correoAdmin);
        String tokenAdmin = tokenAcceso(loginAdmin);

        String correoClinico = correoUnico();
        Long clinicoId = crearUsuarioDirecto(correoClinico, "Dra. Terapeuta", RolUsuario.CLINICO, true, "Psicología");

        String correoPaciente = correoUnico();
        Long pacienteId = crearUsuarioDirecto(correoPaciente, "Paciente Autorizante", RolUsuario.PACIENTE, true);

        MUsuario paciente = usuarioRepositorio.findById(pacienteId).orElseThrow();
        MUsuario clinico = usuarioRepositorio.findById(clinicoId).orElseThrow();

        // 2. Crear evaluación y autorización (sin cita)
        MEvaluacion evaluacion = new MEvaluacion(paciente);
        evaluacionRepositorio.save(evaluacion);

        MAutorizacion autorizacion = new MAutorizacion(
                UUID.randomUUID().toString(),
                evaluacion,
                paciente,
                clinico,
                "1.0",
                Instant.now().minus(1, ChronoUnit.HOURS),
                null
        );
        autorizacionRepositorio.save(autorizacion);

        // 3. Intentar eliminar el clínico
        mockMvc.perform(delete("/api/administracion/clinicos/" + clinicoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("REGLA_DE_NEGOCIO"))
                .andExpect(jsonPath("$.mensaje").value("No es posible eliminar el profesional clínico porque cuenta con autorizaciones de acceso asociadas en su historial. Se recomienda desactivar la cuenta en su lugar."));

        // Verificar que el clínico sigue existiendo
        assertThat(usuarioRepositorio.findById(clinicoId)).isPresent();
    }

    @Test
    @DisplayName("c) Eliminar clínico sin citas ni autorizaciones responde 204 y lo elimina")
    void eliminarClinicoSinCitasNiAutorizacionesResponde204YLoElimina() throws Exception {
        // 1. Crear admin y clínico limpio
        String correoAdmin = correoUnico();
        crearUsuarioDirecto(correoAdmin, "Admin Principal 3", RolUsuario.ADMINISTRADOR, true);
        MvcResult loginAdmin = iniciarSesion(correoAdmin);
        String tokenAdmin = tokenAcceso(loginAdmin);

        String correoClinico = correoUnico();
        Long clinicoId = crearUsuarioDirecto(correoClinico, "Dr. Nuevo Sin Historial", RolUsuario.CLINICO, true, "Medicina General");

        // 2. Eliminar el clínico
        mockMvc.perform(delete("/api/administracion/clinicos/" + clinicoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isNoContent());

        // 3. Verificar que el clínico fue eliminado de la base de datos
        assertThat(usuarioRepositorio.findById(clinicoId)).isEmpty();
    }
}
