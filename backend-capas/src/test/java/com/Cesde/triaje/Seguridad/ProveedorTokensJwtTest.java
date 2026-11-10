package com.Cesde.triaje.Seguridad;

import com.Cesde.triaje.Modelo.Enumeracion.RolUsuario;
import com.Cesde.triaje.Modelo.MUsuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProveedorTokensJwtTest {

    private static final String SECRETO_VALIDO = "secreto-de-pruebas-seguro-con-mas-de-32-bytes-123456";
    private static final String SECRETO_CORTO = "secreto-corto";

    @Test
    @DisplayName("Debe fallar al instanciarse si el secreto es menor a 32 bytes o nulo")
    void debeFallarSiSecretoEsInvalido() {
        assertThrows(IllegalStateException.class, () -> new ProveedorTokensJwt(null, "triaje", 15));
        assertThrows(IllegalStateException.class, () -> new ProveedorTokensJwt(SECRETO_CORTO, "triaje", 15));
    }

    @Test
    @DisplayName("Debe generar y validar token correctamente")
    void debeGenerarYValidarToken() {
        ProveedorTokensJwt proveedor = new ProveedorTokensJwt(SECRETO_VALIDO, "triaje-salud-mental", 15);
        MUsuario usuario = new MUsuario();
        usuario.setId(100L);
        usuario.setRol(RolUsuario.CLINICO);

        ProveedorTokensJwt.TokenAcceso tokenAcceso = proveedor.generar(usuario);
        assertThat(tokenAcceso.token()).isNotBlank();
        assertThat(tokenAcceso.expiraEnSegundos()).isEqualTo(15 * 60);

        Optional<UsuarioAutenticado> autenticadoOpt = proveedor.validar(tokenAcceso.token());
        assertThat(autenticadoOpt).isPresent();
        assertThat(autenticadoOpt.get().id()).isEqualTo(100L);
        assertThat(autenticadoOpt.get().rol()).isEqualTo(RolUsuario.CLINICO);
    }

    @Test
    @DisplayName("Debe rechazar tokens alterados o malformados")
    void debeRechazarTokensInvalidos() {
        ProveedorTokensJwt proveedor = new ProveedorTokensJwt(SECRETO_VALIDO, "triaje-salud-mental", 15);
        assertThat(proveedor.validar("token.invalido.falso")).isEmpty();
        assertThat(proveedor.validar("")).isEmpty();
        assertThat(proveedor.validar(null)).isEmpty();
    }
}
