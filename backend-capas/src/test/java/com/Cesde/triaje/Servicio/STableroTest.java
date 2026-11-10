package com.Cesde.triaje.Servicio;

import com.Cesde.triaje.Dto.RespuestaTablero;
import com.Cesde.triaje.Excepcion.ExcepcionDatoInvalido;
import com.Cesde.triaje.Modelo.Enumeracion.AccionAuditoria;
import com.Cesde.triaje.Modelo.Enumeracion.EstadoEvaluacion;
import com.Cesde.triaje.Modelo.Enumeracion.SeveridadAnsiedad;
import com.Cesde.triaje.Modelo.Enumeracion.SeveridadDepresion;
import com.Cesde.triaje.Repositorio.IEvaluacionAnonimaProjection;
import com.Cesde.triaje.Repositorio.IEvaluacionRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class STableroTest {

    private IEvaluacionRepositorio evaluacionRepositorio;
    private SReporte reporteServicio;
    private SAuditoria auditoriaServicio;
    private STablero servicioTablero;

    @BeforeEach
    void setUp() {
        evaluacionRepositorio = Mockito.mock(IEvaluacionRepositorio.class);
        reporteServicio = Mockito.mock(SReporte.class);
        auditoriaServicio = Mockito.mock(SAuditoria.class);
        servicioTablero = new STablero(evaluacionRepositorio, reporteServicio, auditoriaServicio, 5, false);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 7, 30, 90})
    @DisplayName("STablero acepta valores validos de dias (0, 7, 30, 90) y audita la consulta")
    void deberiaAceptarValoresValidosDeDiasYAuditar(int diasValidos) {
        RespuestaTablero dummy = new RespuestaTablero(false, null, 0, 5, null, null);
        when(reporteServicio.generarTablero(any())).thenReturn(dummy);

        RespuestaTablero respuesta = servicioTablero.obtenerTablero(1L, diasValidos);

        assertNotNull(respuesta);
        verify(auditoriaServicio).registrar(1L, AccionAuditoria.TABLERO_CONSULTADO, String.valueOf(diasValidos));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 1, 15, 60, 100, 365})
    @DisplayName("STablero rechaza valores invalidos de dias lanzando ExcepcionDatoInvalido")
    void deberiaRechazarValoresInvalidosDeDias(int diasInvalidos) {
        ExcepcionDatoInvalido ex = assertThrows(
                ExcepcionDatoInvalido.class,
                () -> servicioTablero.obtenerTablero(1L, diasInvalidos)
        );
        assertEquals("El parámetro 'dias' solo permite los valores: 0, 7, 30 o 90.", ex.getMessage());
    }

    @Test
    @DisplayName("STablero formatea la fecha como YYYY-MM-DD en America/Bogota y no expone datos de usuario")
    @SuppressWarnings("unchecked")
    void deberiaFormatearFechaEnZonaBogotaSinDatosSensibles() {
        // 2026-11-05 02:00:00 UTC corresponde a 2026-11-04 21:00:00 en America/Bogota (UTC-5)
        Instant fechaUtc = Instant.parse("2026-11-05T02:00:00Z");

        IEvaluacionAnonimaProjection proyeccion = new IEvaluacionAnonimaProjection() {
            @Override public Instant getFechaFinalizacion() { return fechaUtc; }
            @Override public Integer getPuntajePhq9() { return 14; }
            @Override public Integer getPuntajeGad7() { return 10; }
            @Override public SeveridadDepresion getSeveridadDepresion() { return SeveridadDepresion.MODERADA; }
            @Override public SeveridadAnsiedad getSeveridadAnsiedad() { return SeveridadAnsiedad.MODERADA; }
            @Override public boolean isRiesgoCrisis() { return false; }
        };

        when(evaluacionRepositorio.findByEstadoOrderByFechaFinalizacionAsc(EstadoEvaluacion.COMPLETADA))
                .thenReturn(List.of(proyeccion));

        RespuestaTablero dummy = new RespuestaTablero(false, null, 1, 5, null, null);
        when(reporteServicio.generarTablero(any())).thenReturn(dummy);

        servicioTablero.obtenerTablero(1L, 0);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(reporteServicio).generarTablero(captor.capture());

        Map<String, Object> peticionEnviada = captor.getValue();
        List<Map<String, Object>> registros = (List<Map<String, Object>>) peticionEnviada.get("registros");

        assertNotNull(registros);
        assertEquals(1, registros.size());

        Map<String, Object> reg = registros.get(0);
        // Debe ser 2026-11-04 (zona Bogotá UTC-5) en formato YYYY-MM-DD sin hora
        assertEquals("2026-11-04", reg.get("fecha"));
        assertEquals(14, reg.get("puntaje_phq9"));
        assertEquals(10, reg.get("puntaje_gad7"));
        assertEquals("MODERADA", reg.get("severidad_depresion"));
        assertEquals("MODERADA", reg.get("severidad_ansiedad"));
        assertEquals(false, reg.get("riesgo_crisis"));

        // Garantizar que no viajan claves sensibles
        assertFalse(reg.containsKey("id"));
        assertFalse(reg.containsKey("usuario"));
        assertFalse(reg.containsKey("usuario_id"));
        assertFalse(reg.containsKey("correo"));
        assertFalse(reg.containsKey("nombre"));
    }

    @Test
    @DisplayName("STablero en modo demo (modoDemo=true) NO consulta la BD real, envía demo=true y audita con MODO_DEMO")
    @SuppressWarnings("unchecked")
    void deberiaManejarModoDemoSinConsultarBaseDeDatos() {
        STablero servicioDemo = new STablero(evaluacionRepositorio, reporteServicio, auditoriaServicio, 5, true);

        RespuestaTablero dummyDemo = new RespuestaTablero(false, true, null, 120, 5, null, null);
        when(reporteServicio.generarTablero(any())).thenReturn(dummyDemo);

        RespuestaTablero respuesta = servicioDemo.obtenerTablero(1L, 30);

        assertNotNull(respuesta);
        assertTrue(respuesta.esDemostracion());

        // Verificar que NUNCA se consultó el repositorio de evaluaciones
        verify(evaluacionRepositorio, Mockito.never()).findByEstadoOrderByFechaFinalizacionAsc(any());
        verify(evaluacionRepositorio, Mockito.never()).findByEstadoAndFechaFinalizacionGreaterThanEqualOrderByFechaFinalizacionAsc(any(), any());

        // Verificar payload hacia Python
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(reporteServicio).generarTablero(captor.capture());
        Map<String, Object> peticion = captor.getValue();
        assertEquals(true, peticion.get("demo"));
        assertEquals(5, peticion.get("minimo_grupo"));
        assertTrue(((List<?>) peticion.get("registros")).isEmpty());

        // Verificar anotación de auditoría con MODO_DEMO
        verify(auditoriaServicio).registrar(1L, AccionAuditoria.TABLERO_CONSULTADO, "30 (MODO_DEMO)");
    }
}
