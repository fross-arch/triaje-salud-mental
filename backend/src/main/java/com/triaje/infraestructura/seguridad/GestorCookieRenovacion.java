package com.triaje.infraestructura.seguridad;

import com.triaje.infraestructura.configuracion.PropiedadesSeguridad;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import java.time.Duration;

/**
 * Crea, lee y elimina la cookie del token de renovación (httpOnly, SameSite=Strict).
 */
@Component
public class GestorCookieRenovacion {

    private final PropiedadesSeguridad.Renovacion configuracion;

    public GestorCookieRenovacion(PropiedadesSeguridad propiedades) {
        this.configuracion = propiedades.renovacion();
    }

    public ResponseCookie crear(String valor, Duration duracion) {
        return construir(valor, duracion);
    }

    public ResponseCookie eliminar() {
        return construir("", Duration.ZERO);
    }

    public String leer(HttpServletRequest solicitud) {
        Cookie cookie = WebUtils.getCookie(solicitud, configuracion.nombreCookie());
        return cookie == null ? null : cookie.getValue();
    }

    private ResponseCookie construir(String valor, Duration duracion) {
        return ResponseCookie.from(configuracion.nombreCookie(), valor)
                .httpOnly(true)
                .secure(configuracion.cookieSegura())
                .sameSite("Strict")
                .path(configuracion.rutaCookie())
                .maxAge(duracion)
                .build();
    }
}
