package com.triaje.aplicacion.casouso;

import com.triaje.aplicacion.dto.DatosSolicitudReporte;
import com.triaje.aplicacion.dto.DatosSolicitudReporte.DatosLineaAyudaReporte;
import com.triaje.aplicacion.puerto.ClienteReportes;
import com.triaje.aplicacion.puerto.RepositorioAuditoria;
import com.triaje.aplicacion.puerto.RepositorioEvaluaciones;
import com.triaje.aplicacion.puerto.RepositorioRespuestasEvaluacion;
import com.triaje.aplicacion.puerto.RepositorioUsuarios;
import com.triaje.dominio.ReglaResultadoMinimo;
import com.triaje.dominio.SeveridadAnsiedad;
import com.triaje.dominio.SeveridadDepresion;
import com.triaje.dominio.excepcion.ExcepcionEvaluacionIncompleta;
import com.triaje.dominio.excepcion.ExcepcionEvaluacionNoEncontrada;
import com.triaje.dominio.modelo.AccionAuditoria;
import com.triaje.dominio.modelo.EstadoEvaluacion;
import com.triaje.dominio.modelo.Evaluacion;
import com.triaje.dominio.modelo.InstrumentoCribado;
import com.triaje.dominio.modelo.RegistroAuditoria;
import com.triaje.dominio.modelo.RespuestaEvaluacion;
import com.triaje.dominio.modelo.Rol;
import com.triaje.dominio.modelo.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GenerarReportePdfEvaluacionTest {

    private RepositorioEvaluaciones repositorioEvaluaciones;
    private RepositorioUsuarios repositorioUsuarios;
    private RepositorioRespuestasEvaluacion repositorioRespuestas;
    private RepositorioAuditoria repositorioAuditoria;
    private ClienteReportes clienteReportes;
    private ReglaResultadoMinimo reglaResultadoMinimo;
    private Clock reloj;
    private GenerarReportePdfEvaluacion casoUso;

    private final Instant momentoFijo = Instant.parse("2026-10-13T10:00:00Z");

    @BeforeEach
    void inicializar() {
        repositorioEvaluaciones = mock(RepositorioEvaluaciones.class);
        repositorioUsuarios = mock(RepositorioUsuarios.class);
        repositorioRespuestas = mock(RepositorioRespuestasEvaluacion.class);
        repositorioAuditoria = mock(RepositorioAuditoria.class);
        clienteReportes = mock(ClienteReportes.class);
        reglaResultadoMinimo = mock(ReglaResultadoMinimo.class);
        reloj = Clock.fixed(momentoFijo, ZoneOffset.UTC);

        List<DatosLineaAyudaReporte> lineas = List.of(
                new DatosLineaAyudaReporte("Linea 106", "106", "24/7", "Orientacion")
        );

        casoUso = new GenerarReportePdfEvaluacion(
                repositorioEvaluaciones,
                repositorioUsuarios,
                repositorioRespuestas,
                repositorioAuditoria,
                clienteReportes,
                reglaResultadoMinimo,
                lineas,
                reloj
        );
    }

    @Test
    @DisplayName("Debe lanzar ExcepcionEvaluacionNoEncontrada cuando la evaluacion no existe")
    void debeBloquearSiEvaluacionNoExiste() {
        when(repositorioEvaluaciones.buscarPorId("eval-inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> casoUso.ejecutar("eval-inexistente", 1L))
                .isInstanceOf(ExcepcionEvaluacionNoEncontrada.class);

        verify(clienteReportes, never()).generarReportePdf(any());
        verify(repositorioAuditoria, never()).registrar(any());
    }

    @Test
    @DisplayName("Debe lanzar ExcepcionEvaluacionNoEncontrada cuando la evaluacion pertenece a otro usuario")
    void debeBloquearSiEvaluacionPerteneceAOtroUsuario() {
        Evaluacion evaluacionAjena = new Evaluacion(
                "eval-100", 2L, EstadoEvaluacion.COMPLETADA, 5, 5,
                SeveridadDepresion.LEVE, SeveridadAnsiedad.LEVE, false, momentoFijo, momentoFijo
        );
        when(repositorioEvaluaciones.buscarPorId("eval-100")).thenReturn(Optional.of(evaluacionAjena));

        assertThatThrownBy(() -> casoUso.ejecutar("eval-100", 1L))
                .isInstanceOf(ExcepcionEvaluacionNoEncontrada.class);

        verify(clienteReportes, never()).generarReportePdf(any());
        verify(repositorioAuditoria, never()).registrar(any());
    }

    @Test
    @DisplayName("Debe lanzar ExcepcionEvaluacionIncompleta cuando la evaluacion esta EN_CURSO")
    void debeBloquearSiEvaluacionEstaEnCurso() {
        Evaluacion evaluacionEnCurso = new Evaluacion(
                "eval-101", 1L, EstadoEvaluacion.EN_CURSO, null, null,
                null, null, false, momentoFijo, null
        );
        when(repositorioEvaluaciones.buscarPorId("eval-101")).thenReturn(Optional.of(evaluacionEnCurso));

        assertThatThrownBy(() -> casoUso.ejecutar("eval-101", 1L))
                .isInstanceOf(ExcepcionEvaluacionIncompleta.class)
                .hasMessageContaining("COMPLETADA");

        verify(clienteReportes, never()).generarReportePdf(any());
        verify(repositorioAuditoria, never()).registrar(any());
    }

    @Test
    @DisplayName("Debe lanzar ExcepcionEvaluacionIncompleta cuando la evaluacion esta ABANDONADA")
    void debeBloquearSiEvaluacionEstaAbandonada() {
        Evaluacion evaluacionAbandonada = new Evaluacion(
                "eval-102", 1L, EstadoEvaluacion.ABANDONADA, null, null,
                null, null, false, momentoFijo, null
        );
        when(repositorioEvaluaciones.buscarPorId("eval-102")).thenReturn(Optional.of(evaluacionAbandonada));

        assertThatThrownBy(() -> casoUso.ejecutar("eval-102", 1L))
                .isInstanceOf(ExcepcionEvaluacionIncompleta.class)
                .hasMessageContaining("ABANDONADA");

        verify(clienteReportes, never()).generarReportePdf(any());
        verify(repositorioAuditoria, never()).registrar(any());
    }

    @Test
    @DisplayName("Debe generar reporte y registrar auditoria PDF_DESCARGADO tras flujo exitoso con 16 respuestas")
    void debeGenerarReporteYRegistrarAuditoriaExitosamente() {
        Evaluacion evaluacion = new Evaluacion(
                "eval-200", 1L, EstadoEvaluacion.COMPLETADA, 11, 7,
                SeveridadDepresion.MODERADA, SeveridadAnsiedad.LEVE, false, momentoFijo, momentoFijo
        );
        Usuario usuario = new Usuario(
                1L, "Laura Gomez", "laura@ejemplo.com", "hash", Rol.PACIENTE, momentoFijo
        );

        List<RespuestaEvaluacion> dieciseisRespuestas = List.of(
                new RespuestaEvaluacion(1L, "eval-200", InstrumentoCribado.PHQ9, 1, 0),
                new RespuestaEvaluacion(2L, "eval-200", InstrumentoCribado.PHQ9, 2, 1),
                new RespuestaEvaluacion(3L, "eval-200", InstrumentoCribado.PHQ9, 3, 2),
                new RespuestaEvaluacion(4L, "eval-200", InstrumentoCribado.PHQ9, 4, 3),
                new RespuestaEvaluacion(5L, "eval-200", InstrumentoCribado.PHQ9, 5, 0),
                new RespuestaEvaluacion(6L, "eval-200", InstrumentoCribado.PHQ9, 6, 1),
                new RespuestaEvaluacion(7L, "eval-200", InstrumentoCribado.PHQ9, 7, 2),
                new RespuestaEvaluacion(8L, "eval-200", InstrumentoCribado.PHQ9, 8, 2),
                new RespuestaEvaluacion(9L, "eval-200", InstrumentoCribado.PHQ9, 9, 0),
                new RespuestaEvaluacion(10L, "eval-200", InstrumentoCribado.GAD7, 1, 0),
                new RespuestaEvaluacion(11L, "eval-200", InstrumentoCribado.GAD7, 2, 1),
                new RespuestaEvaluacion(12L, "eval-200", InstrumentoCribado.GAD7, 3, 2),
                new RespuestaEvaluacion(13L, "eval-200", InstrumentoCribado.GAD7, 4, 1),
                new RespuestaEvaluacion(14L, "eval-200", InstrumentoCribado.GAD7, 5, 1),
                new RespuestaEvaluacion(15L, "eval-200", InstrumentoCribado.GAD7, 6, 1),
                new RespuestaEvaluacion(16L, "eval-200", InstrumentoCribado.GAD7, 7, 1)
        );

        when(repositorioEvaluaciones.buscarPorId("eval-200")).thenReturn(Optional.of(evaluacion));
        when(repositorioUsuarios.buscarPorId(1L)).thenReturn(Optional.of(usuario));
        when(repositorioRespuestas.buscarPorEvaluacionId("eval-200")).thenReturn(dieciseisRespuestas);
        when(reglaResultadoMinimo.evaluar(SeveridadDepresion.MODERADA, SeveridadAnsiedad.LEVE, false)).thenReturn(false);

        byte[] pdfEsperado = "%PDF-documento-valido".getBytes();
        when(clienteReportes.generarReportePdf(any(DatosSolicitudReporte.class))).thenReturn(pdfEsperado);

        byte[] resultado = casoUso.ejecutar("eval-200", 1L);

        assertThat(resultado).isEqualTo(pdfEsperado);

        // Verificacion de la solicitud enviada al cliente de reportes
        ArgumentCaptor<DatosSolicitudReporte> captorSolicitud = ArgumentCaptor.forClass(DatosSolicitudReporte.class);
        verify(clienteReportes).generarReportePdf(captorSolicitud.capture());
        DatosSolicitudReporte solicitudEnviada = captorSolicitud.getValue();
        assertThat(solicitudEnviada.respuestasResumen()).hasSize(16);
        assertThat(solicitudEnviada.respuestasResumen().get(0).instrumento()).isEqualTo("PHQ-9");
        assertThat(solicitudEnviada.respuestasResumen().get(0).item()).isEqualTo(1);
        assertThat(solicitudEnviada.respuestasResumen().get(0).frecuencia()).isEqualTo("Ningún día");
        assertThat(solicitudEnviada.respuestasResumen().get(8).instrumento()).isEqualTo("PHQ-9");
        assertThat(solicitudEnviada.respuestasResumen().get(8).item()).isEqualTo(9);
        assertThat(solicitudEnviada.respuestasResumen().get(8).frecuencia()).isEqualTo("Ningún día");
        assertThat(solicitudEnviada.respuestasResumen().get(9).instrumento()).isEqualTo("GAD-7");
        assertThat(solicitudEnviada.respuestasResumen().get(9).item()).isEqualTo(1);
        assertThat(solicitudEnviada.respuestasResumen().get(9).frecuencia()).isEqualTo("Ningún día");
        assertThat(solicitudEnviada.respuestasResumen().get(15).instrumento()).isEqualTo("GAD-7");
        assertThat(solicitudEnviada.respuestasResumen().get(15).item()).isEqualTo(7);

        // Verificacion de auditoria
        ArgumentCaptor<RegistroAuditoria> captorAuditoria = ArgumentCaptor.forClass(RegistroAuditoria.class);
        verify(repositorioAuditoria).registrar(captorAuditoria.capture());

        RegistroAuditoria auditoriaGuardada = captorAuditoria.getValue();
        assertThat(auditoriaGuardada.usuarioId()).isEqualTo(1L);
        assertThat(auditoriaGuardada.accion()).isEqualTo(AccionAuditoria.PDF_DESCARGADO);
        assertThat(auditoriaGuardada.entidadId()).isEqualTo("eval-200");
        assertThat(auditoriaGuardada.fecha()).isEqualTo(momentoFijo);
    }
}
