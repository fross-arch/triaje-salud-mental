package com.Cesde.triaje.Controlador;

import com.Cesde.triaje.Dto.RespuestaAutorizacion;
import com.Cesde.triaje.Dto.RespuestaEvaluacionDetalle;
import com.Cesde.triaje.Dto.RespuestaEvaluacionResumen;
import com.Cesde.triaje.Dto.RespuestaRegistroItem;
import com.Cesde.triaje.Dto.SolicitudOtorgarAutorizacion;
import com.Cesde.triaje.Dto.SolicitudRespuesta;
import com.Cesde.triaje.Seguridad.UsuarioAutenticado;
import com.Cesde.triaje.Servicio.SAutorizacion;
import com.Cesde.triaje.Servicio.SEvaluacion;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/triaje/evaluaciones")
public class CTriaje {

    private static final Logger REGISTRO = LoggerFactory.getLogger(CTriaje.class);

    private final SEvaluacion evaluacionServicio;
    private final SAutorizacion autorizacionServicio;

    public CTriaje(SEvaluacion evaluacionServicio, SAutorizacion autorizacionServicio) {
        this.evaluacionServicio = evaluacionServicio;
        this.autorizacionServicio = autorizacionServicio;
    }

    @PostMapping
    public ResponseEntity<RespuestaEvaluacionResumen> crearEvaluacion(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        RespuestaEvaluacionResumen respuesta = evaluacionServicio.crearEvaluacion(usuario.id());
        REGISTRO.info("Evaluación creada exitosamente.");
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping
    public ResponseEntity<List<RespuestaEvaluacionResumen>> listarEvaluaciones(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        List<RespuestaEvaluacionResumen> lista = evaluacionServicio.listarEvaluaciones(usuario.id());
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RespuestaEvaluacionDetalle> obtenerDetalle(
            @PathVariable String id,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        RespuestaEvaluacionDetalle detalle = evaluacionServicio.obtenerDetalle(id, usuario.id());
        return ResponseEntity.ok(detalle);
    }

    @PutMapping("/{id}/respuestas")
    public ResponseEntity<RespuestaRegistroItem> registrarRespuesta(
            @PathVariable String id,
            @Valid @RequestBody SolicitudRespuesta solicitud,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        RespuestaRegistroItem respuesta = evaluacionServicio.registrarRespuesta(id, usuario.id(), solicitud);
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/{id}/finalizar")
    public ResponseEntity<RespuestaEvaluacionResumen> finalizarEvaluacion(
            @PathVariable String id,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        RespuestaEvaluacionResumen respuesta = evaluacionServicio.finalizarEvaluacion(id, usuario.id());
        REGISTRO.info("Evaluación finalizada exitosamente.");
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> descargarPdf(
            @PathVariable String id,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        byte[] pdf = evaluacionServicio.generarPdf(id, usuario.id());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte-cribado.pdf\"")
                .body(pdf);
    }

    @PostMapping("/{id}/autorizaciones")
    public ResponseEntity<RespuestaAutorizacion> otorgarAutorizacion(
            @PathVariable String id,
            @Valid @RequestBody SolicitudOtorgarAutorizacion solicitud,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        RespuestaAutorizacion respuesta = autorizacionServicio.otorgar(id, usuario.id(), solicitud);
        REGISTRO.info("Autorización otorgada al clínico exitosamente.");
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping("/{id}/autorizaciones")
    public ResponseEntity<List<RespuestaAutorizacion>> listarAutorizaciones(
            @PathVariable String id,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        List<RespuestaAutorizacion> lista = autorizacionServicio.listarPorEvaluacion(id, usuario.id());
        return ResponseEntity.ok(lista);
    }

    @PostMapping("/{id}/autorizaciones/{clinicoId}/revocar")
    public ResponseEntity<RespuestaAutorizacion> revocarAutorizacion(
            @PathVariable String id,
            @PathVariable Long clinicoId,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        RespuestaAutorizacion respuesta = autorizacionServicio.revocar(id, usuario.id(), clinicoId);
        REGISTRO.info("Autorización revocada exitosamente.");
        return ResponseEntity.ok(respuesta);
    }
}
