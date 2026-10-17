package com.triaje.infraestructura.web.controlador;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.triaje.aplicacion.casouso.ConsultarConsentimientoVigente;
import com.triaje.aplicacion.casouso.RegistrarConsentimiento;
import com.triaje.aplicacion.casouso.RevocarConsentimiento;
import com.triaje.aplicacion.dto.ComandoConsentimiento;
import com.triaje.aplicacion.dto.DatosConsentimiento;
import com.triaje.dominio.modelo.Rol;
import com.triaje.infraestructura.seguridad.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ControladorConsentimientosTest {

    private MockMvc mockMvc;
    private RegistrarConsentimiento registrarConsentimiento;
    private ConsultarConsentimientoVigente consultarConsentimientoVigente;
    private RevocarConsentimiento revocarConsentimiento;
    private ObjectMapper objectMapper;

    @BeforeEach
    void inicializar() {
        registrarConsentimiento = mock(RegistrarConsentimiento.class);
        consultarConsentimientoVigente = mock(ConsultarConsentimientoVigente.class);
        revocarConsentimiento = mock(RevocarConsentimiento.class);
        objectMapper = new ObjectMapper();

        ControladorConsentimientos controlador = new ControladorConsentimientos(
                registrarConsentimiento,
                consultarConsentimientoVigente,
                revocarConsentimiento
        );

        HandlerMethodArgumentResolver resolvedorUsuario = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().equals(UsuarioAutenticado.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return new UsuarioAutenticado(1L, Rol.PACIENTE);
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(controlador)
                .setCustomArgumentResolvers(resolvedorUsuario)
                .build();
    }

    @Test
    @DisplayName("Debe registrar consentimiento exitosamente con codigo 201")
    void debeRegistrarConsentimientoExitosamente() throws Exception {
        DatosConsentimiento datos = new DatosConsentimiento(
                10L, 1L, "1.0", "1.0", true, Instant.now(), null, true
        );
        when(registrarConsentimiento.ejecutar(any(ComandoConsentimiento.class))).thenReturn(datos);

        mockMvc.perform(post("/api/consentimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("confirmaMayoriaEdad", true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.vigente").value(true));
    }

    @Test
    @DisplayName("Debe retornar estado vigente cuando existe consentimiento activo")
    void debeRetornarConsentimientoVigente() throws Exception {
        DatosConsentimiento datos = new DatosConsentimiento(
                10L, 1L, "1.0", "1.0", true, Instant.now(), null, true
        );
        when(consultarConsentimientoVigente.ejecutar(1L)).thenReturn(Optional.of(datos));

        mockMvc.perform(get("/api/consentimientos/vigente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vigente").value(true))
                .andExpect(jsonPath("$.versionConsentimiento").value("1.0"));
    }

    @Test
    @DisplayName("Debe revocar consentimiento exitosamente")
    void debeRevocarConsentimiento() throws Exception {
        mockMvc.perform(post("/api/consentimientos/revocar"))
                .andExpect(status().isOk());

        verify(revocarConsentimiento).ejecutar(1L);
    }
}
