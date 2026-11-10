package com.Cesde.triaje.Seguridad;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import java.time.Duration;

@Component
public class GestorCookieRenovacion {

    private final String nombreCookie;
    private final String rutaCookie;
    private final boolean cookieSegura;

    public GestorCookieRenovacion(
            @Value("${seguridad.renovacion.nombre-cookie:token_renovacion}") String nombreCookie,
            @Value("${seguridad.renovacion.ruta-cookie:/api/autenticacion}") String rutaCookie,
            @Value("${seguridad.renovacion.cookie-segura:false}") boolean cookieSegura
    ) {
        this.nombreCookie = nombreCookie;
        this.rutaCookie = rutaCookie;
        this.cookieSegura = cookieSegura;
    }

    public ResponseCookie crear(String valor, Duration duracion) {
        return construir(valor, duracion);
    }

    public ResponseCookie eliminar() {
        return construir("", Duration.ZERO);
    }

    public String leer(HttpServletRequest solicitud) {
        Cookie cookie = WebUtils.getCookie(solicitud, nombreCookie);
        return cookie == null ? null : cookie.getValue();
    }

    private ResponseCookie construir(String valor, Duration duracion) {
        return ResponseCookie.from(nombreCookie, valor)
                .httpOnly(true)
                .secure(cookieSegura)
                .sameSite("Strict")
                .path(rutaCookie)
                .maxAge(duracion)
                .build();
    }
}
