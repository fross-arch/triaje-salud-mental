package com.triaje.infraestructura.tarea;

import com.triaje.aplicacion.casouso.CancelarCitasPropuestasVencidas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Tarea programada que identifica y cancela de forma automatica las citas en estado PROPUESTA
 * cuya fecha y hora haya expirado.
 */
@Component
public class TareaExpiracionCitas {

    private static final Logger REGISTRO = LoggerFactory.getLogger(TareaExpiracionCitas.class);

    private final CancelarCitasPropuestasVencidas casoUso;

    public TareaExpiracionCitas(CancelarCitasPropuestasVencidas casoUso) {
        this.casoUso = casoUso;
    }

    @Scheduled(cron = "${triaje.cron-citas-expiradas:0 */10 * * * *}")
    public void ejecutarExpiracion() {
        int canceladas = casoUso.ejecutar();
        if (canceladas > 0) {
            REGISTRO.info("Se cancelaron {} citas propuestas que expiraron por fecha pasada.", canceladas);
        }
    }
}
