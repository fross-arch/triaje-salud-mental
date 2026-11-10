package com.Cesde.triaje.Repositorio;

import com.Cesde.triaje.Modelo.Enumeracion.RolUsuario;
import com.Cesde.triaje.Modelo.MUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IUsuarioRepositorio extends JpaRepository<MUsuario, Long> {

    Optional<MUsuario> findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    List<MUsuario> findByRol(RolUsuario rol);

    List<MUsuario> findByRolAndActivoTrue(RolUsuario rol);

    List<IClinicoActivoProjection> findProyeccionByRolAndActivoTrue(RolUsuario rol);

    boolean existsByRol(RolUsuario rol);

    long countByRol(RolUsuario rol);

    @Query("SELECT u.fechaCreacion as fechaCreacion FROM MUsuario u WHERE u.rol = :rol AND u.fechaCreacion >= :desde ORDER BY u.fechaCreacion ASC")
    List<IFechaCreacionProjection> findFechasCreacionPorRolDesde(
            @Param("rol") RolUsuario rol,
            @Param("desde") java.time.Instant desde);

    @Query("SELECT u.fechaCreacion as fechaCreacion FROM MUsuario u WHERE u.rol = :rol ORDER BY u.fechaCreacion ASC")
    List<IFechaCreacionProjection> findFechasCreacionPorRol(
            @Param("rol") RolUsuario rol);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from MUsuario u where u.id = :id")
    int eliminarPorId(@Param("id") Long id);
}

