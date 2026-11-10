package com.Cesde.triaje.Controlador;

import com.Cesde.triaje.Dto.RespuestaEvaluacionClinicoDetalle;
import com.Cesde.triaje.Dto.RespuestaEvaluacionClinicoResumen;
import com.Cesde.triaje.Seguridad.UsuarioAutenticado;
import com.Cesde.triaje.Servicio.SClinico;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/clinico/evaluaciones")
public class CClinicoEvaluacion {

    private final SClinico clinicoServicio;

    public CClinicoEvaluacion(SClinico clinicoServicio) {
        this.clinicoServicio = clinicoServicio;
    }

    @GetMapping
    public ResponseEntity<List<RespuestaEvaluacionClinicoResumen>> listarEvaluaciones(
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        List<RespuestaEvaluacionClinicoResumen> lista = clinicoServicio.listarEvaluaciones(usuario.id());
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RespuestaEvaluacionClinicoDetalle> obtenerDetalle(
            @PathVariable String id,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        RespuestaEvaluacionClinicoDetalle detalle = clinicoServicio.obtenerDetalle(id, usuario.id());
        return ResponseEntity.ok(detalle);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> descargarPdf(
            @PathVariable String id,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        byte[] pdf = clinicoServicio.generarPdf(id, usuario.id());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte-cribado.pdf\"")
                .body(pdf);
    }
}
