package com.triaje.infraestructura.seguridad;

import com.triaje.aplicacion.puerto.GeneradorTokensAcceso;
import com.triaje.dominio.RolUsuario;
import com.triaje.dominio.modelo.Usuario;
import com.triaje.infraestructura.configuracion.PropiedadesSeguridad;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Emite y valida los JWT de acceso (HS256). El token solo lleva el id del usuario y su rol:
 * nunca nombre ni correo.
 */
@Component
public class ProveedorTokensJwt implements GeneradorTokensAcceso {

    private static final int BYTES_MINIMOS_SECRETO = 32;
    private static final String RECLAMO_ROL = "rol";

    private final SecretKey clave;
    private final String emisor;
    private final Duration duracion;
    private final Clock reloj;
    private final JwtParser analizador;

    public ProveedorTokensJwt(PropiedadesSeguridad propiedades, Clock reloj) {
        String secreto = propiedades.jwt() != null ? propiedades.jwt().secreto() : null;
        if (secreto == null || secreto.isBlank() || secreto.getBytes(StandardCharsets.UTF_8).length < BYTES_MINIMOS_SECRETO) {
            throw new IllegalStateException(
                    "Error de configuracion de seguridad: La propiedad JWT_SECRETO es obligatoria y debe contener al menos "
                            + BYTES_MINIMOS_SECRETO + " bytes.");
        }
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.emisor = propiedades.jwt().emisor();
        this.duracion = propiedades.jwt().duracionTokenAcceso();
        this.reloj = reloj;
        this.analizador = Jwts.parser()
                .verifyWith(clave)
                .requireIssuer(emisor)
                .clock(() -> Date.from(reloj.instant()))
                .build();
    }

    @Override
    public TokenAcceso generar(Usuario usuario) {
        Instant ahora = reloj.instant();
        String valor = Jwts.builder()
                .issuer(emisor)
                .subject(String.valueOf(usuario.getId()))
                .claim(RECLAMO_ROL, usuario.getRol().name())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(duracion)))
                .signWith(clave, Jwts.SIG.HS256)
                .compact();
        return new TokenAcceso(valor, duracion.toSeconds());
    }

    /**
     * Valida el token y devuelve el usuario autenticado, o vacío si es inválido o expiró.
     */
    public Optional<UsuarioAutenticado> validar(String token) {
        try {
            Claims reclamos = analizador.parseSignedClaims(token).getPayload();
            Long usuarioId = Long.valueOf(reclamos.getSubject());
            RolUsuario rol = RolUsuario.valueOf(reclamos.get(RECLAMO_ROL, String.class));
            return Optional.of(new UsuarioAutenticado(usuarioId, rol));
        } catch (JwtException | IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }
}
