package com.triaje.infraestructura.web.controlador;

import com.triaje.aplicacion.casouso.CrearEvaluacion;
import com.triaje.aplicacion.casouso.FinalizarEvaluacion;
import com.triaje.aplicacion.casouso.GenerarReportePdfEvaluacion;
import com.triaje.aplicacion.casouso.ListarAutorizacionesEvaluacion;
import com.triaje.aplicacion.casouso.ListarEvaluacionesUsuario;
import com.triaje.aplicacion.casouso.ObtenerDetalleEvaluacion;
import com.triaje.aplicacion.casouso.OtorgarAutorizacionAcceso;
import com.triaje.aplicacion.casouso.RegistrarRespuestaEvaluacion;
import com.triaje.aplicacion.casouso.RevocarAutorizacionAcceso;
import com.triaje.aplicacion.dto.ComandoOtorgarAutorizacion;
import com.triaje.aplicacion.dto.ComandoRegistrarRespuesta;
import com.triaje.aplicacion.dto.DatosAutorizacion;
import com.triaje.aplicacion.dto.DatosEvaluacion;
import com.triaje.aplicacion.dto.DetalleEvaluacion;
import com.triaje.aplicacion.dto.ResultadoRegistroRespuesta;
import com.triaje.infraestructura.seguridad.UsuarioAutenticado;
import com.triaje.infraestructura.web.dto.RespuestaAutorizacion;
import com.triaje.infraestructura.web.dto.RespuestaEvaluacionDetalle;
import com.triaje.infraestructura.web.dto.RespuestaEvaluacionResumen;
import com.triaje.infraestructura.web.dto.RespuestaRegistroItem;
import com.triaje.infraestructura.web.dto.SolicitudOtorgarAutorizacion;
import com.triaje.infraestructura.web.dto.SolicitudRespuesta;
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
public class ControladorTriaje {

    private static final Logger REGISTRO = LoggerFactory.getLogger(ControladorTriaje.class);

    private final CrearEvaluacion crearEvaluacion;
    private final RegistrarRespuestaEvaluacion registrarRespuesta;
    private final FinalizarEvaluacion finalizarEvaluacion;
    private final ListarEvaluacionesUsuario listarEvaluaciones;
    private final ObtenerDetalleEvaluacion obtenerDetalleEvaluacion;
    private final GenerarReportePdfEvaluacion generarReportePdfEvaluacion;
    private final OtorgarAutorizacionAcceso otorgarAutorizacion;
    private final ListarAutorizacionesEvaluacion listarAutorizaciones;
    private final RevocarAutorizacionAcceso revocarAutorizacion;

    public ControladorTriaje(CrearEvaluacion crearEvaluacion,
                             RegistrarRespuestaEvaluacion registrarRespuesta,
                             FinalizarEvaluacion finalizarEvaluacion,
                             ListarEvaluacionesUsuario listarEvaluaciones,
                             ObtenerDetalleEvaluacion obtenerDetalleEvaluacion,
                             GenerarReportePdfEvaluacion generarReportePdfEvaluacion,
                             OtorgarAutorizacionAcceso otorgarAutorizacion,
                             ListarAutorizacionesEvaluacion listarAutorizaciones,
                             RevocarAutorizacionAcceso revocarAutorizacion) {
        this.crearEvaluacion = crearEvaluacion;
        this.registrarRespuesta = registrarRespuesta;
        this.finalizarEvaluacion = finalizarEvaluacion;
        this.listarEvaluaciones = listarEvaluaciones;
        this.obtenerDetalleEvaluacion = obtenerDetalleEvaluacion;
        this.generarReportePdfEvaluacion = generarReportePdfEvaluacion;
        this.otorgarAutorizacion = otorgarAutorizacion;
        this.listarAutorizaciones = listarAutorizaciones;
        this.revocarAutorizacion = revocarAutorizacion;
    }

