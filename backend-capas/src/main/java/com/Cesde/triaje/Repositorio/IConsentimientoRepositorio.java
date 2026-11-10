package com.Cesde.triaje.Repositorio;

import com.Cesde.triaje.Modelo.MConsentimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IConsentimientoRepositorio extends JpaRepository<MConsentimiento, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM MConsentimiento c WHERE c.usuario.id = :usuarioId")
    void deleteByUsuarioId(@Param("usuarioId") Long usuarioId);

    @Query("SELECT c FROM MConsentimiento c WHERE c.usuario.id = :usuarioId AND c.fechaRevocacion IS NULL")
    Optional<MConsentimiento> findByUsuarioIdAndFechaRevocacionIsNull(@Param("usuarioId") Long usuarioId);

    Optional<MConsentimiento> findFirstByUsuarioIdAndFechaRevocacionIsNullOrderByFechaAceptacionDesc(Long usuarioId);

    @Query("SELECT c FROM MConsentimiento c WHERE c.usuario.id = :usuarioId")
    List<MConsentimiento> findByUsuarioId(@Param("usuarioId") Long usuarioId);

    List<MConsentimiento> findByUsuarioIdOrderByFechaAceptacionDesc(Long usuarioId);
}
