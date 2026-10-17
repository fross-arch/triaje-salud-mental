package com.triaje.infraestructura.configuracion;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Propiedades de seguridad leídas de application.yml (prefijo triaje.seguridad).
 */
@ConfigurationProperties(prefix = "triaje.seguridad")
public record PropiedadesSeguridad(
        Jwt jwt,
        Renovacion renovacion,
        Cors cors,
        LimiteIntentos limiteIntentos,
        int bcryptFuerza) {

    public record Jwt(String secreto, String emisor, Duration duracionTokenAcceso) {
    }

    public record Renovacion(Duration duracion, String nombreCookie, String rutaCookie, boolean cookieSegura) {
    }

    public record Cors(List<String> origenesPermitidos) {
    }

    public record LimiteIntentos(int capacidadPorIp, int capacidadPorCorreoEIp, Duration periodo) {
    }
}
