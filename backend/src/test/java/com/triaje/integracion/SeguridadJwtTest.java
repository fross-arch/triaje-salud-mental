package com.triaje.integracion;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SeguridadJwtTest extends PruebaIntegracionBase {

    private static final String SECRETO_PRUEBA = "secreto-de-pruebas-solo-para-testcontainers-0123456789";
    private static final SecretKey CLAVE = Keys.hmacShaKeyFor(SECRETO_PRUEBA.getBytes(StandardCharsets.UTF_8));

    @Test
    @DisplayName("Token de acceso expirado devuelve 401")
    void debeRechazarTokenAccesoExpirado() throws Exception {
        Instant pasado = Instant.now().minus(20, ChronoUnit.MINUTES);
        String tokenExpirado = Jwts.builder()
                .issuer("triaje-salud-mental")
                .subject("1")
                .claim("rol", "PACIENTE")
                .issuedAt(Date.from(pasado.minus(15, ChronoUnit.MINUTES)))
                .expiration(Date.from(pasado))
                .signWith(CLAVE, Jwts.SIG.HS256)
                .compact();

        mockMvc.perform(get("/api/usuarios/yo")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenExpirado)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.estado").value(401))
                .andExpect(jsonPath("$.error").value("NO_AUTENTICADO"));
    }

    @Test
    @DisplayName("Token de acceso con firma alterada devuelve 401")
    void debeRechazarTokenAccesoConFirmaAlterada() throws Exception {
        String correo = correoUnico();
        registrar(correo);
        String tokenValido = tokenAcceso(iniciarSesion(correo));

        // Alteramos un caracter intermedio de la firma para evitar bits de relleno base64
        int indiceAlterar = tokenValido.length() - 5;
        char charOriginal = tokenValido.charAt(indiceAlterar);
        char alterado = (charOriginal == 'x') ? 'y' : 'x';
        String tokenFalso = tokenValido.substring(0, indiceAlterar) + alterado + tokenValido.substring(indiceAlterar + 1);

        mockMvc.perform(get("/api/usuarios/yo")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFalso)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.estado").value(401))
                .andExpect(jsonPath("$.error").value("NO_AUTENTICADO"));
    }
}
