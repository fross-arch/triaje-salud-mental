package com.triaje.infraestructura.web.controlador;

import com.triaje.aplicacion.casouso.CancelarCitaClinico;
import com.triaje.aplicacion.casouso.CumplirCitaClinico;
import com.triaje.aplicacion.casouso.ListarCitasClinico;
import com.triaje.aplicacion.casouso.PosponerCitaClinico;
import com.triaje.aplicacion.casouso.ProponerCita;
import com.triaje.aplicacion.dto.ComandoPosponerCita;
import com.triaje.aplicacion.dto.ComandoProponerCita;
import com.triaje.aplicacion.dto.DatosCitaClinico;
import com.triaje.infraestructura.seguridad.UsuarioAutenticado;
import com.triaje.infraestructura.web.dto.RespuestaCitaClinico;
import com.triaje.infraestructura.web.dto.SolicitudCancelarCitaClinico;
import com.triaje.infraestructura.web.dto.SolicitudCumplirCita;
import com.triaje.infraestructura.web.dto.SolicitudPosponerCita;
import com.triaje.infraestructura.web.dto.SolicitudProponerCita;
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

/**
 * Endpoints del portal clinico para gestion de citas de cribado.
 * Acceso restringido al rol CLINICO.
 */
@RestController
@RequestMapping("/api/clinico/citas")
public class ControladorClinicoCitas {

    private static final Logger REGISTRO = LoggerFactory.getLogger(ControladorClinicoCitas.class);

    private final ProponerCita proponerCita;
    private final ListarCitasClinico listarCitasClinico;
    private final CancelarCitaClinico cancelarCitaClinico;
    private final CumplirCitaClinico cumplirCitaClinico;
    private final PosponerCitaClinico posponerCitaClinico;

    public ControladorClinicoCitas(ProponerCita proponerCita,
                                   ListarCitasClinico listarCitasClinico,
                                   CancelarCitaClinico cancelarCitaClinico,
                                   CumplirCitaClinico cumplirCitaClinico,
                                   PosponerCitaClinico posponerCitaClinico) {
        this.proponerCita = proponerCita;
        this.listarCitasClinico = listarCitasClinico;
        this.cancelarCitaClinico = cancelarCitaClinico;
        this.cumplirCitaClinico = cumplirCitaClinico;
        this.posponerCitaClinico = posponerCitaClinico;
    }

    @PostMapping
    public ResponseEntity<RespuestaCitaClinico> proponer(
            @Valid @RequestBody SolicitudProponerCita solicitud,
            @AuthenticationPrincipal UsuarioAutenticado clinico
    ) {
        ComandoProponerCita comando = new ComandoProponerCita(
                solicitud.evaluacionId(),
                clinico.id(),
                solicitud.fechaHora(),
                solicitud.modalidad(),
                solicitud.mensaje()
        );
        DatosCitaClinico datos = proponerCita.ejecutar(comando);
        REGISTRO.info("Cita propuesta exitosamente por clinico. CitaId: {}", datos.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(RespuestaCitaClinico.desde(datos));
    }

    @GetMapping
    public ResponseEntity<List<RespuestaCitaClinico>> listar(
            @AuthenticationPrincipal UsuarioAutenticado clinico
    ) {
        List<DatosCitaClinico> citas = listarCitasClinico.ejecutar(clinico.id());
        List<RespuestaCitaClinico> respuesta = citas.stream()
                .map(RespuestaCitaClinico::desde)
                .toList();
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/{id}/cumplir")
    public ResponseEntity<RespuestaCitaClinico> cumplir(
            @PathVariable String id,
            @Valid @RequestBody SolicitudCumplirCita solicitud,
            @AuthenticationPrincipal UsuarioAutenticado clinico
    ) {
        DatosCitaClinico cumplida = cumplirCitaClinico.ejecutar(id, clinico.id(), solicitud.observaciones());
        REGISTRO.info("Cita marcada como cumplida exitosamente por clinico. CitaId: {}", id);
        return ResponseEntity.ok(RespuestaCitaClinico.desde(cumplida));
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<RespuestaCitaClinico> cancelar(
            @PathVariable String id,
            @RequestBody(required = false) @Valid SolicitudCancelarCitaClinico solicitud,
            @AuthenticationPrincipal UsuarioAutenticado clinico
    ) {
        String motivo = solicitud != null ? solicitud.motivoEfectivo() : null;
        DatosCitaClinico cancelada = cancelarCitaClinico.ejecutar(id, clinico.id(), motivo);
        REGISTRO.info("Cita cancelada exitosamente por clinico. CitaId: {}", id);
        return ResponseEntity.ok(RespuestaCitaClinico.desde(cancelada));
    }

    @PutMapping("/{id}/posponer")
    public ResponseEntity<RespuestaCitaClinico> posponer(
            @PathVariable String id,
            @Valid @RequestBody SolicitudPosponerCita solicitud,
            @AuthenticationPrincipal UsuarioAutenticado clinico
    ) {
        ComandoPosponerCita comando = new ComandoPosponerCita(
                id,
                clinico.id(),
                solicitud.nuevaFechaHora(),
                solicitud.mensajeEfectivo()
        );
        DatosCitaClinico pospuesta = posponerCitaClinico.ejecutar(comando);
        REGISTRO.info("Cita pospuesta exitosamente por clinico. CitaId: {}", id);
        return ResponseEntity.ok(RespuestaCitaClinico.desde(pospuesta));
    }
}
