package com.triaje.aplicacion.casouso;

import com.triaje.aplicacion.dto.ResumenEvaluacionClinico;
import com.triaje.aplicacion.puerto.EjecutorTransaccional;
import com.triaje.aplicacion.puerto.RepositorioAuditoria;
import com.triaje.aplicacion.puerto.RepositorioAutorizaciones;
import com.triaje.aplicacion.puerto.RepositorioEvaluaciones;
import com.triaje.aplicacion.puerto.RepositorioUsuarios;
import com.triaje.dominio.Autorizacion;
import com.triaje.dominio.ReglaResultadoMinimo;
import com.triaje.dominio.modelo.AccionAuditoria;
import com.triaje.dominio.modelo.Evaluacion;
import com.triaje.dominio.modelo.RegistroAuditoria;
import com.triaje.dominio.modelo.Usuario;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Caso de uso: el clinico lista las evaluaciones para las cuales posee autorizacion vigente.
 * No genera alertas ni notificaciones automaticas.
 * Ordena primero las evaluaciones con riesgoCrisis = true y luego por fechaFinalizacion descendente.
 */
public class ListarEvaluacionesClinico {

    private final RepositorioAutorizaciones repositorioAutorizaciones;
    private final RepositorioEvaluaciones repositorioEvaluaciones;
    private final RepositorioUsuarios repositorioUsuarios;
    private final RepositorioAuditoria repositorioAuditoria;
    private final ReglaResultadoMinimo reglaResultadoMinimo;
    private final EjecutorTransaccional transacciones;
    private final Clock reloj;

    public ListarEvaluacionesClinico(RepositorioAutorizaciones repositorioAutorizaciones,
                                    RepositorioEvaluaciones repositorioEvaluaciones,
                                    RepositorioUsuarios repositorioUsuarios,
                                    RepositorioAuditoria repositorioAuditoria,
                                    ReglaResultadoMinimo reglaResultadoMinimo,
                                    EjecutorTransaccional transacciones,
                                    Clock reloj) {
        this.repositorioAutorizaciones = Objects.requireNonNull(repositorioAutorizaciones);
        this.repositorioEvaluaciones = Objects.requireNonNull(repositorioEvaluaciones);
        this.repositorioUsuarios = Objects.requireNonNull(repositorioUsuarios);
        this.repositorioAuditoria = Objects.requireNonNull(repositorioAuditoria);
        this.reglaResultadoMinimo = Objects.requireNonNull(reglaResultadoMinimo);
        this.transacciones = Objects.requireNonNull(transacciones);
        this.reloj = Objects.requireNonNull(reloj);
    }

    public List<ResumenEvaluacionClinico> ejecutar(Long clinicoId) {
        return transacciones.ejecutar(() -> {
            List<Autorizacion> autorizacionesVigentes = repositorioAutorizaciones.listarVigentesPorClinicoId(clinicoId);
            List<ResumenEvaluacionClinico> resultado = new ArrayList<>();

            for (Autorizacion autorizacion : autorizacionesVigentes) {
                Optional<Evaluacion> evaluacionOpt = repositorioEvaluaciones.buscarPorId(autorizacion.getEvaluacionId());
                if (evaluacionOpt.isEmpty()) {
                    continue;
                }
                Evaluacion evaluacion = evaluacionOpt.get();
                Optional<Usuario> pacienteOpt = repositorioUsuarios.buscarPorId(evaluacion.getUsuarioId());
                String nombrePaciente = pacienteOpt.map(Usuario::getNombreCompleto).orElse("Paciente");

                boolean resultadoMinimo = reglaResultadoMinimo.evaluar(
                        evaluacion.getSeveridadDepresion(),
                        evaluacion.getSeveridadAnsiedad(),
                        evaluacion.isRiesgoCrisis()
                );

                resultado.add(new ResumenEvaluacionClinico(
                        evaluacion.getId(),
                        nombrePaciente,
                        evaluacion.getFechaFinalizacion(),
                        evaluacion.getSeveridadDepresion(),
                        evaluacion.getSeveridadAnsiedad(),
                        evaluacion.isRiesgoCrisis(),
                        resultadoMinimo
                ));
            }

            Comparator<ResumenEvaluacionClinico> comparador = Comparator
                    .comparing(ResumenEvaluacionClinico::riesgoCrisis, Comparator.reverseOrder())
                    .thenComparing(ResumenEvaluacionClinico::fechaFinalizacion, Comparator.nullsLast(Comparator.reverseOrder()));

            resultado.sort(comparador);

            Instant ahora = Instant.now(reloj);
            repositorioAuditoria.registrar(new RegistroAuditoria(
                    clinicoId,
                    AccionAuditoria.ACCESO_CLINICO_LISTADO,
                    null,
                    ahora
            ));

            return resultado;
        });
    }
}
