package com.triaje.infraestructura.web.controlador;

import com.triaje.aplicacion.casouso.ActualizarPaciente;
import com.triaje.aplicacion.casouso.ListarPacientes;
import com.triaje.aplicacion.dto.ComandoActualizarPaciente;
import com.triaje.aplicacion.dto.DatosPacienteAdministracion;
import com.triaje.infraestructura.seguridad.UsuarioAutenticado;
import com.triaje.infraestructura.web.dto.RespuestaPacienteAdministracion;
import com.triaje.infraestructura.web.dto.SolicitudActualizarPaciente;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoint de administracion para consultar y gestionar cuentas de pacientes registrados.
 * Acceso restringido al rol ADMINISTRADOR.
 */
@RestController
@RequestMapping("/api/administracion/pacientes")
public class ControladorAdministracionPacientes {

    private static final Logger REGISTRO = LoggerFactory.getLogger(ControladorAdministracionPacientes.class);

    private final ListarPacientes listarPacientes;
    private final ActualizarPaciente actualizarPaciente;

    public ControladorAdministracionPacientes(ListarPacientes listarPacientes, ActualizarPaciente actualizarPaciente) {
        this.listarPacientes = listarPacientes;
        this.actualizarPaciente = actualizarPaciente;
    }

    @GetMapping
    public ResponseEntity<List<RespuestaPacienteAdministracion>> listar() {
        List<RespuestaPacienteAdministracion> pacientes = listarPacientes.ejecutar().stream()
                .map(RespuestaPacienteAdministracion::desde)
                .toList();
        return ResponseEntity.ok(pacientes);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RespuestaPacienteAdministracion> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody SolicitudActualizarPaciente solicitud,
            @AuthenticationPrincipal UsuarioAutenticado admin
    ) {
        ComandoActualizarPaciente comando = new ComandoActualizarPaciente(
                admin.id(),
                id,
                solicitud.nombreCompleto(),
                solicitud.correo(),
                solicitud.nuevaContrasena()
        );
        DatosPacienteAdministracion datos = actualizarPaciente.ejecutar(comando);
        REGISTRO.info("Cuenta de paciente actualizada exitosamente. Id: {}", id);
        return ResponseEntity.ok(RespuestaPacienteAdministracion.desde(datos));
    }
}
