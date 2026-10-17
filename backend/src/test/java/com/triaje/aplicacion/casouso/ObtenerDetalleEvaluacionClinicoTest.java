package com.triaje.aplicacion.casouso;

import com.triaje.aplicacion.dto.DetalleEvaluacionClinico;
import com.triaje.aplicacion.dto.DetalleEvaluacionClinico.ItemRespuestaClinico;
import com.triaje.aplicacion.puerto.EjecutorTransaccional;
import com.triaje.aplicacion.puerto.RepositorioAuditoria;
import com.triaje.aplicacion.puerto.RepositorioAutorizaciones;
import com.triaje.aplicacion.puerto.RepositorioEvaluaciones;
import com.triaje.aplicacion.puerto.RepositorioRespuestasEvaluacion;
import com.triaje.aplicacion.puerto.RepositorioUsuarios;
import com.triaje.dominio.Autorizacion;
import com.triaje.dominio.PoliticaAccesoClinico;
import com.triaje.dominio.ReglaCrisis;
import com.triaje.dominio.ReglaResultadoMinimo;
import com.triaje.dominio.RolUsuario;
import com.triaje.dominio.SeveridadAnsiedad;
import com.triaje.dominio.SeveridadDepresion;
import com.triaje.dominio.modelo.EstadoEvaluacion;
import com.triaje.dominio.modelo.Evaluacion;
import com.triaje.dominio.modelo.InstrumentoCribado;
import com.triaje.dominio.modelo.RespuestaEvaluacion;
import com.triaje.dominio.modelo.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ObtenerDetalleEvaluacionClinicoTest {

    private RepositorioAutorizaciones repositorioAutorizaciones;
    private RepositorioEvaluaciones repositorioEvaluaciones;
    private RepositorioUsuarios repositorioUsuarios;
    private RepositorioRespuestasEvaluacion repositorioRespuestas;
    private RepositorioAuditoria repositorioAuditoria;
    private PoliticaAccesoClinico politicaAccesoClinico;
    private ReglaResultadoMinimo reglaResultadoMinimo;
    private Clock reloj;
    private ObtenerDetalleEvaluacionClinico casoUso;

    @BeforeEach
    void setUp() {
        repositorioAutorizaciones = mock(RepositorioAutorizaciones.class);
        repositorioEvaluaciones = mock(RepositorioEvaluaciones.class);
        repositorioUsuarios = mock(RepositorioUsuarios.class);
        repositorioRespuestas = mock(RepositorioRespuestasEvaluacion.class);
        repositorioAuditoria = mock(RepositorioAuditoria.class);
        politicaAccesoClinico = new PoliticaAccesoClinico();
        reglaResultadoMinimo = new ReglaResultadoMinimo(new ReglaCrisis());
        EjecutorTransaccional transaccional = new EjecutorTransaccional() {
            @Override
            public <T> T ejecutar(Supplier<T> operacion) {
                return operacion.get();
            }
        };
        reloj = Clock.fixed(Instant.parse("2026-10-16T12:00:00Z"), ZoneOffset.UTC);

        casoUso = new ObtenerDetalleEvaluacionClinico(
                repositorioAutorizaciones,
                repositorioEvaluaciones,
                repositorioUsuarios,
                repositorioRespuestas,
                repositorioAuditoria,
                politicaAccesoClinico,
                reglaResultadoMinimo,
                transaccional,
                reloj
        );
    }

    @Test
    @DisplayName("Debe retornar las 16 respuestas completas con instrumento PHQ9 y GAD7, enunciados oficiales y textos de frecuencia")
    void debeRetornar16RespuestasCompletasConEnunciadosYFrecuencias() {
        String evaluacionId = "eval-test-16";
        Long clinicoId = 42L;
        Long pacienteId = 10L;

        Autorizacion autorizacion = Autorizacion.nueva(
                evaluacionId,
                pacienteId,
                clinicoId,
                "1.0",
                Instant.parse("2026-10-16T10:00:00Z")
        );
        when(repositorioAutorizaciones.buscarVigentePorEvaluacionYClinico(evaluacionId, clinicoId))
                .thenReturn(Optional.of(autorizacion));

        Evaluacion evaluacion = new Evaluacion(
                evaluacionId,
                pacienteId,
                EstadoEvaluacion.COMPLETADA,
                12,
                8,
                SeveridadDepresion.MODERADA,
                SeveridadAnsiedad.LEVE,
                false,
                Instant.parse("2026-10-16T09:00:00Z"),
                Instant.parse("2026-10-16T09:30:00Z")
        );
        when(repositorioEvaluaciones.buscarPorId(evaluacionId)).thenReturn(Optional.of(evaluacion));

        Usuario paciente = new Usuario(
                pacienteId,
                "Juan Pérez",
                "juan@correo.com",
                "hash",
                RolUsuario.PACIENTE,
                true,
                Instant.parse("2026-10-01T00:00:00Z")
        );
        when(repositorioUsuarios.buscarPorId(pacienteId)).thenReturn(Optional.of(paciente));

        List<RespuestaEvaluacion> respuestas = new ArrayList<>();
        // 9 de PHQ-9
        for (int i = 1; i <= 9; i++) {
            respuestas.add(new RespuestaEvaluacion(
                    (long) i,
                    evaluacionId,
                    InstrumentoCribado.PHQ9,
                    i,
                    i % 4 // valores 0, 1, 2, 3
            ));
        }
        // 7 de GAD-7
        for (int i = 1; i <= 7; i++) {
            respuestas.add(new RespuestaEvaluacion(
                    (long) (9 + i),
                    evaluacionId,
                    InstrumentoCribado.GAD7,
                    i,
                    (i + 1) % 4
            ));
        }
        when(repositorioRespuestas.buscarPorEvaluacionId(evaluacionId)).thenReturn(respuestas);

        DetalleEvaluacionClinico detalle = casoUso.ejecutar(evaluacionId, clinicoId);

        assertThat(detalle).isNotNull();
        assertThat(detalle.evaluacionId()).isEqualTo(evaluacionId);
        assertThat(detalle.nombrePaciente()).isEqualTo("Juan Pérez");
        assertThat(detalle.respuestas()).hasSize(16);

        // Primeras 9 son PHQ9
        for (int i = 0; i < 9; i++) {
            ItemRespuestaClinico r = detalle.respuestas().get(i);
            assertThat(r.instrumento()).isEqualTo("PHQ9");
            assertThat(r.indicePregunta()).isEqualTo(i + 1);
            assertThat(r.enunciadoPregunta()).isNotEmpty();
            assertThat(r.valorPuntaje()).isBetween(0, 3);
            assertThat(r.textoFrecuencia()).isIn("Ningún día", "Varios días", "Más de la mitad de los días", "Casi todos los días");
        }

        // Siguientes 7 son GAD7
        for (int i = 0; i < 7; i++) {
            ItemRespuestaClinico r = detalle.respuestas().get(9 + i);
            assertThat(r.instrumento()).isEqualTo("GAD7");
            assertThat(r.indicePregunta()).isEqualTo(i + 1);
            assertThat(r.enunciadoPregunta()).isNotEmpty();
            assertThat(r.valorPuntaje()).isBetween(0, 3);
            assertThat(r.textoFrecuencia()).isIn("Ningún día", "Varios días", "Más de la mitad de los días", "Casi todos los días");
        }

        // Verificamos textos exactos en reactivos específicos
        ItemRespuestaClinico itemPhq1 = detalle.respuestas().get(0);
        assertThat(itemPhq1.enunciadoPregunta()).isEqualTo("Poco interés o placer en hacer cosas");
        assertThat(itemPhq1.valorPuntaje()).isEqualTo(1);
        assertThat(itemPhq1.textoFrecuencia()).isEqualTo("Varios días");

        ItemRespuestaClinico itemGad1 = detalle.respuestas().get(9);
        assertThat(itemGad1.enunciadoPregunta()).isEqualTo("Sentirse nervioso/a, intranquilo/a o con los nervios de punta");
        assertThat(itemGad1.valorPuntaje()).isEqualTo(2);
        assertThat(itemGad1.textoFrecuencia()).isEqualTo("Más de la mitad de los días");

        verify(repositorioAuditoria).registrar(any());
    }
}
