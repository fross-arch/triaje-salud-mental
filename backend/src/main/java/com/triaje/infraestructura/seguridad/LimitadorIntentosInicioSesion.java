package com.triaje.infraestructura.seguridad;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.triaje.infraestructura.configuracion.PropiedadesSeguridad;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Limita los intentos de peticiones usando cubetas de tokens (Bucket4j) almacenadas
 * en una cache Caffeine con caducidad por inactividad de 10 minutos y maximo 10,000 entradas.
 *
 * Aplica dos niveles de restriccion:
 * 1) Limite por IP (solo request.getRemoteAddr()): protege contra ataques masivos de una misma IP.
 * 2) Limite por clave combinada IP + Correo normalizado hasheado: evita que un atacante desde otra IP
 *    pueda denegar el servicio al correo de una persona legitima.
 */
@Component
public class LimitadorIntentosInicioSesion {

    private final Cache<String, Bucket> cubetasIp = Caffeine.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();

    private final Cache<String, Bucket> cubetasCorreoEIp = Caffeine.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();

    private final int capacidadPorIp;
    private final int capacidadPorCorreoEIp;
    private final Duration periodo;

    public LimitadorIntentosInicioSesion(PropiedadesSeguridad propiedades) {
        this.capacidadPorIp = propiedades.limiteIntentos().capacidadPorIp();
        this.capacidadPorCorreoEIp = propiedades.limiteIntentos().capacidadPorCorreoEIp();
        this.periodo = propiedades.limiteIntentos().periodo();
    }

    /**
     * Limita peticiones evaluando unicamente la IP del cliente (para registro o renovacion).
     */
    public void registrarIntentoPorIp(String ip) {
        String claveIp = normalizarIp(ip);
        Bucket cubeta = cubetasIp.get(claveIp, k -> crearCubeta(capacidadPorIp));
        ConsumptionProbe sonda = cubeta.tryConsumeAndReturnRemaining(1);
        if (!sonda.isConsumed()) {
            throw new ExcepcionDemasiadosIntentos(segundosEspera(sonda));
        }
    }

    /**
     * Limita intentos de inicio de sesion evaluando tanto la IP como la combinacion IP + Correo.
     */
    public void registrarIntento(String ip, String correo) {
        registrarIntentoPorIp(ip);

        String claveIp = normalizarIp(ip);
        String claveCombinada = claveIp + "|" + resumirCorreo(correo);

        Bucket cubetaCombinada = cubetasCorreoEIp.get(claveCombinada, k -> crearCubeta(capacidadPorCorreoEIp));
        ConsumptionProbe sonda = cubetaCombinada.tryConsumeAndReturnRemaining(1);
        if (!sonda.isConsumed()) {
            throw new ExcepcionDemasiadosIntentos(segundosEspera(sonda));
        }
    }

    private Bucket crearCubeta(int capacidad) {
        return Bucket.builder()
                .addLimit(limite -> limite.capacity(capacidad).refillGreedy(capacidad, periodo))
                .build();
    }

    private static String normalizarIp(String ip) {
        return (ip == null || ip.isBlank()) ? "127.0.0.1" : ip.trim();
    }

    private static long segundosEspera(ConsumptionProbe sonda) {
        return Math.max(1, Duration.ofNanos(sonda.getNanosToWaitForRefill()).toSeconds() + 1);
    }

    private static String resumirCorreo(String correo) {
        String normalizado = (correo == null) ? "" : correo.trim().toLowerCase(Locale.ROOT);
        try {
            MessageDigest resumen = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(resumen.digest(normalizado.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no esta disponible en la JVM", e);
        }
    }

    /**
     * Se supero el limite permitido de intentos.
     */
    public static class ExcepcionDemasiadosIntentos extends RuntimeException {

        private final long segundosEspera;

        public ExcepcionDemasiadosIntentos(long segundosEspera) {
            super("Demasiados intentos de inicio de sesion.");
            this.segundosEspera = segundosEspera;
        }

        public long getSegundosEspera() {
            return segundosEspera;
        }
    }
}
