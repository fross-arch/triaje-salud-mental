package com.triaje.aplicacion.puerto;

import com.triaje.dominio.RolUsuario;
import com.triaje.dominio.modelo.Usuario;

import java.util.List;
import java.util.Optional;

public interface RepositorioUsuarios {

    Usuario guardar(Usuario usuario);

    Optional<Usuario> buscarPorId(Long id);

    Optional<Usuario> buscarPorCorreo(String correoNormalizado);

    boolean existePorCorreo(String correoNormalizado);

    boolean existePorRol(RolUsuario rol);

    List<Usuario> listarPorRol(RolUsuario rol);

    /**
     * Elimina al usuario. La base de datos borra en cascada consentimientos, evaluaciones,
     * respuestas y tokens de renovación.
     */
    void eliminarPorId(Long id);
}
