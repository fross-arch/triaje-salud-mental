package com.triaje.infraestructura.seguridad;

import com.triaje.aplicacion.puerto.RepositorioUsuarios;
import com.triaje.infraestructura.configuracion.PropiedadesSeguridad;
import com.triaje.infraestructura.web.error.FabricaRespuestaError;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
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

import java.time.Duration;
import java.util.List;

/**
 * Seguridad sin estado: JWT Bearer para la API y cookie httpOnly SameSite=Strict solo para la renovación.
 * CSRF se desactiva porque la API no usa cookies para autenticar; la cookie de renovación está
 * limitada a /api/autenticacion y protegida por SameSite=Strict y el CORS explícito.
 */
@Configuration
@EnableWebSecurity
public class ConfiguracionSeguridad {

    @Bean
    public SecurityFilterChain cadenaFiltrosSeguridad(HttpSecurity http, ProveedorTokensJwt proveedorTokens,
                                                      RepositorioUsuarios repositorioUsuarios,
                                                      FabricaRespuestaError fabricaError) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {
                })
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
                        .authenticationEntryPoint((solicitud, respuesta, e) -> fabricaError.escribir(
                                solicitud, respuesta, HttpStatus.UNAUTHORIZED, "NO_AUTENTICADO",
                                "Debes iniciar sesión para acceder a este recurso."))
                        .accessDeniedHandler((solicitud, respuesta, e) -> fabricaError.escribir(
                                solicitud, respuesta, HttpStatus.FORBIDDEN, "ACCESO_DENEGADO",
                                "No tienes permiso para acceder a este recurso.")))
                .headers(encabezados -> encabezados
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'")))
                .addFilterBefore(new FiltroAutenticacionJwt(proveedorTokens, repositorioUsuarios), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(PropiedadesSeguridad propiedades) {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(propiedades.cors().origenesPermitidos());
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuracion.setAllowCredentials(true);
        configuracion.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", configuracion);
        return fuente;
    }

    @Bean
    public PasswordEncoder passwordEncoder(PropiedadesSeguridad propiedades) {
        return new BCryptPasswordEncoder(propiedades.bcryptFuerza());
    }

    /**
     * Evita que Spring Boot cree un usuario por defecto con contraseña en los logs.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager();
    }
}
