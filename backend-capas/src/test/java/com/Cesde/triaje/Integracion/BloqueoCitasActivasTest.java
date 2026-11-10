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
import org.springframework.http.MediaType;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BloqueoCitasActivasTest extends PruebaIntegracionBase {

    @Autowired
    private ICitaRepositorio citaRepositorio;

    @Autowired
    private IAutorizacionRepositorio autorizacionRepositorio;

    @Autowired
    private IEvaluacionRepositorio evaluacionRepositorio;

    private void crearCitaConEstado(Long pacienteId, Long clinicoId, EstadoCita estado) {
        MUsuario paciente = usuarioRepositorio.findById(pacienteId).orElseThrow();
        MUsuario clinico = usuarioRepositorio.findById(clinicoId).orElseThrow();

        MEvaluacion evaluacion = new MEvaluacion(paciente);
        evaluacion.setEstado(com.Cesde.triaje.Modelo.Enumeracion.EstadoEvaluacion.COMPLETADA);
        evaluacionRepositorio.save(evaluacion);

        MAutorizacion autorizacion = new MAutorizacion(evaluacion, paciente, clinico, "1.0");
        autorizacionRepositorio.save(autorizacion);

        Instant fechaHora = Instant.now().plus(2, ChronoUnit.DAYS);
        MCita cita = new MCita(
                UUID.randomUUID().toString(),
                autorizacion,
                paciente,
                clinico,
                fechaHora,
                ModalidadCita.VIRTUAL,
                "Mensaje de prueba",
                estado,
                estado == EstadoCita.CUMPLIDA ? "Atención completada" : null,
                Instant.now(),
                Instant.now()
        );
        citaRepositorio.save(cita);
    }

    @Test
    @DisplayName("Debe bloquear revocación de consentimiento si el paciente tiene cita PROPUESTA")
    void debeBloquearRevocacionConCitaPropuesta() throws Exception {
        String correoPaciente = correoUnico();
        long pacienteId = registrar(correoPaciente);
        String tokenPaciente = tokenAcceso(iniciarSesion(correoPaciente));

        // Aceptar consentimiento
        mockMvc.perform(post("/api/consentimientos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("confirmaMayoriaEdad", true))))
                .andExpect(status().isCreated());

        Long clinicoId = crearUsuarioDirecto(correoUnico(), "Dra. Clinica", RolUsuario.CLINICO, true);
        crearCitaConEstado(pacienteId, clinicoId, EstadoCita.PROPUESTA);

        // Intentar revocar consentimiento
        mockMvc.perform(post("/api/consentimientos/revocar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("REGLA_DE_NEGOCIO"))
                .andExpect(jsonPath("$.mensaje").value("No puedes revocar el consentimiento mientras tengas una cita activa programada o propuesta. Debes completarla o cancelarla desde 'Mis Citas'."));
    }

    @Test
    @DisplayName("Debe bloquear revocación de consentimiento si el paciente tiene cita CONFIRMADA")
    void debeBloquearRevocacionConCitaConfirmada() throws Exception {
        String correoPaciente = correoUnico();
        long pacienteId = registrar(correoPaciente);
        String tokenPaciente = tokenAcceso(iniciarSesion(correoPaciente));

        mockMvc.perform(post("/api/consentimientos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("confirmaMayoriaEdad", true))))
                .andExpect(status().isCreated());

        Long clinicoId = crearUsuarioDirecto(correoUnico(), "Dr. Medico", RolUsuario.CLINICO, true);
        crearCitaConEstado(pacienteId, clinicoId, EstadoCita.CONFIRMADA);

        mockMvc.perform(post("/api/consentimientos/revocar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("REGLA_DE_NEGOCIO"))
                .andExpect(jsonPath("$.mensaje").value("No puedes revocar el consentimiento mientras tengas una cita activa programada o propuesta. Debes completarla o cancelarla desde 'Mis Citas'."));
    }

    @Test
    @DisplayName("Debe permitir revocación si el paciente tiene cita CUMPLIDA, CANCELADA o RECHAZADA")
    void debePermitirRevocacionConCitasNoActivas() throws Exception {
        String correoPaciente = correoUnico();
        long pacienteId = registrar(correoPaciente);
        String tokenPaciente = tokenAcceso(iniciarSesion(correoPaciente));

        mockMvc.perform(post("/api/consentimientos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("confirmaMayoriaEdad", true))))
                .andExpect(status().isCreated());

        Long clinicoId = crearUsuarioDirecto(correoUnico(), "Dra. Psiquiatra", RolUsuario.CLINICO, true);
        crearCitaConEstado(pacienteId, clinicoId, EstadoCita.CUMPLIDA);
        crearCitaConEstado(pacienteId, clinicoId, EstadoCita.CANCELADA);
        crearCitaConEstado(pacienteId, clinicoId, EstadoCita.RECHAZADA);

        mockMvc.perform(post("/api/consentimientos/revocar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Consentimiento revocado exitosamente."));
    }

    @Test
    @DisplayName("Debe permitir revocación si el paciente no tiene citas")
    void debePermitirRevocacionSinCitas() throws Exception {
        String correoPaciente = correoUnico();
        registrar(correoPaciente);
        String tokenPaciente = tokenAcceso(iniciarSesion(correoPaciente));

        mockMvc.perform(post("/api/consentimientos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("confirmaMayoriaEdad", true))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/consentimientos/revocar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Debe bloquear eliminación de cuenta si el paciente tiene cita PROPUESTA")
    void debeBloquearEliminacionConCitaPropuesta() throws Exception {
        String correoPaciente = correoUnico();
        long pacienteId = registrar(correoPaciente);
        String tokenPaciente = tokenAcceso(iniciarSesion(correoPaciente));

        Long clinicoId = crearUsuarioDirecto(correoUnico(), "Dra. Clinica 2", RolUsuario.CLINICO, true);
        crearCitaConEstado(pacienteId, clinicoId, EstadoCita.PROPUESTA);

        mockMvc.perform(delete("/api/usuarios/yo")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("REGLA_DE_NEGOCIO"))
                .andExpect(jsonPath("$.mensaje").value("No puedes eliminar tu cuenta mientras tengas una cita activa programada o propuesta. Debes completarla o cancelarla desde 'Mis Citas'."));
    }

    @Test
    @DisplayName("Debe bloquear eliminación de cuenta si el paciente tiene cita CONFIRMADA")
    void debeBloquearEliminacionConCitaConfirmada() throws Exception {
        String correoPaciente = correoUnico();
        long pacienteId = registrar(correoPaciente);
        String tokenPaciente = tokenAcceso(iniciarSesion(correoPaciente));

        Long clinicoId = crearUsuarioDirecto(correoUnico(), "Dr. Medico 2", RolUsuario.CLINICO, true);
        crearCitaConEstado(pacienteId, clinicoId, EstadoCita.CONFIRMADA);

        mockMvc.perform(delete("/api/usuarios/yo")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPaciente)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("REGLA_DE_NEGOCIO"))
                .andExpect(jsonPath("$.mensaje").value("No puedes eliminar tu cuenta mientras tengas una cita activa programada o propuesta. Debes completarla o cancelarla desde 'Mis Citas'."));
    }
}
