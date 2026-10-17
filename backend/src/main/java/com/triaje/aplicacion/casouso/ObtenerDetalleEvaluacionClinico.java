package com.triaje.aplicacion.casouso;

import com.triaje.aplicacion.dto.DetalleEvaluacionClinico;
import com.triaje.aplicacion.dto.DetalleEvaluacionClinico.ItemRespuestaClinico;
import com.triaje.aplicacion.puerto.EjecutorTransaccional;
import com.triaje.aplicacion.puerto.RepositorioAuditoria;
import com.triaje.aplicacion.puerto.RepositorioAutorizaciones;
import com.triaje.aplicacion.puerto.RepositorioEvaluaciones;
import com.triaje.aplicacion.puerto.RepositorioRespuestasEvaluacion;
import com.triaje.aplicacion.puerto.RepositorioUsuarios;
import com.triaje.dominio.Autorizacion;
import com.triaje.dominio.PoliticaAccesoClinico;
import com.triaje.dominio.ReglaResultadoMinimo;
import com.triaje.dominio.excepcion.ExcepcionEvaluacionNoEncontrada;
import com.triaje.dominio.modelo.AccionAuditoria;
import com.triaje.dominio.modelo.CatalogoPreguntasCribado;
import com.triaje.dominio.modelo.Evaluacion;
import com.triaje.dominio.modelo.InstrumentoCribado;
import com.triaje.dominio.modelo.RegistroAuditoria;
import com.triaje.dominio.modelo.RespuestaEvaluacion;
import com.triaje.dominio.modelo.Usuario;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Caso de uso: el clinico consulta el detalle de una evaluacion.
 * Verifica estrictamente en la misma transaccion que exista autorizacion vigente;
 * si no existe, responde con 404 (ExcepcionEvaluacionNoEncontrada).
 * Nunca expone el correo electronico del paciente.
 */
public class ObtenerDetalleEvaluacionClinico {

    private final RepositorioAutorizaciones repositorioAutorizaciones;
    private final RepositorioEvaluaciones repositorioEvaluaciones;
    private final RepositorioUsuarios repositorioUsuarios;
    private final RepositorioRespuestasEvaluacion repositorioRespuestas;
    private final RepositorioAuditoria repositorioAuditoria;
    private final PoliticaAccesoClinico politicaAccesoClinico;
    private final ReglaResultadoMinimo reglaResultadoMinimo;
    private final EjecutorTransaccional transacciones;
    private final Clock reloj;

    public ObtenerDetalleEvaluacionClinico(RepositorioAutorizaciones repositorioAutorizaciones,
                                          RepositorioEvaluaciones repositorioEvaluaciones,
                                          RepositorioUsuarios repositorioUsuarios,
                                          RepositorioRespuestasEvaluacion repositorioRespuestas,
                                          RepositorioAuditoria repositorioAuditoria,
                                          PoliticaAccesoClinico politicaAccesoClinico,
                                          ReglaResultadoMinimo reglaResultadoMinimo,
                                          EjecutorTransaccional transacciones,
                                          Clock reloj) {
        this.repositorioAutorizaciones = Objects.requireNonNull(repositorioAutorizaciones);
        this.repositorioEvaluaciones = Objects.requireNonNull(repositorioEvaluaciones);
        this.repositorioUsuarios = Objects.requireNonNull(repositorioUsuarios);
        this.repositorioRespuestas = Objects.requireNonNull(repositorioRespuestas);
        this.repositorioAuditoria = Objects.requireNonNull(repositorioAuditoria);
        this.politicaAccesoClinico = Objects.requireNonNull(politicaAccesoClinico);
        this.reglaResultadoMinimo = Objects.requireNonNull(reglaResultadoMinimo);
        this.transacciones = Objects.requireNonNull(transacciones);
        this.reloj = Objects.requireNonNull(reloj);
    }

    public DetalleEvaluacionClinico ejecutar(String evaluacionId, Long clinicoId) {
        return transacciones.ejecutar(() -> {
            Optional<Autorizacion> autorizacionOpt = repositorioAutorizaciones
                    .buscarVigentePorEvaluacionYClinico(evaluacionId, clinicoId);

            if (!politicaAccesoClinico.permiteLectura(autorizacionOpt, clinicoId, evaluacionId)) {
                throw new ExcepcionEvaluacionNoEncontrada("La evaluacion no existe o no tienes acceso a ella.");
            }

            Evaluacion evaluacion = repositorioEvaluaciones.buscarPorId(evaluacionId)
                    .orElseThrow(() -> new ExcepcionEvaluacionNoEncontrada("La evaluacion no existe o no tienes acceso a ella."));

            Usuario paciente = repositorioUsuarios.buscarPorId(evaluacion.getUsuarioId())
                    .orElseThrow(() -> new ExcepcionEvaluacionNoEncontrada("La evaluacion no existe o no tienes acceso a ella."));

            List<RespuestaEvaluacion> respuestas = repositorioRespuestas.buscarPorEvaluacionId(evaluacionId);

            List<ItemRespuestaClinico> items = respuestas.stream()
                    .sorted((r1, r2) -> {
                        int c = r1.getInstrumento().compareTo(r2.getInstrumento());
                        return c != 0 ? c : Integer.compare(r1.getIndicePregunta(), r2.getIndicePregunta());
                    })
                    .map(r -> new ItemRespuestaClinico(
                            r.getInstrumento().name(),
                            r.getIndicePregunta(),
                            CatalogoPreguntasCribado.obtenerTextoPregunta(r.getInstrumento(), r.getIndicePregunta()),
                            r.getValorPuntaje(),
                            CatalogoPreguntasCribado.obtenerEtiquetaFrecuencia(r.getValorPuntaje())
                    ))
                    .toList();

            boolean resultadoMinimo = reglaResultadoMinimo.evaluar(
                    evaluacion.getSeveridadDepresion(),
                    evaluacion.getSeveridadAnsiedad(),
                    evaluacion.isRiesgoCrisis()
            );

            Instant ahora = Instant.now(reloj);
            repositorioAuditoria.registrar(new RegistroAuditoria(
                    clinicoId,
                    AccionAuditoria.ACCESO_CLINICO_DETALLE,
                    evaluacionId,
                    ahora
            ));

            return new DetalleEvaluacionClinico(
                    evaluacion.getId(),
                    paciente.getNombreCompleto(),
                    evaluacion.getFechaFinalizacion(),
                    evaluacion.getPuntajePhq9(),
                    evaluacion.getPuntajeGad7(),
                    evaluacion.getSeveridadDepresion(),
                    evaluacion.getSeveridadAnsiedad(),
                    evaluacion.isRiesgoCrisis(),
                    resultadoMinimo,
                    items
            );
        });
    }
}
