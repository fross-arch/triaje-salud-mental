package com.Cesde.triaje.Configuracion;

import com.Cesde.triaje.Dto.RespuestaError;
import com.Cesde.triaje.Repositorio.IUsuarioRepositorio;
import com.Cesde.triaje.Seguridad.FiltroAutenticacionJwt;
import com.Cesde.triaje.Seguridad.ProveedorTokensJwt;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Configuration
@EnableWebSecurity
public class ConfiguracionSeguridad {

    private final ObjectMapper mapeador = new ObjectMapper().findAndRegisterModules();

    @Bean
    public SecurityFilterChain cadenaFiltrosSeguridad(
            HttpSecurity http,
            ProveedorTokensJwt proveedorTokens,
            IUsuarioRepositorio usuarioRepositorio
    ) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {})
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(autorizacion -> autorizacion
                        .requestMatchers(HttpMethod.POST,
                                "/api/autenticacion/registro",
                                "/api/autenticacion/inicio-sesion",
                                "/api/autenticacion/renovar",
                                "/api/autenticacion/cierre-sesion").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/recursos-ayuda").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/administracion/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/api/clinico/**").hasRole("CLINICO")
                        .requestMatchers("/api/triaje/evaluaciones/**").hasRole("PACIENTE")
                        .requestMatchers("/api/consentimientos/**").hasRole("PACIENTE")
                        .requestMatchers("/api/clinicos", "/api/clinicos/**").hasRole("PACIENTE")
                        .requestMatchers("/api/citas", "/api/citas/**").hasRole("PACIENTE")
                        .requestMatchers("/api/usuarios/**").authenticated()
                        .anyRequest().authenticated())
                .exceptionHandling(excepciones -> excepciones
                        .authenticationEntryPoint((solicitud, respuesta, e) -> escribirError(
                                solicitud, respuesta, HttpStatus.UNAUTHORIZED, "NO_AUTENTICADO",
                                "Debes iniciar sesión para acceder a este recurso."))
                        .accessDeniedHandler((solicitud, respuesta, e) -> escribirError(
                                solicitud, respuesta, HttpStatus.FORBIDDEN, "ACCESO_DENEGADO",
                                "No tienes permiso para acceder a este recurso.")))
                .headers(encabezados -> encabezados
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'")))
                .addFilterBefore(new FiltroAutenticacionJwt(proveedorTokens, usuarioRepositorio), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void escribirError(HttpServletRequest solicitud, HttpServletResponse respuesta,
                               HttpStatus estado, String codigo, String mensaje) throws IOException {
        respuesta.setStatus(estado.value());
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        respuesta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        RespuestaError error = new RespuestaError(
                Instant.now(),
                estado.value(),
                codigo,
                mensaje,
                solicitud.getRequestURI(),
                List.of()
        );
        mapeador.writeValue(respuesta.getOutputStream(), error);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${seguridad.cors.origenes-permitidos:http://localhost:5173}") List<String> origenesPermitidos
    ) {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(origenesPermitidos);
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuracion.setAllowCredentials(true);
        configuracion.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", configuracion);
        return fuente;
    }

    @Bean
    public PasswordEncoder passwordEncoder(
            @Value("${seguridad.bcrypt.fuerza:12}") int fuerza
    ) {
        return new BCryptPasswordEncoder(fuerza);
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager();
    }
}
