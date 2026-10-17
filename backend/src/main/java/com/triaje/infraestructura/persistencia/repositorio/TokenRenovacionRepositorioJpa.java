package com.triaje.infraestructura.persistencia.repositorio;

import com.triaje.infraestructura.persistencia.entidad.TokenRenovacionEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface TokenRenovacionRepositorioJpa extends JpaRepository<TokenRenovacionEntidad, Long> {

    Optional<TokenRenovacionEntidad> findByHashToken(String hashToken);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update TokenRenovacionEntidad t set t.fechaRevocacion = :ahora "
            + "where t.usuarioId = :usuarioId and t.fechaRevocacion is null")
    int revocarTodosDelUsuario(@Param("usuarioId") Long usuarioId, @Param("ahora") Instant ahora);
}
