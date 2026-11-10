package com.Cesde.triaje.Seguridad;

import com.Cesde.triaje.Excepcion.ExcepcionDemasiadosIntentos;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;

@Component
public class LimitadorIntentos {

    private final Cache<String, Bucket> cubosIp = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .maximumSize(10_000)
            .build();

    private final Cache<String, Bucket> cubosCorreoIp = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .maximumSize(20_000)
            .build();

    private final int capacidadPorIp;
    private final int capacidadPorCorreoEIp;

    public LimitadorIntentos() {
        this(20, 5);
    }

    public LimitadorIntentos(
            @org.springframework.beans.factory.annotation.Value("${triaje.seguridad.limite-intentos.capacidad-por-ip:20}") int capacidadPorIp,
            @org.springframework.beans.factory.annotation.Value("${triaje.seguridad.limite-intentos.capacidad-por-correo-e-ip:5}") int capacidadPorCorreoEIp
    ) {
        this.capacidadPorIp = capacidadPorIp;
        this.capacidadPorCorreoEIp = capacidadPorCorreoEIp;
    }

    public void limpiar() {
        cubosIp.invalidateAll();
        cubosCorreoIp.invalidateAll();
    }

    public void registrarIntentoPorIp(String ip) {
        String clave = ip == null ? "desconocida" : ip.trim();
        Bucket cubo = cubosIp.get(clave, k -> construirCubo(capacidadPorIp, Duration.ofMinutes(1)));
        consumir(cubo);
    }

    public void registrarIntento(String ip, String correo) {
        registrarIntentoPorIp(ip);
        String claveCorreo = (ip == null ? "" : ip.trim()) + "|" + (correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT));
        Bucket cubo = cubosCorreoIp.get(claveCorreo, k -> construirCubo(capacidadPorCorreoEIp, Duration.ofMinutes(1)));
        consumir(cubo);
    }

    private Bucket construirCubo(long capacidad, Duration periodo) {
        Bandwidth limite = Bandwidth.builder()
                .capacity(capacidad)
                .refillIntervally(capacidad, periodo)
                .build();
        return Bucket.builder().addLimit(limite).build();
    }

    private void consumir(Bucket cubo) {
        ConsumptionProbe sonda = cubo.tryConsumeAndReturnRemaining(1);
        if (!sonda.isConsumed()) {
            long segundosEspera = Math.max(1, Duration.ofNanos(sonda.getNanosToWaitForRefill()).toSeconds());
            throw new ExcepcionDemasiadosIntentos(segundosEspera);
        }
    }
}
