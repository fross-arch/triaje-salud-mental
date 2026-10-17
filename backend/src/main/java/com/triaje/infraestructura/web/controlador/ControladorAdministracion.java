package com.triaje.infraestructura.web.controlador;

import com.triaje.aplicacion.casouso.ActualizarClinico;
import com.triaje.aplicacion.casouso.CambiarEstadoClinico;
import com.triaje.aplicacion.casouso.CrearClinico;
import com.triaje.aplicacion.casouso.EliminarClinico;
import com.triaje.aplicacion.casouso.ListarClinicos;
import com.triaje.aplicacion.dto.ComandoActualizarClinico;
import com.triaje.aplicacion.dto.ComandoCrearClinico;
import com.triaje.aplicacion.dto.DatosUsuario;
import com.triaje.infraestructura.seguridad.UsuarioAutenticado;
import com.triaje.infraestructura.web.dto.RespuestaClinico;
import com.triaje.infraestructura.web.dto.SolicitudActualizarClinico;
import com.triaje.infraestructura.web.dto.SolicitudCrearClinico;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints de administración de cuentas clínicas.
 * Acceso restringido al rol ADMINISTRADOR.
 */
@RestController
@RequestMapping("/api/administracion/clinicos")
public class ControladorAdministracion {

    private static final Logger REGISTRO = LoggerFactory.getLogger(ControladorAdministracion.class);

    private final CrearClinico crearClinico;
    private final ListarClinicos listarClinicos;
    private final CambiarEstadoClinico cambiarEstadoClinico;
    private final ActualizarClinico actualizarClinico;
    private final EliminarClinico eliminarClinico;

    public ControladorAdministracion(CrearClinico crearClinico,
                                     ListarClinicos listarClinicos,
                                     CambiarEstadoClinico cambiarEstadoClinico,
                                     ActualizarClinico actualizarClinico,
                                     EliminarClinico eliminarClinico) {
        this.crearClinico = crearClinico;
        this.listarClinicos = listarClinicos;
        this.cambiarEstadoClinico = cambiarEstadoClinico;
        this.actualizarClinico = actualizarClinico;
        this.eliminarClinico = eliminarClinico;
    }

    @PostMapping
    public ResponseEntity<RespuestaClinico> crear(
            @Valid @RequestBody SolicitudCrearClinico solicitud,
            @AuthenticationPrincipal UsuarioAutenticado admin
    ) {
        ComandoCrearClinico comando = new ComandoCrearClinico(
                admin.id(),
                solicitud.nombreCompleto(),
                solicitud.correo(),
                solicitud.contrasenaInicial(),
                solicitud.especialidad()
        );
        DatosUsuario datos = crearClinico.ejecutar(comando);
        REGISTRO.info("Cuenta clinica creada exitosamente. Id: {}", datos.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(RespuestaClinico.desde(datos));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RespuestaClinico> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody SolicitudActualizarClinico solicitud,
            @AuthenticationPrincipal UsuarioAutenticado admin
    ) {
        ComandoActualizarClinico comando = new ComandoActualizarClinico(
                admin.id(),
                id,
                solicitud.nombreCompleto(),
                solicitud.correo(),
                solicitud.especialidad(),
                solicitud.nuevaContrasena()
        );
        DatosUsuario datos = actualizarClinico.ejecutar(comando);
        REGISTRO.info("Cuenta clinica actualizada exitosamente. Id: {}", id);
        return ResponseEntity.ok(RespuestaClinico.desde(datos));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioAutenticado admin
    ) {
        eliminarClinico.ejecutar(admin.id(), id);
        REGISTRO.info("Cuenta clinica eliminada exitosamente. Id: {}", id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<RespuestaClinico>> listar() {
        List<RespuestaClinico> clinicos = listarClinicos.ejecutar().stream()
                .map(RespuestaClinico::desde)
                .toList();
        return ResponseEntity.ok(clinicos);
    }

    @PostMapping("/{id}/desactivar")
    public ResponseEntity<RespuestaClinico> desactivar(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioAutenticado admin
    ) {
        DatosUsuario datos = cambiarEstadoClinico.ejecutar(admin.id(), id, false);
        REGISTRO.info("Cuenta clinica desactivada exitosamente. Id: {}", id);
        return ResponseEntity.ok(RespuestaClinico.desde(datos));
    }

    @PostMapping("/{id}/activar")
    public ResponseEntity<RespuestaClinico> activar(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioAutenticado admin
    ) {
        DatosUsuario datos = cambiarEstadoClinico.ejecutar(admin.id(), id, true);
        REGISTRO.info("Cuenta clinica activada exitosamente. Id: {}", id);
        return ResponseEntity.ok(RespuestaClinico.desde(datos));
    }
}
