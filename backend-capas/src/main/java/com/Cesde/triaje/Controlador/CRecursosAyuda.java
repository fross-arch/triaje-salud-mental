package com.Cesde.triaje.Controlador;

import com.Cesde.triaje.Configuracion.PropiedadesRecursosAyuda;
import com.Cesde.triaje.Dto.RespuestaRecursoAyuda;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recursos-ayuda")
public class CRecursosAyuda {

    private final PropiedadesRecursosAyuda propiedades;

    public CRecursosAyuda(PropiedadesRecursosAyuda propiedades) {
        this.propiedades = propiedades;
    }

    @GetMapping
    public ResponseEntity<List<RespuestaRecursoAyuda>> obtenerRecursos() {
        List<RespuestaRecursoAyuda> lista = propiedades.getRecursosAyuda().stream()
                .map(r -> new RespuestaRecursoAyuda(
                        r.getNombre(),
                        r.getTelefono(),
                        r.getHorario(),
                        r.getDescripcion(),
                        r.getAmbito()
                ))
                .toList();
        return ResponseEntity.ok(lista);
    }
}
