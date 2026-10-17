package com.triaje.infraestructura.web.controlador;

import com.triaje.aplicacion.casouso.DescargarReportePdfClinico;
import com.triaje.aplicacion.casouso.ListarEvaluacionesClinico;
import com.triaje.aplicacion.casouso.ObtenerDetalleEvaluacionClinico;
import com.triaje.aplicacion.dto.DetalleEvaluacionClinico;
import com.triaje.aplicacion.dto.ResumenEvaluacionClinico;
import com.triaje.infraestructura.seguridad.UsuarioAutenticado;
import com.triaje.infraestructura.web.dto.RespuestaEvaluacionClinicoDetalle;
import com.triaje.infraestructura.web.dto.RespuestaEvaluacionClinicoResumen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints del portal clinico para consultar evaluaciones autorizadas por pacientes.
 * Acceso restringido al rol CLINICO.
 */
@RestController
@RequestMapping("/api/clinico/evaluaciones")
public class ControladorClinicoEvaluaciones {

    private static final Logger REGISTRO = LoggerFactory.getLogger(ControladorClinicoEvaluaciones.class);

    private final ListarEvaluacionesClinico listarEvaluacionesClinico;
    private final ObtenerDetalleEvaluacionClinico obtenerDetalleEvaluacionClinico;
    private final DescargarReportePdfClinico descargarReportePdfClinico;

    public ControladorClinicoEvaluaciones(ListarEvaluacionesClinico listarEvaluacionesClinico,
                                         ObtenerDetalleEvaluacionClinico obtenerDetalleEvaluacionClinico,
                                         DescargarReportePdfClinico descargarReportePdfClinico) {
        this.listarEvaluacionesClinico = listarEvaluacionesClinico;
        this.obtenerDetalleEvaluacionClinico = obtenerDetalleEvaluacionClinico;
        this.descargarReportePdfClinico = descargarReportePdfClinico;
    }

    @GetMapping
    public ResponseEntity<List<RespuestaEvaluacionClinicoResumen>> listar(
            @AuthenticationPrincipal UsuarioAutenticado clinico
    ) {
        List<ResumenEvaluacionClinico> lista = listarEvaluacionesClinico.ejecutar(clinico.id());
        List<RespuestaEvaluacionClinicoResumen> respuesta = lista.stream()
                .map(RespuestaEvaluacionClinicoResumen::desde)
                .toList();
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RespuestaEvaluacionClinicoDetalle> obtenerPorId(
            @PathVariable String id,
            @AuthenticationPrincipal UsuarioAutenticado clinico
    ) {
        DetalleEvaluacionClinico detalle = obtenerDetalleEvaluacionClinico.ejecutar(id, clinico.id());
        return ResponseEntity.ok(RespuestaEvaluacionClinicoDetalle.desde(detalle));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> descargarPdf(
            @PathVariable String id,
            @AuthenticationPrincipal UsuarioAutenticado clinico
    ) {
        byte[] pdf = descargarReportePdfClinico.ejecutar(id, clinico.id());
        REGISTRO.info("Clinico descargo reporte PDF de evaluacion. Id: {}", id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte-cribado.pdf")
                .body(pdf);
    }
}
