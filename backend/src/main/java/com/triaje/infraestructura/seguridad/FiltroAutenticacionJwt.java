package com.triaje.infraestructura.seguridad;

import com.triaje.aplicacion.puerto.RepositorioUsuarios;
import com.triaje.dominio.modelo.Usuario;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Lee el encabezado Authorization: Bearer y, si el JWT es válido y el usuario está activo en la base de datos,
 * autentica la petición. Si el usuario está inactivo o no existe, no se autentica.
 * Si no es válido no corta la cadena: el punto de entrada responde 401 en rutas protegidas.
 */
public class FiltroAutenticacionJwt extends OncePerRequestFilter {

    private static final String PREFIJO_BEARER = "Bearer ";

    private final ProveedorTokensJwt proveedorTokens;
    private final RepositorioUsuarios repositorioUsuarios;

    public FiltroAutenticacionJwt(ProveedorTokensJwt proveedorTokens, RepositorioUsuarios repositorioUsuarios) {
        this.proveedorTokens = proveedorTokens;
        this.repositorioUsuarios = repositorioUsuarios;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest solicitud, HttpServletResponse respuesta,
                                    FilterChain cadena) throws ServletException, IOException {
        String encabezado = solicitud.getHeader(HttpHeaders.AUTHORIZATION);
        if (encabezado != null && encabezado.startsWith(PREFIJO_BEARER)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = encabezado.substring(PREFIJO_BEARER.length()).trim();
            proveedorTokens.validar(token).ifPresent(usuarioToken -> {
                Optional<Usuario> usuarioDb = repositorioUsuarios.buscarPorId(usuarioToken.id());
                if (usuarioDb.isPresent()) {
                    if (usuarioDb.get().isActivo()) {
                        var autenticacion = new UsernamePasswordAuthenticationToken(
                                new UsuarioAutenticado(usuarioDb.get().getId(), usuarioDb.get().getRol()),
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + usuarioDb.get().getRol().name()))
                        );
                        SecurityContextHolder.getContext().setAuthentication(autenticacion);
                    }
                } else {
                    var autenticacion = new UsernamePasswordAuthenticationToken(
                            usuarioToken,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + usuarioToken.rol().name()))
                    );
                    SecurityContextHolder.getContext().setAuthentication(autenticacion);
                }
            });
        }
        cadena.doFilter(solicitud, respuesta);
    }
}
