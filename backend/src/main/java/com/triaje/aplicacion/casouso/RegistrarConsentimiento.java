package com.triaje.aplicacion.casouso;

import com.triaje.aplicacion.dto.ComandoConsentimiento;
import com.triaje.aplicacion.dto.DatosConsentimiento;
import com.triaje.aplicacion.puerto.EjecutorTransaccional;
import com.triaje.aplicacion.puerto.RepositorioAuditoria;
import com.triaje.aplicacion.puerto.RepositorioConsentimientos;
import com.triaje.dominio.excepcion.ExcepcionDatoInvalido;
import com.triaje.dominio.modelo.AccionAuditoria;
import com.triaje.dominio.modelo.Consentimiento;
import com.triaje.dominio.modelo.RegistroAuditoria;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * Registra la aceptacion del consentimiento informado y la politica de privacidad.
 */
public class RegistrarConsentimiento {

    private final RepositorioConsentimientos repositorioConsentimientos;
    private final RepositorioAuditoria repositorioAuditoria;
    private final EjecutorTransaccional transacciones;
    private final Clock reloj;

    public RegistrarConsentimiento(RepositorioConsentimientos repositorioConsentimientos,
                                   RepositorioAuditoria repositorioAuditoria,
                                   EjecutorTransaccional transacciones,
                                   Clock reloj) {
        this.repositorioConsentimientos = Objects.requireNonNull(repositorioConsentimientos);
        this.repositorioAuditoria = Objects.requireNonNull(repositorioAuditoria);
        this.transacciones = Objects.requireNonNull(transacciones);
        this.reloj = Objects.requireNonNull(reloj);
    }

    public DatosConsentimiento ejecutar(ComandoConsentimiento comando) {
        if (!comando.confirmaMayoriaEdad()) {
            throw new ExcepcionDatoInvalido("Debes confirmar la mayoría de edad para otorgar consentimiento.");
        }

        return transacciones.ejecutar(() -> {
            Instant ahora = Instant.now(reloj);

            // Si habia un consentimiento vigente anterior, se revoca antes de registrar el nuevo
            repositorioConsentimientos.buscarVigentePorUsuarioId(comando.usuarioId())
                    .ifPresent(previo -> {
                        previo.revocar(ahora);
                        repositorioConsentimientos.guardar(previo);
                    });

            Consentimiento nuevo = Consentimiento.nuevo(comando.usuarioId(), ahora);
            Consentimiento guardado = repositorioConsentimientos.guardar(nuevo);

            repositorioAuditoria.registrar(new RegistroAuditoria(
                    comando.usuarioId(),
                    AccionAuditoria.CONSENTIMIENTO_ACEPTADO,
                    String.valueOf(guardado.getId()),
                    ahora
            ));

            return DatosConsentimiento.desde(guardado);
        });
    }
}
