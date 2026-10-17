package com.triaje.infraestructura.persistencia.adaptador;

import com.triaje.aplicacion.puerto.RepositorioAuditoria;
import com.triaje.dominio.modelo.RegistroAuditoria;
import com.triaje.infraestructura.persistencia.entidad.RegistroAuditoriaEntidad;
import com.triaje.infraestructura.persistencia.repositorio.RegistroAuditoriaRepositorioJpa;
import org.springframework.stereotype.Component;

@Component
public class AdaptadorRepositorioAuditoria implements RepositorioAuditoria {

    private final RegistroAuditoriaRepositorioJpa repositorio;

    public AdaptadorRepositorioAuditoria(RegistroAuditoriaRepositorioJpa repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public void registrar(RegistroAuditoria registro) {
        repositorio.save(new RegistroAuditoriaEntidad(
                registro.usuarioId(), registro.accion().name(), registro.entidadId(), registro.fecha()));
    }
}
