package com.triaje.infraestructura.web.controlador;

import com.triaje.aplicacion.casouso.ActualizarPerfil;
import com.triaje.aplicacion.casouso.EliminarCuenta;
import com.triaje.aplicacion.casouso.ObtenerUsuarioActual;
import com.triaje.aplicacion.dto.ComandoActualizarPerfil;
import com.triaje.aplicacion.dto.DatosUsuario;
import com.triaje.infraestructura.seguridad.GestorCookieRenovacion;
import com.triaje.infraestructura.seguridad.UsuarioAutenticado;
import com.triaje.infraestructura.web.dto.RespuestaUsuario;
import com.triaje.infraestructura.web.dto.SolicitudActualizarPerfil;
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
public class ControladorUsuarios {

    private static final Logger REGISTRO = LoggerFactory.getLogger(ControladorUsuarios.class);

    private final ObtenerUsuarioActual obtenerUsuarioActual;
    private final ActualizarPerfil actualizarPerfil;
    private final EliminarCuenta eliminarCuenta;
    private final GestorCookieRenovacion gestorCookie;

    public ControladorUsuarios(ObtenerUsuarioActual obtenerUsuarioActual,
                               ActualizarPerfil actualizarPerfil,
                               EliminarCuenta eliminarCuenta,
                               GestorCookieRenovacion gestorCookie) {
        this.obtenerUsuarioActual = obtenerUsuarioActual;
        this.actualizarPerfil = actualizarPerfil;
        this.eliminarCuenta = eliminarCuenta;
        this.gestorCookie = gestorCookie;
    }

    @GetMapping("/yo")
    public RespuestaUsuario obtenerPerfil(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return RespuestaUsuario.desde(obtenerUsuarioActual.ejecutar(usuario.id()));
    }

    @PutMapping("/yo")
    public RespuestaUsuario actualizarPerfil(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                             @Valid @RequestBody SolicitudActualizarPerfil solicitud) {
        DatosUsuario datos = actualizarPerfil.ejecutar(new ComandoActualizarPerfil(
                usuario.id(),
                solicitud.nombreCompleto(),
                solicitud.correo(),
                solicitud.especialidad(),
                solicitud.contrasenaActual(),
                solicitud.nuevaContrasena()
        ));
        REGISTRO.info("Perfil de usuario actualizado exitosamente.");
        return RespuestaUsuario.desde(datos);
    }

    /**
     * Elimina la cuenta y todos sus datos (consentimientos, evaluaciones, respuestas y sesiones).
     */
    @DeleteMapping("/yo")
    public ResponseEntity<Void> eliminarCuenta(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        eliminarCuenta.ejecutar(usuario.id());
        REGISTRO.info("Cuenta de usuario eliminada exitosamente.");
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, gestorCookie.eliminar().toString())
                .build();
    }
}
