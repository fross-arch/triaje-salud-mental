package com.Cesde.triaje.Servicio;

import com.Cesde.triaje.Modelo.Enumeracion.AccionAuditoria;
import com.Cesde.triaje.Modelo.MRegistroAuditoria;
import com.Cesde.triaje.Modelo.MUsuario;
import com.Cesde.triaje.Repositorio.IRegistroAuditoriaRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class SAuditoria {

    private final IRegistroAuditoriaRepositorio auditoriaRepositorio;

    public SAuditoria(IRegistroAuditoriaRepositorio auditoriaRepositorio) {
        this.auditoriaRepositorio = Objects.requireNonNull(auditoriaRepositorio, "El repositorio de auditoría es obligatorio");
    }

    public void registrar(Long usuarioId, AccionAuditoria accion, String entidadId) {
        registrar(usuarioId, accion, entidadId, Instant.now());
    }

    public void registrar(Long usuarioId, AccionAuditoria accion, String entidadId, Instant fechaCreacion) {
        Objects.requireNonNull(accion, "La acción de auditoría es obligatoria");
        MRegistroAuditoria registro = new MRegistroAuditoria(
                usuarioId,
                accion,
                entidadId,
                fechaCreacion != null ? fechaCreacion : Instant.now()
        );
        auditoriaRepositorio.save(registro);
    }

    public void registrar(MUsuario usuario, AccionAuditoria accion, String entidadId) {
        registrar(usuario != null ? usuario.getId() : null, accion, entidadId, Instant.now());
    }

    public void registrar(AccionAuditoria accion, String entidadId) {
        registrar((Long) null, accion, entidadId, Instant.now());
    }

    @Transactional(readOnly = true)
    public List<MRegistroAuditoria> buscarPorUsuario(Long usuarioId) {
        return auditoriaRepositorio.findByUsuarioId(usuarioId);
    }

    @Transactional(readOnly = true)
    public List<MRegistroAuditoria> buscarPorUsuarioYAccion(Long usuarioId, AccionAuditoria accion) {
        return auditoriaRepositorio.findByUsuarioIdAndAccion(usuarioId, accion);
    }

    @Transactional(readOnly = true)
    public List<MRegistroAuditoria> buscarPorEntidadYAccion(String entidadId, AccionAuditoria accion) {
        return auditoriaRepositorio.findByEntidadIdAndAccion(entidadId, accion);
    }
}
