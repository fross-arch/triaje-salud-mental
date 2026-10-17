package com.triaje.infraestructura.web.controlador;

import com.triaje.aplicacion.casouso.CerrarSesion;
import com.triaje.aplicacion.casouso.IniciarSesion;
import com.triaje.aplicacion.casouso.RegistrarUsuario;
import com.triaje.aplicacion.casouso.RenovarSesion;
import com.triaje.aplicacion.dto.ComandoRegistro;
import com.triaje.aplicacion.dto.DatosUsuario;
import com.triaje.aplicacion.dto.ResultadoSesion;
import com.triaje.infraestructura.seguridad.GestorCookieRenovacion;
import com.triaje.infraestructura.seguridad.LimitadorIntentosInicioSesion;
import com.triaje.infraestructura.web.dto.RespuestaToken;
import com.triaje.infraestructura.web.dto.RespuestaUsuario;
import com.triaje.infraestructura.web.dto.SolicitudInicioSesion;
import com.triaje.infraestructura.web.dto.SolicitudRegistro;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/autenticacion")
public class ControladorAutenticacion {

    private static final Logger REGISTRO = LoggerFactory.getLogger(ControladorAutenticacion.class);

    private final RegistrarUsuario registrarUsuario;
    private final IniciarSesion iniciarSesion;
    private final RenovarSesion renovarSesion;
    private final CerrarSesion cerrarSesion;
    private final LimitadorIntentosInicioSesion limitador;
    private final GestorCookieRenovacion gestorCookie;

    public ControladorAutenticacion(RegistrarUsuario registrarUsuario, IniciarSesion iniciarSesion,
                                    RenovarSesion renovarSesion, CerrarSesion cerrarSesion,
                                    LimitadorIntentosInicioSesion limitador, GestorCookieRenovacion gestorCookie) {
        this.registrarUsuario = registrarUsuario;
        this.iniciarSesion = iniciarSesion;
        this.renovarSesion = renovarSesion;
        this.cerrarSesion = cerrarSesion;
        this.limitador = limitador;
        this.gestorCookie = gestorCookie;
    }

    @PostMapping("/registro")
    public ResponseEntity<RespuestaUsuario> registrar(@Valid @RequestBody SolicitudRegistro solicitud,
                                                      HttpServletRequest peticion) {
        limitador.registrarIntentoPorIp(peticion.getRemoteAddr());
        DatosUsuario datos = registrarUsuario.ejecutar(
                new ComandoRegistro(solicitud.nombreCompleto(), solicitud.correo(), solicitud.contrasena()));
        REGISTRO.info("Nuevo usuario registrado exitosamente. Id: {}", datos.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(RespuestaUsuario.desde(datos));
    }

    @PostMapping("/inicio-sesion")
    public ResponseEntity<RespuestaToken> iniciarSesion(@Valid @RequestBody SolicitudInicioSesion solicitud,
                                                        HttpServletRequest peticion) {
        limitador.registrarIntento(peticion.getRemoteAddr(), solicitud.correo());
        ResultadoSesion resultado = iniciarSesion.ejecutar(solicitud.correo(), solicitud.contrasena());
        REGISTRO.info("Inicio de sesion exitoso.");
        return respuestaConSesion(resultado);
    }

    @PostMapping("/renovar")
    public ResponseEntity<RespuestaToken> renovar(HttpServletRequest peticion) {
        limitador.registrarIntentoPorIp(peticion.getRemoteAddr());
        ResultadoSesion resultado = renovarSesion.ejecutar(gestorCookie.leer(peticion));
        REGISTRO.debug("Renovacion de sesion exitosa.");
        return respuestaConSesion(resultado);
    }

    @PostMapping("/cierre-sesion")
    public ResponseEntity<Void> cerrarSesion(HttpServletRequest peticion) {
        cerrarSesion.ejecutar(gestorCookie.leer(peticion));
        REGISTRO.info("Cierre de sesion exitoso.");
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, gestorCookie.eliminar().toString())
                .build();
    }

    private ResponseEntity<RespuestaToken> respuestaConSesion(ResultadoSesion resultado) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        gestorCookie.crear(resultado.tokenRenovacion(), resultado.duracionRenovacion()).toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(RespuestaToken.bearer(resultado.tokenAcceso(), resultado.expiraEnSegundos()));
    }
}
