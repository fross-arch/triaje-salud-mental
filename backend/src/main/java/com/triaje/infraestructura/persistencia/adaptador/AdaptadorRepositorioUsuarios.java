package com.triaje.infraestructura.persistencia.adaptador;

import com.triaje.aplicacion.puerto.RepositorioUsuarios;
import com.triaje.dominio.RolUsuario;
import com.triaje.dominio.modelo.Usuario;
import com.triaje.infraestructura.persistencia.entidad.UsuarioEntidad;
import com.triaje.infraestructura.persistencia.repositorio.UsuarioRepositorioJpa;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class AdaptadorRepositorioUsuarios implements RepositorioUsuarios {

    private final UsuarioRepositorioJpa repositorio;

    public AdaptadorRepositorioUsuarios(UsuarioRepositorioJpa repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public Usuario guardar(Usuario usuario) {
        UsuarioEntidad guardada = repositorio.save(new UsuarioEntidad(
                usuario.getId(), usuario.getNombreCompleto(), usuario.getCorreo(), usuario.getContrasenaHash(),
                usuario.getRol(), usuario.isActivo(), usuario.getFechaCreacion(), usuario.getEspecialidad()));
        return aDominio(guardada);
    }

    @Override
    public Optional<Usuario> buscarPorId(Long id) {
        return repositorio.findById(id).map(AdaptadorRepositorioUsuarios::aDominio);
    }

    @Override
    public Optional<Usuario> buscarPorCorreo(String correoNormalizado) {
        return repositorio.findByCorreo(correoNormalizado).map(AdaptadorRepositorioUsuarios::aDominio);
    }

    @Override
    public boolean existePorCorreo(String correoNormalizado) {
        return repositorio.existsByCorreo(correoNormalizado);
    }

    @Override
    public boolean existePorRol(RolUsuario rol) {
        return repositorio.existsByRol(rol);
    }

    @Override
    public List<Usuario> listarPorRol(RolUsuario rol) {
        return repositorio.findByRol(rol).stream()
                .map(AdaptadorRepositorioUsuarios::aDominio)
                .toList();
    }

    @Override
    public void eliminarPorId(Long id) {
        repositorio.eliminarPorId(id);
    }

    private static Usuario aDominio(UsuarioEntidad entidad) {
        return new Usuario(entidad.getId(), entidad.getNombreCompleto(), entidad.getCorreo(),
                entidad.getContrasenaHash(), entidad.getRol(), entidad.isActivo(), entidad.getFechaCreacion(),
                entidad.getEspecialidad());
    }
}
