package com.Cesde.triaje.Controlador;

import com.Cesde.triaje.Dto.RespuestaUsuario;
import com.Cesde.triaje.Dto.SolicitudActualizarPerfil;
import com.Cesde.triaje.Seguridad.GestorCookieRenovacion;
import com.Cesde.triaje.Seguridad.UsuarioAutenticado;
import com.Cesde.triaje.Servicio.SUsuario;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
public class CUsuario {

    private static final Logger REGISTRO = LoggerFactory.getLogger(CUsuario.class);

    private final SUsuario usuarioServicio;
    private final GestorCookieRenovacion gestorCookie;

    public CUsuario(SUsuario usuarioServicio, GestorCookieRenovacion gestorCookie) {
        this.usuarioServicio = usuarioServicio;
        this.gestorCookie = gestorCookie;
    }

    @GetMapping("/yo")
    public ResponseEntity<RespuestaUsuario> obtenerPerfil(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        RespuestaUsuario respuesta = usuarioServicio.obtenerPerfil(usuario.id());
        return ResponseEntity.ok(respuesta);
    }

    @PutMapping("/yo")
    public ResponseEntity<RespuestaUsuario> actualizarPerfil(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody SolicitudActualizarPerfil solicitud) {
        RespuestaUsuario respuesta = usuarioServicio.actualizarPerfil(usuario.id(), solicitud);
        REGISTRO.info("Perfil de usuario actualizado exitosamente.");
        return ResponseEntity.ok(respuesta);
    }

    @DeleteMapping("/yo")
    public ResponseEntity<Void> eliminarCuenta(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        usuarioServicio.eliminarCuenta(usuario.id());
        REGISTRO.info("Cuenta de usuario eliminada exitosamente.");
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, gestorCookie.eliminar().toString())
                .build();
    }
}
