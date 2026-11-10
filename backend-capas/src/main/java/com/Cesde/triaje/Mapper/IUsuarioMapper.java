package com.Cesde.triaje.Mapper;

import com.Cesde.triaje.Dto.RespuestaClinico;
import com.Cesde.triaje.Dto.RespuestaClinicoActivo;
import com.Cesde.triaje.Dto.RespuestaPacienteAdministracion;
import com.Cesde.triaje.Dto.RespuestaUsuario;
import com.Cesde.triaje.Modelo.MUsuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface IUsuarioMapper {

    @Mapping(target = "rol", expression = "java(usuario.getRol() != null ? usuario.getRol().name() : null)")
    RespuestaUsuario aRespuestaUsuario(MUsuario usuario);

    @Mapping(target = "rol", expression = "java(usuario.getRol() != null ? usuario.getRol().name() : null)")
    RespuestaClinico aRespuestaClinico(MUsuario usuario);

    RespuestaClinicoActivo aRespuestaClinicoActivo(MUsuario usuario);

    @Mapping(source = "nombreCompleto", target = "nombre")
    @Mapping(source = "fechaCreacion", target = "fechaRegistro")
    RespuestaPacienteAdministracion aRespuestaPacienteAdministracion(MUsuario usuario);

    List<RespuestaUsuario> aListaRespuestaUsuario(List<MUsuario> usuarios);

    List<RespuestaClinico> aListaRespuestaClinico(List<MUsuario> usuarios);

    List<RespuestaClinicoActivo> aListaRespuestaClinicoActivo(List<MUsuario> usuarios);

    List<RespuestaPacienteAdministracion> aListaRespuestaPacienteAdministracion(List<MUsuario> usuarios);
}
