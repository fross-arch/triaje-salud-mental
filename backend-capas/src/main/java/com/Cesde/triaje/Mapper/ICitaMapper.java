package com.Cesde.triaje.Mapper;

import com.Cesde.triaje.Dto.RespuestaCitaClinico;
import com.Cesde.triaje.Dto.RespuestaCitaPaciente;
import com.Cesde.triaje.Modelo.MCita;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ICitaMapper {

    @Mapping(source = "clinico.nombreCompleto", target = "nombreClinico")
    RespuestaCitaPaciente aRespuestaCitaPaciente(MCita cita);

    @Mapping(source = "paciente.nombreCompleto", target = "nombrePaciente")
    RespuestaCitaClinico aRespuestaCitaClinico(MCita cita);

    List<RespuestaCitaPaciente> aListaRespuestaCitaPaciente(List<MCita> citas);

    List<RespuestaCitaClinico> aListaRespuestaCitaClinico(List<MCita> citas);
}
