import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { DetalleEvaluacionClinico } from '../../pantallas/DetalleEvaluacionClinico.jsx';
import { servicioClinicoEvaluaciones } from '../../servicios/servicioClinicoEvaluaciones.js';
import { servicioCitas } from '../../servicios/servicioCitas.js';

vi.mock('../../servicios/servicioClinicoEvaluaciones.js', () => ({
  servicioClinicoEvaluaciones: {
    obtenerDetalle: vi.fn(),
    descargarPdf: vi.fn()
  }
}));

vi.mock('../../servicios/servicioCitas.js', () => ({
  servicioCitas: {
    listarCitasClinico: vi.fn(),
    proponerCita: vi.fn()
  }
}));

describe('DetalleEvaluacionClinico', () => {
  const detalleCompleto16Mock = {
    id: 'eval-clin-123',
    evaluacionId: 'eval-clin-123',
    pacienteId: 50,
    nombrePaciente: 'Paciente Prueba Detalle',
    correoPaciente: 'paciente@prueba.com',
    puntajePhq9: 14,
    severidadDepresion: 'MODERADA',
    puntajeGad7: 10,
    severidadAnsiedad: 'MODERADA',
    riesgoCrisis: true,
    fechaFinalizacion: '2026-10-15T12:00:00Z',
    respuestas: [
      // 9 ítems de PHQ-9
      {
        instrumento: 'PHQ9',
        indicePregunta: 1,
        enunciadoPregunta: 'Poco interés o placer en hacer cosas',
        valorPuntaje: 2,
        textoFrecuencia: 'Más de la mitad de los días'
      },
      {
        instrumento: 'PHQ9',
        indicePregunta: 2,
        enunciadoPregunta: 'Sentirse desanimado/a, deprimido/a o sin esperanza',
        valorPuntaje: 1,
        textoFrecuencia: 'Varios días'
      },
      {
        instrumento: 'PHQ9',
        indicePregunta: 3,
        enunciadoPregunta: 'Problemas para dormir o para mantenerse dormido/a, o dormir demasiado',
        valorPuntaje: 2,
        textoFrecuencia: 'Más de la mitad de los días'
      },
      {
        instrumento: 'PHQ9',
        indicePregunta: 4,
        enunciadoPregunta: 'Sentirse cansado/a o tener poca energía',
        valorPuntaje: 3,
        textoFrecuencia: 'Casi todos los días'
      },
      {
        instrumento: 'PHQ9',
        indicePregunta: 5,
        enunciadoPregunta: 'Poco apetito o comer en exceso',
        valorPuntaje: 0,
        textoFrecuencia: 'Ningún día'
      },
      {
        instrumento: 'PHQ9',
        indicePregunta: 6,
        enunciadoPregunta: 'Sentirse mal consigo mismo/a — o sentir que es un/a fracasado/a o que ha decepcionado a su familia o a usted mismo/a',
        valorPuntaje: 1,
        textoFrecuencia: 'Varios días'
      },
      {
        instrumento: 'PHQ9',
        indicePregunta: 7,
        enunciadoPregunta: 'Dificultad para concentrarse en cosas tales como leer el periódico o ver televisión',
        valorPuntaje: 2,
        textoFrecuencia: 'Más de la mitad de los días'
      },
      {
        instrumento: 'PHQ9',
        indicePregunta: 8,
        enunciadoPregunta: 'Moverse o hablar tan lentamente que otras personas lo han notado, o lo contrario: estar tan inquieto/a o intranquilo/a que se ha estado moviendo mucho más de lo habitual',
        valorPuntaje: 2,
        textoFrecuencia: 'Más de la mitad de los días'
      },
      {
        instrumento: 'PHQ9',
        indicePregunta: 9,
        enunciadoPregunta: 'Pensamientos de que estaría mejor muerto/a o de hacerse daño de alguna manera',
        valorPuntaje: 1,
        textoFrecuencia: 'Varios días'
      },
      // 7 ítems de GAD-7
      {
        instrumento: 'GAD7',
        indicePregunta: 1,
        enunciadoPregunta: 'Sentirse nervioso/a, intranquilo/a o con los nervios de punta',
        valorPuntaje: 3,
        textoFrecuencia: 'Casi todos los días'
      },
      {
        instrumento: 'GAD7',
        indicePregunta: 2,
        enunciadoPregunta: 'No poder parar o controlar las preocupaciones',
        valorPuntaje: 2,
        textoFrecuencia: 'Más de la mitad de los días'
      },
      {
        instrumento: 'GAD7',
        indicePregunta: 3,
        enunciadoPregunta: 'Preocuparse demasiado por diferentes cosas',
        valorPuntaje: 1,
        textoFrecuencia: 'Varios días'
      },
      {
        instrumento: 'GAD7',
        indicePregunta: 4,
        enunciadoPregunta: 'Dificultad para relajarse',
        valorPuntaje: 2,
        textoFrecuencia: 'Más de la mitad de los días'
      },
      {
        instrumento: 'GAD7',
        indicePregunta: 5,
        enunciadoPregunta: 'Estar tan inquieto/a que le cuesta permanecer quieto/a',
        valorPuntaje: 0,
        textoFrecuencia: 'Ningún día'
      },
      {
        instrumento: 'GAD7',
        indicePregunta: 6,
        enunciadoPregunta: 'Ponerse molesto/a o irritable con facilidad',
        valorPuntaje: 1,
        textoFrecuencia: 'Varios días'
      },
      {
        instrumento: 'GAD7',
        indicePregunta: 7,
        enunciadoPregunta: 'Sentir miedo como si algo terrible fuera a pasar',
        valorPuntaje: 1,
        textoFrecuencia: 'Varios días'
      }
    ]
  };

  const detalleMockParcial = {
    id: 'eval-clin-123',
    evaluacionId: 'eval-clin-123',
    pacienteId: 50,
    nombrePaciente: 'Paciente Prueba Detalle',
    correoPaciente: 'paciente@prueba.com',
    puntajePhq9: 14,
    severidadDepresion: 'MODERADA',
    puntajeGad7: 10,
    severidadAnsiedad: 'MODERADA',
    riesgoCrisis: true,
    fechaFinalizacion: '2026-10-15T12:00:00Z',
    respuestas: [
      { instrumento: 'PHQ-9', indicePregunta: 1, valorPuntaje: 2 },
      { instrumento: 'PHQ-9', indicePregunta: 9, valorPuntaje: 1 },
      { instrumento: 'GAD-7', indicePregunta: 1, valorPuntaje: 3 }
    ]
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('despliega el desglose completo de 16 respuestas con preguntas, respuestas en badge y puntajes', async () => {
    servicioClinicoEvaluaciones.obtenerDetalle.mockResolvedValueOnce(detalleCompleto16Mock);
    servicioCitas.listarCitasClinico.mockResolvedValueOnce([]);

    render(
      <MemoryRouter initialEntries={['/clinico/evaluaciones/eval-clin-123']}>
        <Routes>
          <Route path="/clinico/evaluaciones/:id" element={<DetalleEvaluacionClinico />} />
        </Routes>
      </MemoryRouter>
    );

    // Encabezado y datos del paciente
    expect(await screen.findByText('Paciente Prueba Detalle')).toBeInTheDocument();
    expect(screen.getByText('Desglose de respuestas ítem a ítem')).toBeInTheDocument();

    // Verificación de ítems de PHQ-9
    expect(screen.getByText(/Poco interés o placer en hacer cosas/i)).toBeInTheDocument();
    expect(screen.getByText(/Sentirse desanimado\/a, deprimido\/a o sin esperanza/i)).toBeInTheDocument();
    expect(screen.getByText(/Problemas para dormir/i)).toBeInTheDocument();
    expect(screen.getByText(/Sentirse cansado\/a o tener poca energía/i)).toBeInTheDocument();
    expect(screen.getByText(/Poco apetito o comer en exceso/i)).toBeInTheDocument();
    expect(screen.getByText(/Dificultad para concentrarse/i)).toBeInTheDocument();
    expect(screen.getByText(/Moverse o hablar tan lentamente/i)).toBeInTheDocument();
    expect(screen.getByText(/Pensamientos de que estaría mejor muerto/i)).toBeInTheDocument();

    // Verificación de alerta de riesgo de autolesión en ítem 9
    expect(screen.getByText('Alerta de riesgo de autolesión')).toBeInTheDocument();

    // Verificación de ítems de GAD-7
    expect(screen.getByText(/Sentirse nervioso\/a, intranquilo\/a/i)).toBeInTheDocument();
    expect(screen.getByText(/No poder parar o controlar las preocupaciones/i)).toBeInTheDocument();
    expect(screen.getByText(/Preocuparse demasiado por diferentes cosas/i)).toBeInTheDocument();
    expect(screen.getByText(/Dificultad para relajarse/i)).toBeInTheDocument();
    expect(screen.getByText(/Estar tan inquieto\/a que le cuesta permanecer quieto\/a/i)).toBeInTheDocument();
    expect(screen.getByText(/Ponerse molesto\/a o irritable con facilidad/i)).toBeInTheDocument();
    expect(screen.getByText(/Sentir miedo como si algo terrible fuera a pasar/i)).toBeInTheDocument();

    // Verificación de badges de frecuencia y formato de puntos
    expect(screen.getAllByText('Más de la mitad de los días').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Casi todos los días').length).toBeGreaterThan(0);
    expect(screen.getAllByText('2 pts').length).toBeGreaterThan(0);
    expect(screen.getAllByText('3 pts').length).toBeGreaterThan(0);

    // Botón de proponer cita disponible
    const botonProponer = screen.getByRole('button', { name: /Proponer cita/i });
    expect(botonProponer).toBeEnabled();
  });

  it('aplica catálogo fallback de reactivos y traducción de escala cuando faltan enunciadoPregunta o textoFrecuencia', async () => {
    servicioClinicoEvaluaciones.obtenerDetalle.mockResolvedValueOnce(detalleMockParcial);
    servicioCitas.listarCitasClinico.mockResolvedValueOnce([]);

    render(
      <MemoryRouter initialEntries={['/clinico/evaluaciones/eval-clin-123']}>
        <Routes>
          <Route path="/clinico/evaluaciones/:id" element={<DetalleEvaluacionClinico />} />
        </Routes>
      </MemoryRouter>
    );

    expect(await screen.findByText('Paciente Prueba Detalle')).toBeInTheDocument();

    // Fallback de enunciado en PHQ-9
    expect(screen.getByText(/Poco interés o placer en hacer cosas/i)).toBeInTheDocument();
    expect(screen.getByText(/Más de la mitad de los días/i)).toBeInTheDocument();
    expect(screen.getByText('2 pts')).toBeInTheDocument();

    // Pregunta 9 con fallback
    expect(screen.getByText(/Pensamientos de que estaría mejor muerto/i)).toBeInTheDocument();
    expect(screen.getByText(/Varios días/i)).toBeInTheDocument();
    expect(screen.getByText('1 pts')).toBeInTheDocument();
    expect(screen.getByText('Alerta de riesgo de autolesión')).toBeInTheDocument();

    // Fallback de enunciado en GAD-7
    expect(screen.getByText(/Sentirse nervioso\/a, intranquilo\/a/i)).toBeInTheDocument();
    expect(screen.getByText(/Casi todos los días/i)).toBeInTheDocument();
    expect(screen.getByText('3 pts')).toBeInTheDocument();
  });

  it('muestra estado visual vacío cuando no hay respuestas registradas', async () => {
    servicioClinicoEvaluaciones.obtenerDetalle.mockResolvedValueOnce({
      ...detalleMockParcial,
      respuestas: []
    });
    servicioCitas.listarCitasClinico.mockResolvedValueOnce([]);

    render(
      <MemoryRouter initialEntries={['/clinico/evaluaciones/eval-clin-123']}>
        <Routes>
          <Route path="/clinico/evaluaciones/:id" element={<DetalleEvaluacionClinico />} />
        </Routes>
      </MemoryRouter>
    );

    expect(await screen.findByText('Paciente Prueba Detalle')).toBeInTheDocument();
    expect(
      screen.getByText('No se encontraron respuestas registradas para esta evaluación.')
    ).toBeInTheDocument();
  });

  it('deshabilita el botón de proponer cita y muestra advertencia si ya existe una cita activa con el paciente', async () => {
    servicioClinicoEvaluaciones.obtenerDetalle.mockResolvedValueOnce(detalleCompleto16Mock);
    servicioCitas.listarCitasClinico.mockResolvedValueOnce([
      {
        id: 'cita-activa-99',
        pacienteId: 50,
        estado: 'PROPUESTA',
        fechaHoraInicio: '2026-10-25T14:00:00Z'
      }
    ]);

    render(
      <MemoryRouter initialEntries={['/clinico/evaluaciones/eval-clin-123']}>
        <Routes>
          <Route path="/clinico/evaluaciones/:id" element={<DetalleEvaluacionClinico />} />
        </Routes>
      </MemoryRouter>
    );

    expect(await screen.findByText('Paciente Prueba Detalle')).toBeInTheDocument();

    // Verificamos aviso de cita activa existente
    expect(
      screen.getByText(/El paciente ya cuenta con una cita activa en estado/i)
    ).toBeInTheDocument();

    // El botón debe estar deshabilitado
    const botonProponer = screen.getByRole('button', { name: /Cita ya activa para esta evaluación/i });
    expect(botonProponer).toBeDisabled();
  });
});
