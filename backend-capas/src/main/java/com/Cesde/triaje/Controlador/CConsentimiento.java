package com.Cesde.triaje.Controlador;

import com.Cesde.triaje.Dto.RespuestaConsentimiento;
import com.Cesde.triaje.Dto.RespuestaConsentimientoVigente;
import com.Cesde.triaje.Dto.SolicitudConsentimiento;
import com.Cesde.triaje.Seguridad.UsuarioAutenticado;
import com.Cesde.triaje.Servicio.SConsentimiento;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/consentimientos")
public class CConsentimiento {

    private static final Logger REGISTRO = LoggerFactory.getLogger(CConsentimiento.class);

    private final SConsentimiento consentimientoServicio;

    public CConsentimiento(SConsentimiento consentimientoServicio) {
        this.consentimientoServicio = consentimientoServicio;
    }

    @PostMapping
    public ResponseEntity<RespuestaConsentimiento> registrar(
            @Valid @RequestBody SolicitudConsentimiento solicitud,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        RespuestaConsentimiento respuesta = consentimientoServicio.registrarConsentimiento(usuario.id(), solicitud);
        REGISTRO.info("Consentimiento registrado exitosamente.");
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping("/vigente")
    public ResponseEntity<RespuestaConsentimientoVigente> consultarVigente(
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        RespuestaConsentimientoVigente respuesta = consentimientoServicio.consultarVigente(usuario.id());
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/revocar")
    public ResponseEntity<java.util.Map<String, String>> revocar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        consentimientoServicio.revocar(usuario.id());
        REGISTRO.info("Consentimiento revocado exitosamente.");
        return ResponseEntity.ok(java.util.Map.of("mensaje", "Consentimiento revocado exitosamente."));
    }
}
