package com.triaje.infraestructura.web.controlador;

import com.triaje.aplicacion.casouso.CancelarCitaPaciente;
import com.triaje.aplicacion.casouso.ConfirmarCitaPaciente;
import com.triaje.aplicacion.casouso.ListarCitasPaciente;
import com.triaje.aplicacion.casouso.RechazarCitaPaciente;
import com.triaje.aplicacion.dto.DatosCitaPaciente;
import com.triaje.infraestructura.seguridad.UsuarioAutenticado;
import com.triaje.infraestructura.web.dto.RespuestaCitaPaciente;
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

/**
 * Endpoints del paciente para consultar y gestionar sus citas de cribado.
 * Acceso restringido al rol PACIENTE.
 */
@RestController
@RequestMapping("/api/citas")
public class ControladorCitas {

    private static final Logger REGISTRO = LoggerFactory.getLogger(ControladorCitas.class);

    private final ListarCitasPaciente listarCitasPaciente;
    private final ConfirmarCitaPaciente confirmarCitaPaciente;
    private final RechazarCitaPaciente rechazarCitaPaciente;
    private final CancelarCitaPaciente cancelarCitaPaciente;

    public ControladorCitas(ListarCitasPaciente listarCitasPaciente,
                            ConfirmarCitaPaciente confirmarCitaPaciente,
                            RechazarCitaPaciente rechazarCitaPaciente,
                            CancelarCitaPaciente cancelarCitaPaciente) {
        this.listarCitasPaciente = listarCitasPaciente;
        this.confirmarCitaPaciente = confirmarCitaPaciente;
        this.rechazarCitaPaciente = rechazarCitaPaciente;
        this.cancelarCitaPaciente = cancelarCitaPaciente;
    }

    @GetMapping
    public ResponseEntity<List<RespuestaCitaPaciente>> listar(
            @AuthenticationPrincipal UsuarioAutenticado paciente
    ) {
        List<DatosCitaPaciente> citas = listarCitasPaciente.ejecutar(paciente.id());
        List<RespuestaCitaPaciente> respuesta = citas.stream()
                .map(RespuestaCitaPaciente::desde)
                .toList();
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/{id}/confirmar")
    public ResponseEntity<RespuestaCitaPaciente> confirmar(
            @PathVariable String id,
            @AuthenticationPrincipal UsuarioAutenticado paciente
    ) {
        DatosCitaPaciente confirmada = confirmarCitaPaciente.ejecutar(id, paciente.id());
        REGISTRO.info("Cita confirmada exitosamente por paciente. CitaId: {}", id);
        return ResponseEntity.ok(RespuestaCitaPaciente.desde(confirmada));
    }

    @PostMapping("/{id}/rechazar")
    public ResponseEntity<RespuestaCitaPaciente> rechazar(
            @PathVariable String id,
            @AuthenticationPrincipal UsuarioAutenticado paciente
    ) {
        DatosCitaPaciente rechazada = rechazarCitaPaciente.ejecutar(id, paciente.id());
        REGISTRO.info("Cita rechazada exitosamente por paciente. CitaId: {}", id);
        return ResponseEntity.ok(RespuestaCitaPaciente.desde(rechazada));
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<RespuestaCitaPaciente> cancelar(
            @PathVariable String id,
            @AuthenticationPrincipal UsuarioAutenticado paciente
    ) {
        DatosCitaPaciente cancelada = cancelarCitaPaciente.ejecutar(id, paciente.id());
        REGISTRO.info("Cita cancelada exitosamente por paciente. CitaId: {}", id);
        return ResponseEntity.ok(RespuestaCitaPaciente.desde(cancelada));
    }
}
