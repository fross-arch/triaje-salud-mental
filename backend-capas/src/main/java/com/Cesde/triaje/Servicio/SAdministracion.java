package com.Cesde.triaje.Servicio;

import com.Cesde.triaje.Dto.RespuestaClinico;
import com.Cesde.triaje.Dto.RespuestaPacienteAdministracion;
import com.Cesde.triaje.Dto.SolicitudActualizarClinico;
import com.Cesde.triaje.Dto.SolicitudActualizarPaciente;
import com.Cesde.triaje.Dto.SolicitudCrearClinico;
import com.Cesde.triaje.Excepcion.ExcepcionCorreoYaRegistrado;
import com.Cesde.triaje.Excepcion.ExcepcionDominio;
import com.Cesde.triaje.Excepcion.ExcepcionUsuarioNoEncontrado;
import com.Cesde.triaje.Mapper.IUsuarioMapper;
import com.Cesde.triaje.Modelo.Enumeracion.AccionAuditoria;
import com.Cesde.triaje.Modelo.Enumeracion.RolUsuario;
import com.Cesde.triaje.Modelo.MRegistroAuditoria;
import com.Cesde.triaje.Modelo.MUsuario;
import com.Cesde.triaje.Regla.PoliticaContrasena;
import com.Cesde.triaje.Repositorio.IAutorizacionRepositorio;
import com.Cesde.triaje.Repositorio.ICitaRepositorio;
import com.Cesde.triaje.Repositorio.IConsentimientoRepositorio;
import com.Cesde.triaje.Repositorio.IRegistroAuditoriaRepositorio;
import com.Cesde.triaje.Repositorio.ITokenRenovacionRepositorio;
import com.Cesde.triaje.Repositorio.IUsuarioRepositorio;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class SAdministracion {

    private final IUsuarioRepositorio usuarioRepositorio;
    private final ICitaRepositorio citaRepositorio;
    private final IAutorizacionRepositorio autorizacionRepositorio;
    private final ITokenRenovacionRepositorio tokenRenovacionRepositorio;
    private final IConsentimientoRepositorio consentimientoRepositorio;
    private final SAuditoria auditoriaServicio;
    private final PasswordEncoder codificadorContrasenas;
    private final IUsuarioMapper usuarioMapper;

    public SAdministracion(
            IUsuarioRepositorio usuarioRepositorio,
            ICitaRepositorio citaRepositorio,
            IAutorizacionRepositorio autorizacionRepositorio,
            ITokenRenovacionRepositorio tokenRenovacionRepositorio,
            IConsentimientoRepositorio consentimientoRepositorio,
            SAuditoria auditoriaServicio,
            PasswordEncoder codificadorContrasenas,
            IUsuarioMapper usuarioMapper) {
        this.usuarioRepositorio = usuarioRepositorio;
        this.citaRepositorio = citaRepositorio;
        this.autorizacionRepositorio = autorizacionRepositorio;
        this.tokenRenovacionRepositorio = tokenRenovacionRepositorio;
        this.consentimientoRepositorio = consentimientoRepositorio;
        this.auditoriaServicio = auditoriaServicio;
        this.codificadorContrasenas = codificadorContrasenas;
        this.usuarioMapper = usuarioMapper;
    }

    @Transactional(readOnly = true)
    public List<RespuestaClinico> listarClinicos() {
        return usuarioRepositorio.findByRol(RolUsuario.CLINICO).stream()
                .map(usuarioMapper::aRespuestaClinico)
                .toList();
    }

    public RespuestaClinico crearClinico(SolicitudCrearClinico solicitud, Long adminId) {
        String correoNormalizado = normalizarCorreo(solicitud.correo());
        if (usuarioRepositorio.existsByCorreo(correoNormalizado)) {
            throw new ExcepcionCorreoYaRegistrado("El correo electrónico ya está registrado en el sistema.");
        }

        PoliticaContrasena.validar(solicitud.contrasenaInicial());

        MUsuario nuevo = new MUsuario(
                solicitud.nombreCompleto().trim(),
                correoNormalizado,
                codificadorContrasenas.encode(solicitud.contrasenaInicial()),
                RolUsuario.CLINICO
        );
        nuevo.setEspecialidad(solicitud.especialidad() != null ? solicitud.especialidad().trim() : null);
        nuevo.setActivo(true);
        MUsuario guardado = usuarioRepositorio.save(nuevo);

        auditoriaServicio.registrar(
                adminId,
                AccionAuditoria.CLINICO_CREADO,
                String.valueOf(guardado.getId())
        );

        return usuarioMapper.aRespuestaClinico(guardado);
    }

    public RespuestaClinico actualizarClinico(Long id, SolicitudActualizarClinico solicitud, Long adminId) {
        MUsuario clinico = usuarioRepositorio.findById(id)
                .orElseThrow(() -> new ExcepcionUsuarioNoEncontrado("El clínico no existe."));

        if (clinico.getRol() != RolUsuario.CLINICO) {
            throw new ExcepcionUsuarioNoEncontrado("El usuario especificado no es un clínico.");
        }

        String correoNormalizado = normalizarCorreo(solicitud.correo());
        if (!correoNormalizado.equals(clinico.getCorreo()) && usuarioRepositorio.existsByCorreo(correoNormalizado)) {
            throw new ExcepcionCorreoYaRegistrado("El correo electrónico ya está registrado en el sistema.");
        }

        clinico.setNombreCompleto(solicitud.nombreCompleto().trim());
        clinico.setCorreo(correoNormalizado);
        clinico.setEspecialidad(solicitud.especialidad() != null ? solicitud.especialidad().trim() : null);

        if (solicitud.nuevaContrasena() != null && !solicitud.nuevaContrasena().isBlank()) {
            PoliticaContrasena.validar(solicitud.nuevaContrasena());
            clinico.setContrasenaHash(codificadorContrasenas.encode(solicitud.nuevaContrasena()));
        }

        MUsuario guardado = usuarioRepositorio.save(clinico);

        auditoriaServicio.registrar(
                adminId,
                AccionAuditoria.CLINICO_ACTUALIZADO,
                String.valueOf(guardado.getId())
        );

        return usuarioMapper.aRespuestaClinico(guardado);
    }

    public void eliminarClinico(Long id, Long adminId) {
        MUsuario clinico = usuarioRepositorio.findById(id)
                .orElseThrow(() -> new ExcepcionUsuarioNoEncontrado("El clínico no existe."));

        if (clinico.getRol() != RolUsuario.CLINICO) {
            throw new ExcepcionUsuarioNoEncontrado("El usuario especificado no es un clínico.");
        }

        if (citaRepositorio.existsByClinicoId(id)) {
            throw new ExcepcionDominio("No es posible eliminar el profesional clínico porque cuenta con citas asociadas en su historial. Se recomienda desactivar la cuenta en su lugar.");
        }

        if (autorizacionRepositorio.existsByClinicoId(id)) {
            throw new ExcepcionDominio("No es posible eliminar el profesional clínico porque cuenta con autorizaciones de acceso asociadas en su historial. Se recomienda desactivar la cuenta en su lugar.");
        }

        tokenRenovacionRepositorio.deleteByUsuarioId(id);
        consentimientoRepositorio.deleteByUsuarioId(id);
        usuarioRepositorio.eliminarPorId(id);

        auditoriaServicio.registrar(
                adminId,
                AccionAuditoria.CLINICO_ELIMINADO,
                String.valueOf(id)
        );
    }

    public RespuestaClinico cambiarEstadoClinico(Long id, boolean activo, Long adminId) {
        MUsuario clinico = usuarioRepositorio.findById(id)
                .orElseThrow(() -> new ExcepcionUsuarioNoEncontrado("El clínico no existe."));

        if (clinico.getRol() != RolUsuario.CLINICO) {
            throw new ExcepcionUsuarioNoEncontrado("El usuario especificado no es un clínico.");
        }

        clinico.setActivo(activo);
        MUsuario guardado = usuarioRepositorio.save(clinico);

        auditoriaServicio.registrar(
                adminId,
                activo ? AccionAuditoria.CLINICO_ACTIVADO : AccionAuditoria.CLINICO_DESACTIVADO,
                String.valueOf(guardado.getId())
        );

        return usuarioMapper.aRespuestaClinico(guardado);
    }

    @Transactional(readOnly = true)
    public List<RespuestaPacienteAdministracion> listarPacientes() {
        return usuarioRepositorio.findByRol(RolUsuario.PACIENTE).stream()
                .map(usuarioMapper::aRespuestaPacienteAdministracion)
                .toList();
    }

    public RespuestaPacienteAdministracion actualizarPaciente(Long id, SolicitudActualizarPaciente solicitud, Long adminId) {
        MUsuario paciente = usuarioRepositorio.findById(id)
                .orElseThrow(() -> new ExcepcionUsuarioNoEncontrado("El paciente no existe."));

        if (paciente.getRol() != RolUsuario.PACIENTE) {
            throw new ExcepcionUsuarioNoEncontrado("El usuario especificado no es un paciente.");
        }

        String correoNormalizado = normalizarCorreo(solicitud.correo());
        if (!correoNormalizado.equals(paciente.getCorreo()) && usuarioRepositorio.existsByCorreo(correoNormalizado)) {
            throw new ExcepcionCorreoYaRegistrado("El correo electrónico ya está registrado en el sistema.");
        }

        paciente.setNombreCompleto(solicitud.nombreCompleto().trim());
        paciente.setCorreo(correoNormalizado);

        if (solicitud.nuevaContrasena() != null && !solicitud.nuevaContrasena().isBlank()) {
            PoliticaContrasena.validar(solicitud.nuevaContrasena());
            paciente.setContrasenaHash(codificadorContrasenas.encode(solicitud.nuevaContrasena()));
        }

        MUsuario guardado = usuarioRepositorio.save(paciente);

        auditoriaServicio.registrar(
                adminId,
                AccionAuditoria.PACIENTE_ACTUALIZADO,
                String.valueOf(guardado.getId())
        );

        return usuarioMapper.aRespuestaPacienteAdministracion(guardado);
    }

    private String normalizarCorreo(String correo) {
        return correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
    }
}
