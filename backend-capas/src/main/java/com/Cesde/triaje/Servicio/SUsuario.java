package com.Cesde.triaje.Servicio;

import com.Cesde.triaje.Dto.RespuestaUsuario;
import com.Cesde.triaje.Dto.SolicitudActualizarPerfil;
import com.Cesde.triaje.Excepcion.ExcepcionCorreoYaRegistrado;
import com.Cesde.triaje.Excepcion.ExcepcionDatoInvalido;
import com.Cesde.triaje.Excepcion.ExcepcionUsuarioNoEncontrado;
import com.Cesde.triaje.Mapper.IUsuarioMapper;
import com.Cesde.triaje.Modelo.Enumeracion.AccionAuditoria;
import com.Cesde.triaje.Modelo.Enumeracion.RolUsuario;
import com.Cesde.triaje.Modelo.MRegistroAuditoria;
import com.Cesde.triaje.Modelo.MUsuario;
import com.Cesde.triaje.Regla.PoliticaContrasena;
import com.Cesde.triaje.Repositorio.IRegistroAuditoriaRepositorio;
import com.Cesde.triaje.Excepcion.ExcepcionDominio;
import com.Cesde.triaje.Modelo.Enumeracion.EstadoCita;
import com.Cesde.triaje.Repositorio.IAutorizacionRepositorio;
import com.Cesde.triaje.Repositorio.ICitaRepositorio;
import com.Cesde.triaje.Repositorio.IConsentimientoRepositorio;
import com.Cesde.triaje.Repositorio.IEvaluacionRepositorio;
import com.Cesde.triaje.Repositorio.IRegistroAuditoriaRepositorio;
import com.Cesde.triaje.Repositorio.IRespuestaEvaluacionRepositorio;
import com.Cesde.triaje.Repositorio.ITokenRenovacionRepositorio;
import com.Cesde.triaje.Repositorio.IUsuarioRepositorio;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class SUsuario {

    private final IUsuarioRepositorio usuarioRepositorio;
    private final ICitaRepositorio citaRepositorio;
    private final IAutorizacionRepositorio autorizacionRepositorio;
    private final IRespuestaEvaluacionRepositorio respuestaEvaluacionRepositorio;
    private final IEvaluacionRepositorio evaluacionRepositorio;
    private final IConsentimientoRepositorio consentimientoRepositorio;
    private final ITokenRenovacionRepositorio tokenRenovacionRepositorio;
    private final IRegistroAuditoriaRepositorio registroAuditoriaRepositorio;
    private final SAuditoria auditoriaServicio;
    private final PasswordEncoder passwordEncoder;
    private final IUsuarioMapper usuarioMapper;

    public SUsuario(
            IUsuarioRepositorio usuarioRepositorio,
            ICitaRepositorio citaRepositorio,
            IAutorizacionRepositorio autorizacionRepositorio,
            IRespuestaEvaluacionRepositorio respuestaEvaluacionRepositorio,
            IEvaluacionRepositorio evaluacionRepositorio,
            IConsentimientoRepositorio consentimientoRepositorio,
            ITokenRenovacionRepositorio tokenRenovacionRepositorio,
            IRegistroAuditoriaRepositorio registroAuditoriaRepositorio,
            SAuditoria auditoriaServicio,
            PasswordEncoder passwordEncoder,
            IUsuarioMapper usuarioMapper
    ) {
        this.usuarioRepositorio = usuarioRepositorio;
        this.citaRepositorio = citaRepositorio;
        this.autorizacionRepositorio = autorizacionRepositorio;
        this.respuestaEvaluacionRepositorio = respuestaEvaluacionRepositorio;
        this.evaluacionRepositorio = evaluacionRepositorio;
        this.consentimientoRepositorio = consentimientoRepositorio;
        this.tokenRenovacionRepositorio = tokenRenovacionRepositorio;
        this.registroAuditoriaRepositorio = registroAuditoriaRepositorio;
        this.auditoriaServicio = auditoriaServicio;
        this.passwordEncoder = passwordEncoder;
        this.usuarioMapper = usuarioMapper;
    }

    @Transactional(readOnly = true)
    public RespuestaUsuario obtenerPerfil(Long usuarioId) {
        MUsuario usuario = usuarioRepositorio.findById(usuarioId)
                .orElseThrow(ExcepcionUsuarioNoEncontrado::new);
        return usuarioMapper.aRespuestaUsuario(usuario);
    }

    public RespuestaUsuario actualizarPerfil(Long usuarioId, SolicitudActualizarPerfil solicitud) {
        MUsuario usuario = usuarioRepositorio.findById(usuarioId)
                .orElseThrow(ExcepcionUsuarioNoEncontrado::new);

        String correoNormalizado = solicitud.correo().trim().toLowerCase(Locale.ROOT);
        if (!correoNormalizado.equals(usuario.getCorreo()) && usuarioRepositorio.existsByCorreo(correoNormalizado)) {
            throw new ExcepcionCorreoYaRegistrado("El correo ya se encuentra registrado.");
        }

        usuario.setNombreCompleto(solicitud.nombreCompleto().trim());
        usuario.setCorreo(correoNormalizado);

        if (usuario.getRol() == RolUsuario.CLINICO) {
            usuario.setEspecialidad(solicitud.especialidad() != null ? solicitud.especialidad().trim() : null);
        }

        if (solicitud.nuevaContrasena() != null && !solicitud.nuevaContrasena().isBlank()) {
            if (solicitud.contrasenaActual() == null || solicitud.contrasenaActual().isBlank()
                    || !passwordEncoder.matches(solicitud.contrasenaActual(), usuario.getContrasenaHash())) {
                throw new ExcepcionDatoInvalido("La contraseña actual no coincide.");
            }
            PoliticaContrasena.validar(solicitud.nuevaContrasena());
            usuario.setContrasenaHash(passwordEncoder.encode(solicitud.nuevaContrasena()));
        }

        MUsuario guardado = usuarioRepositorio.save(usuario);
        auditoriaServicio.registrar(guardado, AccionAuditoria.PERFIL_ACTUALIZADO, String.valueOf(guardado.getId()));

        return usuarioMapper.aRespuestaUsuario(guardado);
    }

    public void eliminarCuenta(Long usuarioId) {
        if (!usuarioRepositorio.existsById(usuarioId)) {
            throw new ExcepcionUsuarioNoEncontrado();
        }
        if (citaRepositorio.existsByPacienteIdAndEstadoIn(usuarioId, List.of(EstadoCita.PROPUESTA, EstadoCita.CONFIRMADA))) {
            throw new ExcepcionDominio("No puedes eliminar tu cuenta mientras tengas una cita activa programada o propuesta. Debes completarla o cancelarla desde 'Mis Citas'.");
        }

        // Borrado explícito y transaccional en orden:
        // 1. tokens_renovacion
        tokenRenovacionRepositorio.deleteByUsuarioId(usuarioId);
        // 2. citas del paciente
        citaRepositorio.deleteByPacienteId(usuarioId);
        // 3. autorizaciones del paciente
        autorizacionRepositorio.deleteByPacienteId(usuarioId);
        // 4. respuestas_evaluacion
        respuestaEvaluacionRepositorio.deleteByUsuarioId(usuarioId);
        // 5. evaluaciones
        evaluacionRepositorio.deleteByUsuarioId(usuarioId);
        // 6. consentimientos
        consentimientoRepositorio.deleteByUsuarioId(usuarioId);
        // 7. auditoría: anonimizar usuario_id a null
        registroAuditoriaRepositorio.anonimizarUsuario(usuarioId);
        // 8. por último el usuario
        usuarioRepositorio.eliminarPorId(usuarioId);
        // 9. auditoría: registrar evento CUENTA_ELIMINADA con usuarioId=null y entidadId=String.valueOf(usuarioId)
        auditoriaServicio.registrar((Long) null, AccionAuditoria.CUENTA_ELIMINADA, String.valueOf(usuarioId));
    }
}
