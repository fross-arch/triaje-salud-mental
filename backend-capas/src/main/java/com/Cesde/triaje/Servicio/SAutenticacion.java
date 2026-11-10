package com.Cesde.triaje.Servicio;

import com.Cesde.triaje.Dto.RespuestaUsuario;
import com.Cesde.triaje.Dto.SolicitudInicioSesion;
import com.Cesde.triaje.Dto.SolicitudRegistro;
import com.Cesde.triaje.Excepcion.ExcepcionCorreoYaRegistrado;
import com.Cesde.triaje.Excepcion.ExcepcionCredencialesInvalidas;
import com.Cesde.triaje.Excepcion.ExcepcionTokenRenovacionInvalido;
import com.Cesde.triaje.Mapper.IUsuarioMapper;
import com.Cesde.triaje.Modelo.Enumeracion.AccionAuditoria;
import com.Cesde.triaje.Modelo.Enumeracion.RolUsuario;
import com.Cesde.triaje.Modelo.MRegistroAuditoria;
import com.Cesde.triaje.Modelo.MTokenRenovacion;
import com.Cesde.triaje.Modelo.MUsuario;
import com.Cesde.triaje.Regla.PoliticaContrasena;
import com.Cesde.triaje.Repositorio.IRegistroAuditoriaRepositorio;
import com.Cesde.triaje.Repositorio.ITokenRenovacionRepositorio;
import com.Cesde.triaje.Repositorio.IUsuarioRepositorio;
import com.Cesde.triaje.Seguridad.ProveedorTokensJwt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;

@Service
@Transactional
public class SAutenticacion {

    private final IUsuarioRepositorio usuarioRepositorio;
    private final ITokenRenovacionRepositorio tokenRenovacionRepositorio;
    private final SAuditoria auditoriaServicio;
    private final PasswordEncoder passwordEncoder;
    private final ProveedorTokensJwt proveedorTokensJwt;
    private final IUsuarioMapper usuarioMapper;
    private final Duration duracionRenovacion;
    private final SecureRandom generadorAleatorio = new SecureRandom();

    public SAutenticacion(
            IUsuarioRepositorio usuarioRepositorio,
            ITokenRenovacionRepositorio tokenRenovacionRepositorio,
            SAuditoria auditoriaServicio,
            PasswordEncoder passwordEncoder,
            ProveedorTokensJwt proveedorTokensJwt,
            IUsuarioMapper usuarioMapper,
            @Value("${seguridad.renovacion.duracion-dias:7}") long duracionDias
    ) {
        this.usuarioRepositorio = usuarioRepositorio;
        this.tokenRenovacionRepositorio = tokenRenovacionRepositorio;
        this.auditoriaServicio = auditoriaServicio;
        this.passwordEncoder = passwordEncoder;
        this.proveedorTokensJwt = proveedorTokensJwt;
        this.usuarioMapper = usuarioMapper;
        this.duracionRenovacion = Duration.ofDays(duracionDias);
    }

    public record ResultadoSesion(String tokenAcceso, long expiraEnSegundos, String tokenRenovacion, Duration duracionRenovacion) {
    }

    private static final String HASH_FICTICIO = "$2a$12$e80yVvU.yQkLp7gq6vMv.uKzVb19DkJZ9b8gL/a1PZ3n78k9wE2.u";

    public RespuestaUsuario registrar(SolicitudRegistro solicitud) {
        PoliticaContrasena.validar(solicitud.contrasena());
        String correoNormalizado = solicitud.correo().trim().toLowerCase(Locale.ROOT);

        if (usuarioRepositorio.existsByCorreo(correoNormalizado)) {
            throw new ExcepcionCorreoYaRegistrado("El correo ya se encuentra registrado.");
        }

        MUsuario nuevo = new MUsuario(
                solicitud.nombreCompleto().trim(),
                correoNormalizado,
                passwordEncoder.encode(solicitud.contrasena()),
                RolUsuario.PACIENTE
        );
        MUsuario guardado = usuarioRepositorio.save(nuevo);

        return usuarioMapper.aRespuestaUsuario(guardado);
    }

