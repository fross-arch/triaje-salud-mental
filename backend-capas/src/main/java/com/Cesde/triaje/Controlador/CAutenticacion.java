package com.Cesde.triaje.Controlador;

import com.Cesde.triaje.Dto.RespuestaError;
import com.Cesde.triaje.Dto.RespuestaToken;
import com.Cesde.triaje.Dto.RespuestaUsuario;
import com.Cesde.triaje.Dto.SolicitudInicioSesion;
import com.Cesde.triaje.Dto.SolicitudRegistro;
import com.Cesde.triaje.Excepcion.ExcepcionDemasiadosIntentos;
import com.Cesde.triaje.Excepcion.ExcepcionTokenRenovacionInvalido;
import com.Cesde.triaje.Seguridad.GestorCookieRenovacion;
import com.Cesde.triaje.Seguridad.LimitadorIntentos;
import com.Cesde.triaje.Servicio.SAutenticacion;
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

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/autenticacion")
public class CAutenticacion {

    private static final Logger REGISTRO = LoggerFactory.getLogger(CAutenticacion.class);

    private final SAutenticacion autenticacionServicio;
    private final LimitadorIntentos limitadorIntentos;
    private final GestorCookieRenovacion gestorCookie;

    public CAutenticacion(
            SAutenticacion autenticacionServicio,
            LimitadorIntentos limitadorIntentos,
            GestorCookieRenovacion gestorCookie) {
        this.autenticacionServicio = autenticacionServicio;
        this.limitadorIntentos = limitadorIntentos;
        this.gestorCookie = gestorCookie;
    }

    @PostMapping("/registro")
    public ResponseEntity<RespuestaUsuario> registrar(
            @Valid @RequestBody SolicitudRegistro solicitud,
            HttpServletRequest peticion) {
        limitadorIntentos.registrarIntentoPorIp(peticion.getRemoteAddr());
        RespuestaUsuario respuesta = autenticacionServicio.registrar(solicitud);
        REGISTRO.info("Nuevo usuario registrado exitosamente.");
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @PostMapping("/inicio-sesion")
    public ResponseEntity<RespuestaToken> iniciarSesion(
            @Valid @RequestBody SolicitudInicioSesion solicitud,
            HttpServletRequest peticion) {
        limitadorIntentos.registrarIntento(peticion.getRemoteAddr(), solicitud.correo());
        SAutenticacion.ResultadoSesion resultado = autenticacionServicio.iniciarSesion(solicitud);
        REGISTRO.info("Inicio de sesión exitoso.");
        return respuestaConSesion(resultado);
    }

    @PostMapping("/renovar")
    public ResponseEntity<?> renovar(HttpServletRequest peticion) {
        limitadorIntentos.registrarIntentoPorIp(peticion.getRemoteAddr());
        String tokenRenovacion = gestorCookie.leer(peticion);
        try {
            SAutenticacion.ResultadoSesion resultado = autenticacionServicio.renovar(tokenRenovacion);
            REGISTRO.debug("Renovación de sesión exitosa.");
            return respuestaConSesion(resultado);
        } catch (ExcepcionTokenRenovacionInvalido | ExcepcionDemasiadosIntentos e) {
            throw e;
        } catch (Exception e) {
            REGISTRO.warn("Fallo no controlado al renovar sesión: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.SET_COOKIE, gestorCookie.eliminar().toString())
                    .body(new RespuestaError(
                            Instant.now(),
                            HttpStatus.UNAUTHORIZED.value(),
                            "SESION_INVALIDA",
                            "No fue posible renovar la sesión.",
                            peticion.getRequestURI(),
                            List.of()
                    ));
        }
    }

    @PostMapping("/cierre-sesion")
    public ResponseEntity<Void> cerrarSesion(HttpServletRequest peticion) {
        String tokenRenovacion = gestorCookie.leer(peticion);
        autenticacionServicio.cerrarSesion(tokenRenovacion);
        REGISTRO.info("Cierre de sesión exitoso.");
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, gestorCookie.eliminar().toString())
                .build();
    }

    private ResponseEntity<RespuestaToken> respuestaConSesion(SAutenticacion.ResultadoSesion resultado) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        gestorCookie.crear(resultado.tokenRenovacion(), resultado.duracionRenovacion()).toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(new RespuestaToken(resultado.tokenAcceso(), "Bearer", resultado.expiraEnSegundos()));
    }
}
