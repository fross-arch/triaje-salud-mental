package com.Cesde.triaje.Seguridad;

import com.Cesde.triaje.Excepcion.ExcepcionDemasiadosIntentos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LimitadorIntentosTest {

    @Test
    @DisplayName("Debe permitir 20 intentos por IP y bloquear al intento 21")
    void debeBloquearExcesoPorIp() {
        LimitadorIntentos limitador = new LimitadorIntentos();
        String ip = "192.168.1.100";

        for (int i = 0; i < 20; i++) {
            assertDoesNotThrow(() -> limitador.registrarIntentoPorIp(ip));
        }

        ExcepcionDemasiadosIntentos ex = assertThrows(ExcepcionDemasiadosIntentos.class,
                () -> limitador.registrarIntentoPorIp(ip));
        assertThat(ex.getSegundosEspera()).isPositive();
    }

    @Test
    @DisplayName("Debe permitir 5 intentos por correo e IP y bloquear al intento 6")
    void debeBloquearExcesoPorCorreoEIp() {
        LimitadorIntentos limitador = new LimitadorIntentos();
        String ip = "192.168.1.101";
        String correo = "paciente@cesde.edu.co";

        for (int i = 0; i < 5; i++) {
            assertDoesNotThrow(() -> limitador.registrarIntento(ip, correo));
        }

        ExcepcionDemasiadosIntentos ex = assertThrows(ExcepcionDemasiadosIntentos.class,
                () -> limitador.registrarIntento(ip, correo));
        assertThat(ex.getSegundosEspera()).isPositive();
    }
}
