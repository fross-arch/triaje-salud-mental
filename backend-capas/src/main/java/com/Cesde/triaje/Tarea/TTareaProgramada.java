package com.Cesde.triaje.Tarea;

import com.Cesde.triaje.Modelo.Enumeracion.AccionAuditoria;
import com.Cesde.triaje.Modelo.Enumeracion.EstadoCita;
import com.Cesde.triaje.Modelo.Enumeracion.EstadoEvaluacion;
import com.Cesde.triaje.Modelo.MCita;
import com.Cesde.triaje.Modelo.MEvaluacion;
import com.Cesde.triaje.Repositorio.ICitaRepositorio;
import com.Cesde.triaje.Repositorio.IEvaluacionRepositorio;
import com.Cesde.triaje.Servicio.SAuditoria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class TTareaProgramada {

    private static final Logger REGISTRO = LoggerFactory.getLogger(TTareaProgramada.class);

    private final IEvaluacionRepositorio evaluacionRepositorio;
    private final ICitaRepositorio citaRepositorio;
    private final SAuditoria auditoriaServicio;

    public TTareaProgramada(
            IEvaluacionRepositorio evaluacionRepositorio,
            ICitaRepositorio citaRepositorio,
            SAuditoria auditoriaServicio) {
        this.evaluacionRepositorio = evaluacionRepositorio;
        this.citaRepositorio = citaRepositorio;
        this.auditoriaServicio = auditoriaServicio;
    }

    @Scheduled(cron = "${triaje.cron-evaluaciones-abandonadas:0 0 * * * *}")
    @Transactional
    public void abandonarEvaluacionesVencidas() {
        Instant hace24Horas = Instant.now().minus(24, ChronoUnit.HOURS);
        List<MEvaluacion> vencidas = evaluacionRepositorio.findByEstadoAndFechaCreacionBefore(
                EstadoEvaluacion.EN_CURSO,
                hace24Horas
        );

        if (!vencidas.isEmpty()) {
            for (MEvaluacion evaluacion : vencidas) {
                evaluacion.setEstado(EstadoEvaluacion.ABANDONADA);
            }
            evaluacionRepositorio.saveAll(vencidas);
            REGISTRO.info("Se marcaron {} evaluaciones vencidas como ABANDONADA.", vencidas.size());
        }
    }

    @Scheduled(cron = "${triaje.cron-citas-expiradas:0 */10 * * * *}")
    @Transactional
    public void cancelarCitasPropuestasVencidas() {
        Instant ahora = Instant.now();
        List<MCita> citasVencidas = citaRepositorio.findByEstadoAndFechaHoraBefore(
                EstadoCita.PROPUESTA,
                ahora
        );

        if (!citasVencidas.isEmpty()) {
            for (MCita cita : citasVencidas) {
                cita.setEstado(EstadoCita.CANCELADA);
                cita.setMotivoCancelacion("Cancelada automáticamente: tiempo de propuesta expirado.");
                auditoriaServicio.registrar(AccionAuditoria.CITA_CANCELADA, cita.getId());
            }
            citaRepositorio.saveAll(citasVencidas);
            REGISTRO.info("Se cancelaron {} citas propuestas que expiraron por fecha pasada.", citasVencidas.size());
        }
    }
}
