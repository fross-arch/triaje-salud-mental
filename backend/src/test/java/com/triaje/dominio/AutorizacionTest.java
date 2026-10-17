package com.triaje.dominio;

import com.triaje.dominio.excepcion.ExcepcionDatoInvalido;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AutorizacionTest {

    @Test
    @DisplayName("Una autorizacion nueva debe estar vigente sin fecha de revocacion")
    void autorizacionNuevaDebeEstarVigente() {
        String evaluacionId = UUID.randomUUID().toString();
        Instant ahora = Instant.now();

        Autorizacion autorizacion = Autorizacion.nueva(evaluacionId, 10L, 20L, "1.0", ahora);

        assertThat(autorizacion.getId()).isNotBlank();
        assertThat(autorizacion.getEvaluacionId()).isEqualTo(evaluacionId);
        assertThat(autorizacion.getPacienteId()).isEqualTo(10L);
        assertThat(autorizacion.getClinicoId()).isEqualTo(20L);
        assertThat(autorizacion.getVersionTextoAutorizacion()).isEqualTo("1.0");
        assertThat(autorizacion.getFechaOtorgamiento()).isEqualTo(ahora);
        assertThat(autorizacion.getFechaRevocacion()).isNull();
        assertThat(autorizacion.estaVigente()).isTrue();
    }

    @Test
    @DisplayName("Revocar debe ser una mutacion pura que genera nueva instancia no vigente")
    void revocarDebeSerMutacionPura() {
        String evaluacionId = UUID.randomUUID().toString();
        Instant ahora = Instant.now();
        Instant fechaRevocacion = ahora.plusSeconds(3600);

        Autorizacion original = Autorizacion.nueva(evaluacionId, 10L, 20L, "1.0", ahora);
        Autorizacion revocada = original.revocar(fechaRevocacion);

        assertThat(original.estaVigente()).isTrue();
        assertThat(original.getFechaRevocacion()).isNull();

        assertThat(revocada.getId()).isEqualTo(original.getId());
        assertThat(revocada.estaVigente()).isFalse();
        assertThat(revocada.getFechaRevocacion()).isEqualTo(fechaRevocacion);
    }

    @Test
    @DisplayName("Otorgar debe reactivar una autorizacion previamente revocada como mutacion pura")
    void otorgarDebeReactivarAutorizacion() {
        String evaluacionId = UUID.randomUUID().toString();
        Instant ahora = Instant.now();
        Instant fechaRevocacion = ahora.plusSeconds(100);
        Instant nuevaFecha = ahora.plusSeconds(200);

        Autorizacion original = Autorizacion.nueva(evaluacionId, 10L, 20L, "1.0", ahora);
        Autorizacion revocada = original.revocar(fechaRevocacion);
        Autorizacion reactivada = revocada.otorgar(nuevaFecha);

        assertThat(revocada.estaVigente()).isFalse();
        assertThat(reactivada.getId()).isEqualTo(original.getId());
        assertThat(reactivada.estaVigente()).isTrue();
        assertThat(reactivada.getFechaRevocacion()).isNull();
        assertThat(reactivada.getFechaOtorgamiento()).isEqualTo(nuevaFecha);
    }

    @Test
    @DisplayName("Debe validar datos obligatorios en el constructor")
    void debeValidarDatosObligatorios() {
        Instant ahora = Instant.now();

        assertThatThrownBy(() -> new Autorizacion(null, "ev-1", 1L, 2L, "1.0", ahora, null))
                .isInstanceOf(ExcepcionDatoInvalido.class);

        assertThatThrownBy(() -> new Autorizacion("id-1", "", 1L, 2L, "1.0", ahora, null))
                .isInstanceOf(ExcepcionDatoInvalido.class);

        assertThatThrownBy(() -> new Autorizacion("id-1", "ev-1", null, 2L, "1.0", ahora, null))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new Autorizacion("id-1", "ev-1", 1L, null, "1.0", ahora, null))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new Autorizacion("id-1", "ev-1", 1L, 2L, " ", ahora, null))
                .isInstanceOf(ExcepcionDatoInvalido.class);

        assertThatThrownBy(() -> new Autorizacion("id-1", "ev-1", 1L, 2L, "1.0", null, null))
                .isInstanceOf(NullPointerException.class);
    }
}
