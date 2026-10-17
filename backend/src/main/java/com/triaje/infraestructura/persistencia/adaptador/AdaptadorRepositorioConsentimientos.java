package com.triaje.infraestructura.persistencia.adaptador;

import com.triaje.aplicacion.puerto.RepositorioConsentimientos;
import com.triaje.dominio.modelo.Consentimiento;
import com.triaje.infraestructura.persistencia.entidad.ConsentimientoEntidad;
import com.triaje.infraestructura.persistencia.repositorio.ConsentimientoRepositorioJpa;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class AdaptadorRepositorioConsentimientos implements RepositorioConsentimientos {

    private final ConsentimientoRepositorioJpa repositorio;

    public AdaptadorRepositorioConsentimientos(ConsentimientoRepositorioJpa repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public Consentimiento guardar(Consentimiento consentimiento) {
        ConsentimientoEntidad entidad = new ConsentimientoEntidad(
                consentimiento.getId(),
                consentimiento.getUsuarioId(),
                consentimiento.getVersionConsentimiento(),
                consentimiento.getVersionPolitica(),
                consentimiento.isConfirmaMayoriaEdad(),
                consentimiento.getFechaAceptacion(),
                consentimiento.getFechaRevocacion()
        );
        ConsentimientoEntidad guardada = repositorio.saveAndFlush(entidad);
        return aDominio(guardada);
    }

    @Override
    public Optional<Consentimiento> buscarVigentePorUsuarioId(Long usuarioId) {
        return repositorio.findFirstByUsuarioIdAndFechaRevocacionIsNullOrderByFechaAceptacionDesc(usuarioId)
                .map(AdaptadorRepositorioConsentimientos::aDominio);
    }

    @Override
    public List<Consentimiento> buscarPorUsuarioId(Long usuarioId) {
        return repositorio.findByUsuarioIdOrderByFechaAceptacionDesc(usuarioId).stream()
                .map(AdaptadorRepositorioConsentimientos::aDominio)
                .toList();
    }

    @Override
    public Optional<Consentimiento> buscarPorId(Long id) {
        return repositorio.findById(id).map(AdaptadorRepositorioConsentimientos::aDominio);
    }

    private static Consentimiento aDominio(ConsentimientoEntidad entidad) {
        return new Consentimiento(
                entidad.getId(),
                entidad.getUsuarioId(),
                entidad.getVersionConsentimiento(),
                entidad.getVersionPolitica(),
                entidad.isConfirmaMayoriaEdad(),
                entidad.getFechaAceptacion(),
                entidad.getFechaRevocacion()
        );
    }
}
