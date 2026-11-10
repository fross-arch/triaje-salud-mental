package com.Cesde.triaje.Mapper;

import com.Cesde.triaje.Dto.RespuestaClinico;
import com.Cesde.triaje.Dto.RespuestaClinicoActivo;
import com.Cesde.triaje.Dto.RespuestaPacienteAdministracion;
import com.Cesde.triaje.Dto.RespuestaUsuario;
import com.Cesde.triaje.Modelo.MUsuario;

public final class UsuarioMapper {

    private UsuarioMapper() {
    }

    public static RespuestaUsuario aRespuestaUsuario(MUsuario usuario) {
        if (usuario == null) return null;
        return new RespuestaUsuario(
                usuario.getId(),
                usuario.getNombreCompleto(),
                usuario.getCorreo(),
                usuario.getRol().name(),
                usuario.getEspecialidad()
        );
    }

    public static RespuestaClinico aRespuestaClinico(MUsuario usuario) {
        if (usuario == null) return null;
        return new RespuestaClinico(
                usuario.getId(),
                usuario.getNombreCompleto(),
                usuario.getCorreo(),
                usuario.getRol().name(),
                usuario.isActivo(),
                usuario.getEspecialidad()
        );
    }

    public static RespuestaClinicoActivo aRespuestaClinicoActivo(MUsuario usuario) {
        if (usuario == null) return null;
        return new RespuestaClinicoActivo(
                usuario.getId(),
                usuario.getNombreCompleto(),
                usuario.getEspecialidad()
        );
    }

    public static RespuestaPacienteAdministracion aRespuestaPacienteAdministracion(MUsuario usuario) {
        if (usuario == null) return null;
        return new RespuestaPacienteAdministracion(
                usuario.getId(),
                usuario.getNombreCompleto(),
                usuario.getCorreo(),
                usuario.getFechaCreacion(),
                usuario.isActivo()
        );
    }
}
