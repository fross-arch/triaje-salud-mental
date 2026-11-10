package com.Cesde.triaje.Controlador;

import com.Cesde.triaje.Dto.RespuestaClinicoActivo;
import com.Cesde.triaje.Servicio.SClinico;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/clinicos")
public class CClinico {

    private final SClinico clinicoServicio;

    public CClinico(SClinico clinicoServicio) {
        this.clinicoServicio = clinicoServicio;
    }

    @GetMapping
    public ResponseEntity<List<RespuestaClinicoActivo>> listarActivos() {
        List<RespuestaClinicoActivo> lista = clinicoServicio.listarActivos();
        return ResponseEntity.ok(lista);
    }
}
