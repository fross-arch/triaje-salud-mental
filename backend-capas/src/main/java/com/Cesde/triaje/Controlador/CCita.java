package com.Cesde.triaje.Controlador;

import com.Cesde.triaje.Dto.RespuestaCitaPaciente;
import com.Cesde.triaje.Seguridad.UsuarioAutenticado;
import com.Cesde.triaje.Servicio.SCita;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/citas")
public class CCita {

    private static final Logger REGISTRO = LoggerFactory.getLogger(CCita.class);

    private final SCita citaServicio;

    public CCita(SCita citaServicio) {
        this.citaServicio = citaServicio;
    }

    @GetMapping
    public ResponseEntity<List<RespuestaCitaPaciente>> listarCitas(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        List<RespuestaCitaPaciente> citas = citaServicio.listarCitasPaciente(usuario.id());
        return ResponseEntity.ok(citas);
    }

    @PostMapping("/{id}/confirmar")
    public ResponseEntity<RespuestaCitaPaciente> confirmar(
            @PathVariable String id,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        RespuestaCitaPaciente respuesta = citaServicio.confirmarCita(id, usuario.id());
        REGISTRO.info("Cita confirmada exitosamente.");
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/{id}/rechazar")
    public ResponseEntity<RespuestaCitaPaciente> rechazar(
            @PathVariable String id,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        RespuestaCitaPaciente respuesta = citaServicio.rechazarCita(id, usuario.id());
        REGISTRO.info("Cita rechazada exitosamente.");
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<RespuestaCitaPaciente> cancelar(
            @PathVariable String id,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        RespuestaCitaPaciente respuesta = citaServicio.cancelarCitaPaciente(id, usuario.id());
        REGISTRO.info("Cita cancelada exitosamente.");
        return ResponseEntity.ok(respuesta);
    }
}
