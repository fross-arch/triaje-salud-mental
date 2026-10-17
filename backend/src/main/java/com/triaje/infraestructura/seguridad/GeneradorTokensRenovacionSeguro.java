package com.triaje.infraestructura.seguridad;

import com.triaje.aplicacion.puerto.GeneradorTokensRenovacion;
import com.triaje.infraestructura.configuracion.PropiedadesSeguridad;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Genera tokens de renovación opacos (256 bits aleatorios) y su hash SHA-256 para almacenarlos.
 */
@Component
public class GeneradorTokensRenovacionSeguro implements GeneradorTokensRenovacion {

    private static final int BYTES_TOKEN = 32;

    private final SecureRandom aleatorio = new SecureRandom();
    private final Duration duracion;

    public GeneradorTokensRenovacionSeguro(PropiedadesSeguridad propiedades) {
        this.duracion = propiedades.renovacion().duracion();
    }

    @Override
    public String generarValor() {
        byte[] bytes = new byte[BYTES_TOKEN];
        aleatorio.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public String calcularHash(String valor) {
        try {
            MessageDigest resumen = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(resumen.digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible en la JVM", e);
        }
    }

    @Override
    public Duration duracion() {
        return duracion;
    }
}
