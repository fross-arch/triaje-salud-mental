package com.triaje.infraestructura.persistencia.repositorio;

import com.triaje.infraestructura.persistencia.entidad.ConsentimientoEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConsentimientoRepositorioJpa extends JpaRepository<ConsentimientoEntidad, Long> {

    Optional<ConsentimientoEntidad> findFirstByUsuarioIdAndFechaRevocacionIsNullOrderByFechaAceptacionDesc(Long usuarioId);

    List<ConsentimientoEntidad> findByUsuarioIdOrderByFechaAceptacionDesc(Long usuarioId);
}
