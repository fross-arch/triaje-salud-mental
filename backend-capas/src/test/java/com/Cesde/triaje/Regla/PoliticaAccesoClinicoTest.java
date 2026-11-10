package com.Cesde.triaje.Regla;

import com.Cesde.triaje.Modelo.MAutorizacion;
import com.Cesde.triaje.Modelo.MEvaluacion;
import com.Cesde.triaje.Modelo.MUsuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PoliticaAccesoClinicoTest {

    private final PoliticaAccesoClinico politica = new PoliticaAccesoClinico();

    private MAutorizacion crearAutorizacion(String evaluacionId, Long pacienteId, Long clinicoId) {
        MEvaluacion evaluacion = new MEvaluacion();
        evaluacion.setId(evaluacionId);

        MUsuario paciente = new MUsuario();
        paciente.setId(pacienteId);

        MUsuario clinico = new MUsuario();
        clinico.setId(clinicoId);

        return new MAutorizacion(
                UUID.randomUUID().toString(),
                evaluacion,
                paciente,
                clinico,
                "1.0",
                Instant.now(),
                null
        );
    }

    @Test
    @DisplayName("Permite lectura si la autorizacion esta vigente y coinciden clinico y evaluacion")
    void permiteLecturaAutorizacionVigenteYDatosCoincidentes() {
        String evaluacionId = UUID.randomUUID().toString();
        Long clinicoId = 55L;
        MAutorizacion autorizacion = crearAutorizacion(evaluacionId, 10L, clinicoId);

        boolean resultado = politica.permiteLectura(autorizacion, clinicoId, evaluacionId);

        assertThat(resultado).isTrue();
    }

    @Test
    @DisplayName("No permite lectura si la autorizacion esta revocada")
    void noPermiteLecturaAutorizacionRevocada() {
        String evaluacionId = UUID.randomUUID().toString();
        Long clinicoId = 55L;
        MAutorizacion autorizacion = crearAutorizacion(evaluacionId, 10L, clinicoId);
        autorizacion.revocar(Instant.now());

        boolean resultado = politica.permiteLectura(autorizacion, clinicoId, evaluacionId);

        assertThat(resultado).isFalse();
    }

    @Test
    @DisplayName("No permite lectura si el clinico no coincide")
    void noPermiteLecturaClinicoDistinto() {
        String evaluacionId = UUID.randomUUID().toString();
        Long clinicoAutorizado = 55L;
        Long clinicoIntruso = 99L;
        MAutorizacion autorizacion = crearAutorizacion(evaluacionId, 10L, clinicoAutorizado);

        boolean resultado = politica.permiteLectura(autorizacion, clinicoIntruso, evaluacionId);

        assertThat(resultado).isFalse();
    }

    @Test
    @DisplayName("No permite lectura si la evaluacion no coincide")
    void noPermiteLecturaEvaluacionDistinta() {
        String evaluacionId = UUID.randomUUID().toString();
        String otraEvaluacionId = UUID.randomUUID().toString();
        Long clinicoId = 55L;
        MAutorizacion autorizacion = crearAutorizacion(evaluacionId, 10L, clinicoId);

        boolean resultado = politica.permiteLectura(autorizacion, clinicoId, otraEvaluacionId);

        assertThat(resultado).isFalse();
    }

    @Test
    @DisplayName("No permite lectura si los argumentos son nulos o el Optional esta vacio")
    void noPermiteLecturaConValoresNulos() {
        assertThat(politica.permiteLectura((MAutorizacion) null, 1L, "ev-1")).isFalse();
        assertThat(politica.permiteLectura(Optional.empty(), 1L, "ev-1")).isFalse();

        MAutorizacion autorizacion = crearAutorizacion("ev-1", 10L, 1L);
        assertThat(politica.permiteLectura(autorizacion, null, "ev-1")).isFalse();
        assertThat(politica.permiteLectura(autorizacion, 1L, null)).isFalse();
    }
}
