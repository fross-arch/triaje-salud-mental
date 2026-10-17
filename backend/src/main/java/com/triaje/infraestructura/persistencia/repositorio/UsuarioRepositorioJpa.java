package com.triaje.infraestructura.persistencia.repositorio;

import com.triaje.infraestructura.persistencia.entidad.UsuarioEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.triaje.dominio.RolUsuario;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepositorioJpa extends JpaRepository<UsuarioEntidad, Long> {

    Optional<UsuarioEntidad> findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    boolean existsByRol(RolUsuario rol);

    List<UsuarioEntidad> findByRol(RolUsuario rol);

    /**
     * Borrado directo en SQL para que MySQL aplique las cascadas definidas en el esquema.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from UsuarioEntidad u where u.id = :id")
    int eliminarPorId(@Param("id") Long id);
}
