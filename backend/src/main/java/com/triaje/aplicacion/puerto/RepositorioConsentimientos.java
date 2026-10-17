package com.triaje.aplicacion.puerto;

import com.triaje.dominio.modelo.Consentimiento;

import java.util.List;
import java.util.Optional;

public interface RepositorioConsentimientos {

    Consentimiento guardar(Consentimiento consentimiento);

    Optional<Consentimiento> buscarVigentePorUsuarioId(Long usuarioId);

    List<Consentimiento> buscarPorUsuarioId(Long usuarioId);

    Optional<Consentimiento> buscarPorId(Long id);
}
