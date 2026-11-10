package com.Cesde.triaje.Repositorio;

import com.Cesde.triaje.Modelo.Enumeracion.AccionAuditoria;
import com.Cesde.triaje.Modelo.MRegistroAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Repository
public interface IRegistroAuditoriaRepositorio extends JpaRepository<MRegistroAuditoria, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE MRegistroAuditoria r SET r.usuarioId = NULL WHERE r.usuarioId = :usuarioId")
    int anonimizarUsuario(@Param("usuarioId") Long usuarioId);

    List<MRegistroAuditoria> findByUsuarioId(Long usuarioId);

    List<MRegistroAuditoria> findByUsuarioIdAndAccion(Long usuarioId, AccionAuditoria accion);

    List<MRegistroAuditoria> findByEntidadIdAndAccion(String entidadId, AccionAuditoria accion);

    @Query("SELECT r.accion as accion, r.fechaCreacion as fechaCreacion FROM MRegistroAuditoria r WHERE r.accion IN :acciones AND r.fechaCreacion >= :desde ORDER BY r.fechaCreacion ASC")
    List<IEventoActividadProjection> findActividadDesde(
            @Param("acciones") Collection<AccionAuditoria> acciones,
            @Param("desde") Instant desde);

    @Query("SELECT r.accion as accion, r.fechaCreacion as fechaCreacion FROM MRegistroAuditoria r WHERE r.accion IN :acciones ORDER BY r.fechaCreacion ASC")
    List<IEventoActividadProjection> findActividadHistorica(
            @Param("acciones") Collection<AccionAuditoria> acciones);
}