    public ResultadoSesion iniciarSesion(SolicitudInicioSesion solicitud) {
        if (solicitud.contrasena() == null || solicitud.contrasena().isEmpty()) {
            throw new ExcepcionCredencialesInvalidas();
        }

        String correoNormalizado = solicitud.correo() == null ? "" : solicitud.correo().trim().toLowerCase(Locale.ROOT);
        var usuarioOpt = usuarioRepositorio.findByCorreo(correoNormalizado);

        if (usuarioOpt.isEmpty()) {
            // Protección contra timing attack anti-enumeración
            passwordEncoder.matches(solicitud.contrasena(), HASH_FICTICIO);
            throw new ExcepcionCredencialesInvalidas();
        }

        MUsuario usuario = usuarioOpt.get();

        if (!passwordEncoder.matches(solicitud.contrasena(), usuario.getContrasenaHash()) || !usuario.isActivo()) {
            throw new ExcepcionCredencialesInvalidas();
        }

        var tokenAcceso = proveedorTokensJwt.generar(usuario);
        String tokenRenovacionPlano = generarTokenAleatorio();
        String hashToken = sha256(tokenRenovacionPlano);

        Instant ahora = Instant.now();
        MTokenRenovacion entidadToken = new MTokenRenovacion(
                usuario,
                hashToken,
                ahora.plus(duracionRenovacion)
        );
        tokenRenovacionRepositorio.save(entidadToken);

        auditoriaServicio.registrar(usuario, AccionAuditoria.INICIO_SESION, String.valueOf(usuario.getId()));

        return new ResultadoSesion(tokenAcceso.token(), tokenAcceso.expiraEnSegundos(), tokenRenovacionPlano, duracionRenovacion);
    }

    @Transactional(noRollbackFor = ExcepcionTokenRenovacionInvalido.class)
    public ResultadoSesion renovar(String tokenRenovacionPlano) {
        if (tokenRenovacionPlano == null || tokenRenovacionPlano.isBlank()) {
            throw new ExcepcionTokenRenovacionInvalido("No se proporcionó el token de renovación.");
        }

        String hash = sha256(tokenRenovacionPlano);
        MTokenRenovacion token = tokenRenovacionRepositorio.findByHashToken(hash)
                .orElseThrow(ExcepcionTokenRenovacionInvalido::new);

        Instant ahora = Instant.now();
        // Protección contra reutilización de token: si ya está revocado, revoca toda la cadena del usuario
        if (token.estaRevocado()) {
            tokenRenovacionRepositorio.revocarTodosDelUsuario(token.getUsuario().getId(), ahora);
            throw new ExcepcionTokenRenovacionInvalido("El token de renovación ya fue utilizado previamente.");
        }

        if (token.estaExpirado(ahora)) {
            throw new ExcepcionTokenRenovacionInvalido("El token de renovación ha expirado.");
        }

        MUsuario usuario = token.getUsuario();
        if (!usuario.isActivo()) {
            throw new ExcepcionTokenRenovacionInvalido("La cuenta de usuario está desactivada.");
        }

        // Rotación estricta de token de renovación
        token.revocar(ahora);
        tokenRenovacionRepositorio.save(token);

        String nuevoTokenPlano = generarTokenAleatorio();
        String nuevoHash = sha256(nuevoTokenPlano);
        MTokenRenovacion nuevoToken = new MTokenRenovacion(
                usuario,
                nuevoHash,
                ahora.plus(duracionRenovacion)
        );
        tokenRenovacionRepositorio.save(nuevoToken);

        var tokenAcceso = proveedorTokensJwt.generar(usuario);
        return new ResultadoSesion(tokenAcceso.token(), tokenAcceso.expiraEnSegundos(), nuevoTokenPlano, duracionRenovacion);
    }

    public void cerrarSesion(String tokenRenovacionPlano) {
        if (tokenRenovacionPlano != null && !tokenRenovacionPlano.isBlank()) {
            String hash = sha256(tokenRenovacionPlano);
            tokenRenovacionRepositorio.findByHashToken(hash).ifPresent(t -> {
                if (!t.estaRevocado()) {
                    t.revocar(Instant.now());
                    tokenRenovacionRepositorio.save(t);
                }
            });
        }
    }

    private String generarTokenAleatorio() {
        byte[] bytes = new byte[32];
        generadorAleatorio.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String sha256(String texto) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Error al calcular hash SHA-256", e);
        }
    }
}
