package com.triaje.aplicacion.casouso;

import com.triaje.aplicacion.dto.DatosConsentimiento;
import com.triaje.aplicacion.puerto.RepositorioConsentimientos;

import java.util.Objects;
import java.util.Optional;

/**
 * Consulta si el usuario autenticado cuenta con un consentimiento vigente.
 */
public class ConsultarConsentimientoVigente {

    private final RepositorioConsentimientos repositorioConsentimientos;

    public ConsultarConsentimientoVigente(RepositorioConsentimientos repositorioConsentimientos) {
        this.repositorioConsentimientos = Objects.requireNonNull(repositorioConsentimientos);
    }

    public Optional<DatosConsentimiento> ejecutar(Long usuarioId) {
        return repositorioConsentimientos.buscarVigentePorUsuarioId(usuarioId)
                .map(DatosConsentimiento::desde);
    }
}
