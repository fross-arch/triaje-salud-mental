package com.Cesde.triaje.Regla;

import com.Cesde.triaje.Excepcion.ExcepcionDatoInvalido;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PoliticaContrasenaTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Debe lanzar ExcepcionDatoInvalido si la contraseña es nula o blanca")
    void deberiaLanzarExcepcionCuandoContrasenaEsNulaOBlanca(String contrasenaInvalida) {
        assertThrows(ExcepcionDatoInvalido.class, () -> PoliticaContrasena.validar(contrasenaInvalida));
    }

    @ParameterizedTest
    @ValueSource(strings = {"123456789", "a", "segura123", "abcde1234"})
    @DisplayName("Debe lanzar ExcepcionDatoInvalido si la contraseña tiene menos de 10 caracteres")
    void deberiaLanzarExcepcionCuandoTieneMenosDeDiezCaracteres(String contrasenaCorta) {
        assertThrows(ExcepcionDatoInvalido.class, () -> PoliticaContrasena.validar(contrasenaCorta));
    }

    @Test
    @DisplayName("Debe lanzar ExcepcionDatoInvalido si la contraseña supera los 72 bytes")
    void deberiaLanzarExcepcionCuandoSuperaSetentaYDosBytes() {
        String contrasenaLarga = "a".repeat(73);
        assertThrows(ExcepcionDatoInvalido.class, () -> PoliticaContrasena.validar(contrasenaLarga));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ContraseñaSegura123",
            "1234567890",
            "MiClaveClinica!2026",
            "abcdefghij"
    })
    @DisplayName("Debe aceptar contraseñas válidas de al menos 10 caracteres y hasta 72 bytes")
    void deberiaAceptarContrasenasValidas(String contrasenaValida) {
        assertDoesNotThrow(() -> PoliticaContrasena.validar(contrasenaValida));
    }
}
