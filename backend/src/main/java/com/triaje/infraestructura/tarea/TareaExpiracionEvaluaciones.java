package com.triaje.infraestructura.tarea;

import com.triaje.aplicacion.casouso.AbandonarEvaluacionesVencidas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Tarea programada que marca como ABANDONADA aquellas evaluaciones en estado EN_CURSO
 * con mas de 24 horas de antiguedad, preservando intacto el riesgo de crisis.
 */
@Component
public class TareaExpiracionEvaluaciones {

    private static final Logger REGISTRO = LoggerFactory.getLogger(TareaExpiracionEvaluaciones.class);

    private final AbandonarEvaluacionesVencidas casoUso;

    public TareaExpiracionEvaluaciones(AbandonarEvaluacionesVencidas casoUso) {
        this.casoUso = casoUso;
    }

    @Scheduled(cron = "${triaje.cron-evaluaciones-abandonadas:0 0 * * * *}")
    public void ejecutarExpiracion() {
        int abandonadas = casoUso.ejecutar();
        if (abandonadas > 0) {
            REGISTRO.info("Se marcaron {} evaluaciones vencidas como ABANDONADA.", abandonadas);
        }
    }
}
