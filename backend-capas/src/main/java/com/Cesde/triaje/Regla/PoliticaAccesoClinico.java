package com.Cesde.triaje.Regla;

import com.Cesde.triaje.Modelo.MAutorizacion;

import java.util.Objects;
import java.util.Optional;

/**
 * Politica de dominio que valida si un clinico tiene permiso de lectura sobre una evaluacion.
 * Capa de reglas pura sin dependencias de infraestructura ni Spring.
 */
public class PoliticaAccesoClinico {

    /**
     * Valida si la autorizacion dada concede acceso de lectura al clinico sobre la evaluacion.
     * Requiere que la autorizacion exista, este vigente y coincida tanto el clinico como la evaluacion.
     */
    public boolean permiteLectura(MAutorizacion autorizacion, Long clinicoId, String evaluacionId) {
        if (autorizacion == null || clinicoId == null || evaluacionId == null) {
            return false;
        }
        return autorizacion.estaVigente()
                && Objects.equals(autorizacion.getClinicoId(), clinicoId)
                && Objects.equals(autorizacion.getEvaluacionId(), evaluacionId);
    }

    public boolean permiteLectura(Optional<MAutorizacion> autorizacionOpt, Long clinicoId, String evaluacionId) {
        if (autorizacionOpt == null || autorizacionOpt.isEmpty()) {
            return false;
        }
        return permiteLectura(autorizacionOpt.get(), clinicoId, evaluacionId);
    }
}
