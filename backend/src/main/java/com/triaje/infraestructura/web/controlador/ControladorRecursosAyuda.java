package com.triaje.infraestructura.web.controlador;

import com.triaje.infraestructura.configuracion.PropiedadesRecursosAyuda;
import com.triaje.infraestructura.web.dto.RespuestaRecursoAyuda;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recursos-ayuda")
public class ControladorRecursosAyuda {

    private final PropiedadesRecursosAyuda propiedades;

    public ControladorRecursosAyuda(PropiedadesRecursosAyuda propiedades) {
        this.propiedades = propiedades;
    }

    @GetMapping
    public ResponseEntity<List<RespuestaRecursoAyuda>> obtenerRecursos() {
        List<RespuestaRecursoAyuda> lista = propiedades.recursosAyuda().stream()
                .map(RespuestaRecursoAyuda::desde)
                .toList();
        return ResponseEntity.ok(lista);
    }
}
