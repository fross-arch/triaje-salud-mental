package com.Cesde.triaje.Controlador;

import com.Cesde.triaje.Dto.RespuestaCitaClinico;
import com.Cesde.triaje.Dto.SolicitudCancelarCitaClinico;
import com.Cesde.triaje.Dto.SolicitudCumplirCita;
import com.Cesde.triaje.Dto.SolicitudPosponerCita;
import com.Cesde.triaje.Dto.SolicitudProponerCita;
import com.Cesde.triaje.Seguridad.UsuarioAutenticado;
import com.Cesde.triaje.Servicio.SCita;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/clinico/citas")
public class CClinicoCita {

    private static final Logger REGISTRO = LoggerFactory.getLogger(CClinicoCita.class);

    private final SCita citaServicio;

    public CClinicoCita(SCita citaServicio) {
        this.citaServicio = citaServicio;
    }

    @PostMapping
    public ResponseEntity<RespuestaCitaClinico> proponerCita(
            @Valid @RequestBody SolicitudProponerCita solicitud,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        RespuestaCitaClinico respuesta = citaServicio.proponerCita(usuario.id(), solicitud);
        REGISTRO.info("Cita propuesta por clínico exitosamente.");
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping
    public ResponseEntity<List<RespuestaCitaClinico>> listarCitas(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        List<RespuestaCitaClinico> lista = citaServicio.listarCitasClinico(usuario.id());
        return ResponseEntity.ok(lista);
    }

    @PostMapping("/{id}/cumplir")
    public ResponseEntity<RespuestaCitaClinico> cumplir(
            @PathVariable String id,
            @Valid @RequestBody SolicitudCumplirCita solicitud,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        RespuestaCitaClinico respuesta = citaServicio.cumplirCita(id, usuario.id(), solicitud);
        REGISTRO.info("Cita cumplida por clínico exitosamente.");
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<RespuestaCitaClinico> cancelar(
            @PathVariable String id,
            @RequestBody(required = false) SolicitudCancelarCitaClinico solicitud,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        RespuestaCitaClinico respuesta = citaServicio.cancelarCitaClinico(id, usuario.id(), solicitud);
        REGISTRO.info("Cita cancelada por clínico exitosamente.");
        return ResponseEntity.ok(respuesta);
    }

    @PutMapping("/{id}/posponer")
    public ResponseEntity<RespuestaCitaClinico> posponer(
            @PathVariable String id,
            @Valid @RequestBody SolicitudPosponerCita solicitud,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        RespuestaCitaClinico respuesta = citaServicio.posponerCita(id, usuario.id(), solicitud);
        REGISTRO.info("Cita pospuesta por clínico exitosamente.");
        return ResponseEntity.ok(respuesta);
    }
}
