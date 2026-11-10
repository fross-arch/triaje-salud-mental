package com.Cesde.triaje.Controlador;

import com.Cesde.triaje.Dto.RespuestaTablero;
import com.Cesde.triaje.Modelo.Enumeracion.RolUsuario;
import com.Cesde.triaje.Seguridad.UsuarioAutenticado;
import com.Cesde.triaje.Servicio.SAdministracion;
import com.Cesde.triaje.Servicio.STablero;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CAdministracionTableroTest {

    private SAdministracion administracionServicio;
    private STablero tableroServicio;
    private CAdministracion controlador;
    private UsuarioAutenticado adminAutenticado;

    @BeforeEach
    void setUp() {
        administracionServicio = Mockito.mock(SAdministracion.class);
        tableroServicio = Mockito.mock(STablero.class);
        controlador = new CAdministracion(administracionServicio, tableroServicio);
        adminAutenticado = new UsuarioAutenticado(1L, RolUsuario.ADMINISTRADOR);
    }

    @Test
    @DisplayName("CAdministracion delega la consulta del tablero a STablero y responde 200 OK")
    void deberiaDelegarObtencionTableroAlServicio() {
        RespuestaTablero respuestaEsperada = new RespuestaTablero(
                false,
                null,
                10,
                5,
                new RespuestaTablero.IndicadoresTablero(10, 8.5, 8.0, 2.1, 6.2, 6.0, 1.8, 0.75, 0, 0.0, 5),
                new RespuestaTablero.GraficosTablero("phq9_b64", "gad7_b64", "semanas_b64", "matriz_b64")
        );

        when(tableroServicio.obtenerTablero(1L, 30)).thenReturn(respuestaEsperada);

        ResponseEntity<RespuestaTablero> respuesta = controlador.obtenerTablero(30, adminAutenticado);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertNotNull(respuesta.getBody());
        assertEquals(10, respuesta.getBody().totalEvaluaciones());

        verify(tableroServicio).obtenerTablero(1L, 30);
    }
}
