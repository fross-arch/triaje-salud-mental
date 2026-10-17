package com.triaje.infraestructura.persistencia.repositorio;

import com.triaje.infraestructura.persistencia.entidad.RegistroAuditoriaEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegistroAuditoriaRepositorioJpa extends JpaRepository<RegistroAuditoriaEntidad, Long> {

    List<RegistroAuditoriaEntidad> findByUsuarioIdAndAccion(Long usuarioId, String accion);

    List<RegistroAuditoriaEntidad> findByEntidadIdAndAccion(String entidadId, String accion);
}
