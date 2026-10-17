package com.triaje.infraestructura.web.controlador;

import com.triaje.aplicacion.casouso.ConsultarConsentimientoVigente;
import com.triaje.aplicacion.casouso.RegistrarConsentimiento;
import com.triaje.aplicacion.casouso.RevocarConsentimiento;
import com.triaje.aplicacion.dto.ComandoConsentimiento;
import com.triaje.aplicacion.dto.DatosConsentimiento;
import com.triaje.infraestructura.seguridad.UsuarioAutenticado;
import com.triaje.infraestructura.web.dto.RespuestaConsentimiento;
import com.triaje.infraestructura.web.dto.RespuestaConsentimientoVigente;
import com.triaje.infraestructura.web.dto.SolicitudConsentimiento;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/consentimientos")
public class ControladorConsentimientos {

    private static final Logger REGISTRO = LoggerFactory.getLogger(ControladorConsentimientos.class);

    private final RegistrarConsentimiento registrarConsentimiento;
    private final ConsultarConsentimientoVigente consultarConsentimientoVigente;
    private final RevocarConsentimiento revocarConsentimiento;

    public ControladorConsentimientos(RegistrarConsentimiento registrarConsentimiento,
                                      ConsultarConsentimientoVigente consultarConsentimientoVigente,
                                      RevocarConsentimiento revocarConsentimiento) {
        this.registrarConsentimiento = registrarConsentimiento;
        this.consultarConsentimientoVigente = consultarConsentimientoVigente;
        this.revocarConsentimiento = revocarConsentimiento;
    }

    @PostMapping
    public ResponseEntity<RespuestaConsentimiento> registrar(
            @Valid @RequestBody SolicitudConsentimiento solicitud,
            @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        ComandoConsentimiento comando = new ComandoConsentimiento(usuario.id(), solicitud.confirmaMayoriaEdad());
        DatosConsentimiento datos = registrarConsentimiento.ejecutar(comando);
        REGISTRO.info("Consentimiento registrado exitosamente. Id: {}", datos.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(RespuestaConsentimiento.desde(datos));
    }

    @GetMapping("/vigente")
    public ResponseEntity<RespuestaConsentimientoVigente> consultarVigente(
            @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        return consultarConsentimientoVigente.ejecutar(usuario.id())
                .map(datos -> ResponseEntity.ok(RespuestaConsentimientoVigente.activo(datos)))
                .orElseGet(() -> ResponseEntity.ok(RespuestaConsentimientoVigente.inactivo()));
    }

    @PostMapping("/revocar")
    public ResponseEntity<Void> revocar(
            @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        revocarConsentimiento.ejecutar(usuario.id());
        REGISTRO.info("Consentimiento revocado exitosamente.");
        return ResponseEntity.ok().build();
    }
}
