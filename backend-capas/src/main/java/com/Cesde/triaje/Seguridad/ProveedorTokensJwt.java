package com.Cesde.triaje.Seguridad;

import com.Cesde.triaje.Modelo.MUsuario;
import com.Cesde.triaje.Modelo.Enumeracion.RolUsuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Component
public class ProveedorTokensJwt {

    private static final String RECLAMO_ROL = "rol";
    private static final int BYTES_MINIMOS_SECRETO = 32;

    private final SecretKey clave;
    private final String emisor;
    private final Duration duracion;
    private final JwtParser analizador;

    public ProveedorTokensJwt(
            @Value("${seguridad.jwt.secreto}") String secreto,
            @Value("${seguridad.jwt.emisor:triaje-salud-mental}") String emisor,
            @Value("${seguridad.jwt.duracion-minutos:15}") long duracionMinutos
    ) {
        if (secreto == null || secreto.getBytes(StandardCharsets.UTF_8).length < BYTES_MINIMOS_SECRETO) {
            throw new IllegalStateException("JWT_SECRETO debe tener al menos " + BYTES_MINIMOS_SECRETO + " bytes.");
        }
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.emisor = emisor;
        this.duracion = Duration.ofMinutes(duracionMinutos);
        this.analizador = Jwts.parser()
                .verifyWith(clave)
                .requireIssuer(emisor)
                .build();
    }

    public record TokenAcceso(String token, long expiraEnSegundos) {
    }

    public TokenAcceso generar(MUsuario usuario) {
        Instant ahora = Instant.now();
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
