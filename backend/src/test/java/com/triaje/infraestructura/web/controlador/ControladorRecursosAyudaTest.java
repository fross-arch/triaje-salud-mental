package com.triaje.infraestructura.web.controlador;

import com.triaje.infraestructura.configuracion.PropiedadesRecursosAyuda;
import com.triaje.infraestructura.configuracion.PropiedadesRecursosAyuda.RecursoAyuda;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ControladorRecursosAyudaTest {

    private MockMvc mockMvc;

    @BeforeEach
    void inicializar() {
        PropiedadesRecursosAyuda propiedades = new PropiedadesRecursosAyuda(List.of(
                new RecursoAyuda("Linea 123", "123", "24/7", "Emergencias medicas y situaciones de crisis vital inmediata.", "Nacional (Colombia)"),
                new RecursoAyuda("Linea 192 opcion 4", "192 opcion 4", "24/7", "Orientacion y atencion en salud mental del Ministerio de Salud.", "Nacional (Colombia)"),
                new RecursoAyuda("Linea Amiga Saludable Medellin", "106 / (604) 444 44 48", "24/7", "Acompanamiento psicosocial y prevencion del suicidio de la Alcaldia de Medellin.", "Medellin y area metropolitana")
        ));
        ControladorRecursosAyuda controlador = new ControladorRecursosAyuda(propiedades);
        mockMvc = MockMvcBuilders.standaloneSetup(controlador).build();
    }

    @Test
    @DisplayName("Debe exponer la lista de recursos de ayuda configurados")
    void debeExponerRecursosAyudaConfigurados() throws Exception {
        mockMvc.perform(get("/api/recursos-ayuda"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].nombre").value("Linea 123"))
                .andExpect(jsonPath("$[0].telefono").value("123"))
                .andExpect(jsonPath("$[1].nombre").value("Linea 192 opcion 4"))
                .andExpect(jsonPath("$[2].nombre").value("Linea Amiga Saludable Medellin"));
    }
}
