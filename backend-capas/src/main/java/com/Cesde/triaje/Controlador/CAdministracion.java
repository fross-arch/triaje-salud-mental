package com.Cesde.triaje.Controlador;

import com.Cesde.triaje.Dto.RespuestaClinico;
import com.Cesde.triaje.Dto.RespuestaPacienteAdministracion;
import com.Cesde.triaje.Dto.RespuestaTablero;
import com.Cesde.triaje.Dto.SolicitudActualizarClinico;
import com.Cesde.triaje.Dto.SolicitudActualizarPaciente;
import com.Cesde.triaje.Dto.SolicitudCrearClinico;
import com.Cesde.triaje.Seguridad.UsuarioAutenticado;
import com.Cesde.triaje.Servicio.SAdministracion;
import com.Cesde.triaje.Servicio.STablero;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/administracion")
public class CAdministracion {

    private static final Logger REGISTRO = LoggerFactory.getLogger(CAdministracion.class);

    private final SAdministracion administracionServicio;
    private final STablero tableroServicio;

    public CAdministracion(
            SAdministracion administracionServicio,
            STablero tableroServicio) {
        this.administracionServicio = administracionServicio;
        this.tableroServicio = tableroServicio;
    }

    // ==========================================
    // GESTIÓN DE CLÍNICOS
    // ==========================================

    @GetMapping("/clinicos")
    public ResponseEntity<List<RespuestaClinico>> listarClinicos() {
        List<RespuestaClinico> lista = administracionServicio.listarClinicos();
        return ResponseEntity.ok(lista);
    }

    @PostMapping("/clinicos")
    public ResponseEntity<RespuestaClinico> crearClinico(
            @Valid @RequestBody SolicitudCrearClinico solicitud,
            @AuthenticationPrincipal UsuarioAutenticado admin) {
        RespuestaClinico respuesta = administracionServicio.crearClinico(solicitud, admin.id());
        REGISTRO.info("Clínico creado por administrador id: {}", admin.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @PutMapping("/clinicos/{id}")
    public ResponseEntity<RespuestaClinico> actualizarClinico(
            @PathVariable Long id,
            @Valid @RequestBody SolicitudActualizarClinico solicitud,
            @AuthenticationPrincipal UsuarioAutenticado admin) {
        RespuestaClinico respuesta = administracionServicio.actualizarClinico(id, solicitud, admin.id());
        REGISTRO.info("Clínico actualizado id: {} por administrador id: {}", id, admin.id());
        return ResponseEntity.ok(respuesta);
    }

    @DeleteMapping("/clinicos/{id}")
    public ResponseEntity<Void> eliminarClinico(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioAutenticado admin) {
        administracionServicio.eliminarClinico(id, admin.id());
        REGISTRO.info("Clínico eliminado id: {} por administrador id: {}", id, admin.id());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/clinicos/{id}/activar")
    public ResponseEntity<RespuestaClinico> activarClinico(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioAutenticado admin) {
        RespuestaClinico respuesta = administracionServicio.cambiarEstadoClinico(id, true, admin.id());
        REGISTRO.info("Clínico activado id: {} por administrador id: {}", id, admin.id());
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/clinicos/{id}/desactivar")
    public ResponseEntity<RespuestaClinico> desactivarClinico(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioAutenticado admin) {
        RespuestaClinico respuesta = administracionServicio.cambiarEstadoClinico(id, false, admin.id());
        REGISTRO.info("Clínico desactivado id: {} por administrador id: {}", id, admin.id());
        return ResponseEntity.ok(respuesta);
    }

    // ==========================================
    // GESTIÓN DE PACIENTES
    // ==========================================

    @GetMapping("/pacientes")
    public ResponseEntity<List<RespuestaPacienteAdministracion>> listarPacientes() {
        List<RespuestaPacienteAdministracion> lista = administracionServicio.listarPacientes();
        return ResponseEntity.ok(lista);
    }

    @PutMapping("/pacientes/{id}")
    public ResponseEntity<RespuestaPacienteAdministracion> actualizarPaciente(
            @PathVariable Long id,
            @Valid @RequestBody SolicitudActualizarPaciente solicitud,
            @AuthenticationPrincipal UsuarioAutenticado admin) {
        RespuestaPacienteAdministracion respuesta = administracionServicio.actualizarPaciente(id, solicitud, admin.id());
        REGISTRO.info("Paciente actualizado id: {} por administrador id: {}", id, admin.id());
        return ResponseEntity.ok(respuesta);
    }

    // ==========================================
    // TABLERO ESTADÍSTICO
    // ==========================================

    @GetMapping("/tablero")
    public ResponseEntity<RespuestaTablero> obtenerTablero(
            @RequestParam(name = "dias", defaultValue = "30") int dias,
            @AuthenticationPrincipal UsuarioAutenticado admin) {
        RespuestaTablero respuesta = tableroServicio.obtenerTablero(admin.id(), dias);
        REGISTRO.info("Tablero consultado por administrador id: {} para periodo de {} días", admin.id(), dias);
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/tablero/exportar")
    public ResponseEntity<byte[]> exportarTablero(
            @RequestParam(name = "dias", defaultValue = "30") int dias,
            @RequestParam(name = "formato", defaultValue = "csv") String formato,
            @AuthenticationPrincipal UsuarioAutenticado admin) {
        STablero.ArchivoExportado archivo = tableroServicio.exportarTablero(admin.id(), dias, formato);
        REGISTRO.info("Tablero exportado por administrador id: {} en formato {} para {} días", admin.id(), formato, dias);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + archivo.nombreArchivo() + "\"")
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, archivo.tipoContenido())
                .body(archivo.contenido());
    }
}

