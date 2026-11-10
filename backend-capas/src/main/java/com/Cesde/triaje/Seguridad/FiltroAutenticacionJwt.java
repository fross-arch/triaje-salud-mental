package com.Cesde.triaje.Seguridad;

import com.Cesde.triaje.Modelo.MUsuario;
import com.Cesde.triaje.Repositorio.IUsuarioRepositorio;
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

public class FiltroAutenticacionJwt extends OncePerRequestFilter {

    private static final String PREFIJO_BEARER = "Bearer ";

    private final ProveedorTokensJwt proveedorTokens;
    private final IUsuarioRepositorio usuarioRepositorio;

    public FiltroAutenticacionJwt(ProveedorTokensJwt proveedorTokens, IUsuarioRepositorio usuarioRepositorio) {
        this.proveedorTokens = proveedorTokens;
        this.usuarioRepositorio = usuarioRepositorio;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest solicitud, HttpServletResponse respuesta,
                                    FilterChain cadena) throws ServletException, IOException {
        String encabezado = solicitud.getHeader(HttpHeaders.AUTHORIZATION);
        if (encabezado != null && encabezado.startsWith(PREFIJO_BEARER)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = encabezado.substring(PREFIJO_BEARER.length()).trim();
            proveedorTokens.validar(token).ifPresent(usuarioToken -> {
                Optional<MUsuario> usuarioDb = usuarioRepositorio.findById(usuarioToken.id());
                if (usuarioDb.isPresent() && usuarioDb.get().isActivo()) {
                    var auth = new UsernamePasswordAuthenticationToken(
                            usuarioToken,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + usuarioDb.get().getRol().name()))
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            });
        }
        cadena.doFilter(solicitud, respuesta);
    }
}
