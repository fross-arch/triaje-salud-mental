package com.triaje.dominio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PoliticaAccesoClinicoTest {

    private final PoliticaAccesoClinico politica = new PoliticaAccesoClinico();

    @Test
    @DisplayName("Permite lectura si la autorizacion esta vigente y coinciden clinico y evaluacion")
    void permiteLecturaAutorizacionVigenteYDatosCoincidentes() {
        String evaluacionId = UUID.randomUUID().toString();
        Long clinicoId = 55L;
        Autorizacion autorizacion = Autorizacion.nueva(evaluacionId, 10L, clinicoId, "1.0", Instant.now());

        boolean resultado = politica.permiteLectura(autorizacion, clinicoId, evaluacionId);

        assertThat(resultado).isTrue();
    }

    @Test
    @DisplayName("No permite lectura si la autorizacion esta revocada")
    void noPermiteLecturaAutorizacionRevocada() {
        String evaluacionId = UUID.randomUUID().toString();
        Long clinicoId = 55L;
        Autorizacion autorizacion = Autorizacion.nueva(evaluacionId, 10L, clinicoId, "1.0", Instant.now())
                .revocar(Instant.now());

        boolean resultado = politica.permiteLectura(autorizacion, clinicoId, evaluacionId);

        assertThat(resultado).isFalse();
    }

    @Test
    @DisplayName("No permite lectura si el clinico no coincide")
    void noPermiteLecturaClinicoDistinto() {
        String evaluacionId = UUID.randomUUID().toString();
        Long clinicoAutorizado = 55L;
        Long clinicoIntruso = 99L;
        Autorizacion autorizacion = Autorizacion.nueva(evaluacionId, 10L, clinicoAutorizado, "1.0", Instant.now());

        boolean resultado = politica.permiteLectura(autorizacion, clinicoIntruso, evaluacionId);

        assertThat(resultado).isFalse();
    }

    @Test
    @DisplayName("No permite lectura si la evaluacion no coincide")
    void noPermiteLecturaEvaluacionDistinta() {
        String evaluacionId = UUID.randomUUID().toString();
        String otraEvaluacionId = UUID.randomUUID().toString();
        Long clinicoId = 55L;
        Autorizacion autorizacion = Autorizacion.nueva(evaluacionId, 10L, clinicoId, "1.0", Instant.now());

        boolean resultado = politica.permiteLectura(autorizacion, clinicoId, otraEvaluacionId);

        assertThat(resultado).isFalse();
    }

    @Test
    @DisplayName("No permite lectura si los argumentos son nulos o el Optional esta vacio")
    void noPermiteLecturaConValoresNulos() {
        assertThat(politica.permiteLectura((Autorizacion) null, 1L, "ev-1")).isFalse();
        assertThat(politica.permiteLectura(Optional.empty(), 1L, "ev-1")).isFalse();

        Autorizacion autorizacion = Autorizacion.nueva("ev-1", 10L, 1L, "1.0", Instant.now());
        assertThat(politica.permiteLectura(autorizacion, null, "ev-1")).isFalse();
        assertThat(politica.permiteLectura(autorizacion, 1L, null)).isFalse();
    }
}
