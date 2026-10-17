package com.triaje.aplicacion.casouso;

import com.triaje.aplicacion.puerto.EjecutorTransaccional;
import com.triaje.aplicacion.puerto.RepositorioAuditoria;
import com.triaje.aplicacion.puerto.RepositorioCitas;
import com.triaje.dominio.Cita;
import com.triaje.dominio.MaquinaEstadosCita;
import com.triaje.dominio.modelo.AccionAuditoria;
import com.triaje.dominio.modelo.RegistroAuditoria;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Caso de uso: identifica y cancela de forma automatica las citas en estado PROPUESTA
 * cuya fecha y hora haya expirado (fecha pasada).
 */
public class CancelarCitasPropuestasVencidas {

    private final RepositorioCitas repositorioCitas;
    private final RepositorioAuditoria repositorioAuditoria;
    private final MaquinaEstadosCita maquinaEstadosCita;
    private final EjecutorTransaccional transacciones;
    private final Clock reloj;

    public CancelarCitasPropuestasVencidas(RepositorioCitas repositorioCitas,
                                          RepositorioAuditoria repositorioAuditoria,
                                          MaquinaEstadosCita maquinaEstadosCita,
                                          EjecutorTransaccional transacciones,
                                          Clock reloj) {
        this.repositorioCitas = Objects.requireNonNull(repositorioCitas);
        this.repositorioAuditoria = Objects.requireNonNull(repositorioAuditoria);
        this.maquinaEstadosCita = Objects.requireNonNull(maquinaEstadosCita);
        this.transacciones = Objects.requireNonNull(transacciones);
        this.reloj = Objects.requireNonNull(reloj);
    }

    public int ejecutar() {
        return transacciones.ejecutar(() -> {
            Instant ahora = Instant.now(reloj);
            List<Cita> vencidas = repositorioCitas.listarPropuestasConFechaHoraMenorA(ahora);

            for (Cita cita : vencidas) {
                Cita cancelada = cita.cancelar(null, ahora, maquinaEstadosCita);
                repositorioCitas.guardar(cancelada);

                repositorioAuditoria.registrar(new RegistroAuditoria(
                        null,
                        AccionAuditoria.CITA_CANCELADA,
                        cancelada.getId(),
                        ahora
                ));
            }

            return vencidas.size();
        });
    }
}
