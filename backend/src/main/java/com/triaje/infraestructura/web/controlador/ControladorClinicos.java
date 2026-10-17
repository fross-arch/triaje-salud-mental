package com.triaje.infraestructura.web.controlador;

import com.triaje.aplicacion.casouso.ListarClinicosActivos;
import com.triaje.infraestructura.web.dto.RespuestaClinicoActivo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoint para que los pacientes consulten los clinicos activos disponibles.
 */
@RestController
@RequestMapping("/api/clinicos")
public class ControladorClinicos {

    private final ListarClinicosActivos listarClinicosActivos;

    public ControladorClinicos(ListarClinicosActivos listarClinicosActivos) {
        this.listarClinicosActivos = listarClinicosActivos;
    }

    @GetMapping
    public ResponseEntity<List<RespuestaClinicoActivo>> listarActivos() {
        List<RespuestaClinicoActivo> lista = listarClinicosActivos.ejecutar().stream()
                .map(RespuestaClinicoActivo::desde)
                .toList();
        return ResponseEntity.ok(lista);
    }
}