    @PostMapping
    public ResponseEntity<RespuestaEvaluacionResumen> crear(
            @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        DatosEvaluacion datos = crearEvaluacion.ejecutar(usuario.id());
        REGISTRO.info("Evaluacion iniciada exitosamente. Id: {}", datos.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(RespuestaEvaluacionResumen.desde(datos));
    }

    @PutMapping("/{id}/respuestas")
    public ResponseEntity<RespuestaRegistroItem> registrarRespuestaItem(
            @PathVariable String id,
            @Valid @RequestBody SolicitudRespuesta solicitud,
            @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        ComandoRegistrarRespuesta comando = new ComandoRegistrarRespuesta(
                id,
                usuario.id(),
                solicitud.instrumento(),
                solicitud.indicePregunta(),
                solicitud.valorPuntaje()
        );
        ResultadoRegistroRespuesta resultado = registrarRespuesta.ejecutar(comando);
        REGISTRO.debug("Respuesta tecnica registrada para evaluacion. Id: {}", id);
        return ResponseEntity.ok(RespuestaRegistroItem.desde(resultado));
    }

    @PostMapping("/{id}/finalizar")
    public ResponseEntity<RespuestaEvaluacionResumen> finalizar(
            @PathVariable String id,
            @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        DatosEvaluacion datos = finalizarEvaluacion.ejecutar(id, usuario.id());
        REGISTRO.info("Evaluacion completada exitosamente. Id: {}", datos.id());
        return ResponseEntity.ok(RespuestaEvaluacionResumen.desde(datos));
    }

    @GetMapping
    public ResponseEntity<List<RespuestaEvaluacionResumen>> listar(
            @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        List<RespuestaEvaluacionResumen> lista = listarEvaluaciones.ejecutar(usuario.id()).stream()
                .map(RespuestaEvaluacionResumen::desde)
                .toList();
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RespuestaEvaluacionDetalle> obtenerPorId(
            @PathVariable String id,
            @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        DetalleEvaluacion detalle = obtenerDetalleEvaluacion.ejecutar(id, usuario.id());
        return ResponseEntity.ok(RespuestaEvaluacionDetalle.desde(detalle));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> descargarPdf(
            @PathVariable String id,
            @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        byte[] pdf = generarReportePdfEvaluacion.ejecutar(id, usuario.id());
        REGISTRO.info("Reporte PDF generado exitosamente. Id: {}", id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte-cribado.pdf\"")
                .body(pdf);
    }

    @PostMapping("/{id}/autorizaciones")
    public ResponseEntity<RespuestaAutorizacion> otorgarAutorizacion(
            @PathVariable String id,
            @Valid @RequestBody SolicitudOtorgarAutorizacion solicitud,
            @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        ComandoOtorgarAutorizacion comando = new ComandoOtorgarAutorizacion(
                id,
                usuario.id(),
                solicitud.clinicoId(),
                Boolean.TRUE.equals(solicitud.confirmaAutorizacion())
        );
        DatosAutorizacion datos = otorgarAutorizacion.ejecutar(comando);
        REGISTRO.info("Autorizacion de acceso otorgada exitosamente. EvaluacionId: {}, ClinicoId: {}", id, solicitud.clinicoId());
        return ResponseEntity.status(HttpStatus.CREATED).body(RespuestaAutorizacion.desde(datos));
    }

    @GetMapping("/{id}/autorizaciones")
    public ResponseEntity<List<RespuestaAutorizacion>> listarAutorizaciones(
            @PathVariable String id,
            @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        List<RespuestaAutorizacion> lista = listarAutorizaciones.ejecutar(id, usuario.id()).stream()
                .map(RespuestaAutorizacion::desde)
                .toList();
        return ResponseEntity.ok(lista);
    }

    @PostMapping("/{id}/autorizaciones/{clinicoId}/revocar")
    public ResponseEntity<RespuestaAutorizacion> revocarAutorizacion(
            @PathVariable String id,
            @PathVariable Long clinicoId,
            @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        DatosAutorizacion datos = revocarAutorizacion.ejecutar(id, clinicoId, usuario.id());
        REGISTRO.info("Autorizacion de acceso revocada exitosamente. EvaluacionId: {}, ClinicoId: {}", id, clinicoId);
        return ResponseEntity.ok(RespuestaAutorizacion.desde(datos));
    }
}
