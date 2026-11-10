package com.Cesde.triaje.Repositorio;

import com.Cesde.triaje.Modelo.MTokenRenovacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface ITokenRenovacionRepositorio extends JpaRepository<MTokenRenovacion, Long> {

    Optional<MTokenRenovacion> findByHashToken(String hashToken);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM MTokenRenovacion t WHERE t.usuario.id = :usuarioId")
    void deleteByUsuarioId(@Param("usuarioId") Long usuarioId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE MTokenRenovacion t SET t.fechaRevocacion = :ahora WHERE t.usuario.id = :usuarioId AND t.fechaRevocacion IS NULL")
    int revocarTodosDelUsuario(@Param("usuarioId") Long usuarioId, @Param("ahora") Instant ahora);
}
