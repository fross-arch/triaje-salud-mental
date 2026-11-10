import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { AsistenteTriaje } from '../../pantallas/AsistenteTriaje.jsx';
import { servicioTriaje } from '../../servicios/servicioTriaje.js';
import { servicioRecursosAyuda } from '../../servicios/servicioRecursosAyuda.js';

vi.mock('../../servicios/servicioTriaje.js', () => ({
  servicioTriaje: {
    registrarRespuesta: vi.fn(),
    finalizarEvaluacion: vi.fn(),
    obtenerDetalle: vi.fn().mockResolvedValue({
      evaluacion: { id: 'eval-123', estado: 'EN_CURSO', riesgoCrisis: false },
      respuestas: []
    })
  }
}));

vi.mock('../../servicios/servicioRecursosAyuda.js', () => ({
  servicioRecursosAyuda: {
    obtenerRecursos: vi.fn().mockResolvedValue([
      {
        nombre: 'Línea 106',
        telefono: '106',
        horario: '24 horas',
        descripcion: 'Atención en crisis',
        ambito: 'Medellín'
      }
    ])
  }
}));

const mockNavegacion = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavegacion,
    useParams: () => ({ id: 'eval-123' })
  };
});

describe('AsistenteTriaje', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const renderizarAsistente = () => {
    return render(
      <MemoryRouter initialEntries={['/asistente/eval-123']}>
        <Routes>
          <Route path="/asistente/:id" element={<AsistenteTriaje />} />
        </Routes>
      </MemoryRouter>
    );
  };

  it('muestra el enunciado temporal obligatorio, la primera pregunta y la barra de progreso accesible', () => {
    renderizarAsistente();

    // Enunciado temporal superior obligatorio
    expect(
      screen.getByText(/Durante las últimas 2 semanas, ¿con qué frecuencia le han molestado los siguientes problemas\?/i)
    ).toBeInTheDocument();

    // Primer reactivo de PHQ-9
    expect(
      screen.getByRole('heading', { level: 1, name: /Poco interés o placer en hacer cosas/i })
    ).toBeInTheDocument();

    // Barra de progreso con aria-valuenow = 1
    const barraProgreso = screen.getByRole('progressbar');
    expect(barraProgreso).toHaveAttribute('aria-valuenow', '1');
    expect(barraProgreso).toHaveAttribute('aria-valuemin', '1');
    expect(barraProgreso).toHaveAttribute('aria-valuemax', '16');
  });

  it('avanza de a una pregunta a la vez y guarda inmediatamente la respuesta en el backend', async () => {
    servicioTriaje.registrarRespuesta.mockResolvedValueOnce({
      evaluacionId: 'eval-123',
      instrumento: 'PHQ9',
      indicePregunta: 1,
      riesgoCrisis: false
    });

    renderizarAsistente();

    const usuarioInteractivo = userEvent.setup();

    // Seleccionar opcion "Varios días" (valor 1)
    const botonOpcionVariosDias = screen.getByRole('button', { name: /Varios días/i });
    await usuarioInteractivo.click(botonOpcionVariosDias);

    // Verificamos que se envio a PUT /api/triaje/evaluaciones/{id}/respuestas con los datos correctos
    expect(servicioTriaje.registrarRespuesta).toHaveBeenCalledTimes(1);
    expect(servicioTriaje.registrarRespuesta).toHaveBeenCalledWith('eval-123', 'PHQ9', 1, 1);

    // Ahora se muestra la segunda pregunta
    expect(
      await screen.findByRole('heading', { level: 1, name: /Sentirse desanimado\/a, deprimido\/a o sin esperanza/i })
    ).toBeInTheDocument();

    // La barra de progreso avanza a 2
    const barraProgreso = screen.getByRole('progressbar');
    expect(barraProgreso).toHaveAttribute('aria-valuenow', '2');
  });

  it('permite abrir el modal de informacion sobre los cuestionarios sin perder el avance', async () => {
    renderizarAsistente();

    const usuarioInteractivo = userEvent.setup();
    const botonSobreCuestionarios = screen.getByRole('button', { name: /Sobre los cuestionarios/i });

    await usuarioInteractivo.click(botonSobreCuestionarios);

    // Modal se despliega
    expect(
      screen.getByRole('dialog', { name: /Sobre los cuestionarios que vas a responder/i })
    ).toBeInTheDocument();

    // Cerrar modal
    const botonCerrarModal = screen.getByRole('button', { name: /Entendido, volver a la evaluacion/i });
    await usuarioInteractivo.click(botonCerrarModal);

    // La pregunta inicial sigue presente
    expect(
      screen.getByRole('heading', { level: 1, name: /Poco interés o placer en hacer cosas/i })
    ).toBeInTheDocument();
  });

  it('muestra el avatar acompañando el reactivo en estado PREGUNTANDO y pasa a CRISIS ante riesgo de crisis', async () => {
    servicioTriaje.registrarRespuesta.mockResolvedValueOnce({
      evaluacionId: 'eval-123',
      instrumento: 'PHQ9',
      indicePregunta: 1,
      riesgoCrisis: true
    });

    renderizarAsistente();

    // El avatar esta presente acompañando la pregunta inicial
    const svgAvatar = screen.getByTestId('svg-presentador-avatar');
    expect(svgAvatar).toBeInTheDocument();
    expect(svgAvatar).toHaveAttribute('data-estado-visual', 'PREGUNTANDO');

    const usuarioInteractivo = userEvent.setup();
    const botonOpcion = screen.getByRole('button', { name: /Varios días/i });
    await usuarioInteractivo.click(botonOpcion);

    // AvisoCrisis aparece de forma prioritaria con su rol de alerta
    expect(
      await screen.findByRole('alert', { name: /Aviso prioritario de apoyo y lineas de ayuda/i })
    ).toBeInTheDocument();

    // El avatar pasa de inmediato al estado sobrio 'CRISIS'
    const svgAvatarCrisis = screen.getByTestId('svg-presentador-avatar');
    expect(svgAvatarCrisis).toHaveAttribute('data-estado-visual', 'CRISIS');
    expect(svgAvatarCrisis).toHaveAttribute('data-animaciones-activas', 'false');
  });

  it('reanuda una evaluación en curso cargando sus respuestas y posicionándose en la primera pregunta pendiente', async () => {
    servicioTriaje.obtenerDetalle.mockResolvedValueOnce({
      evaluacion: { id: 'eval-123', estado: 'EN_CURSO', riesgoCrisis: false },
      respuestas: [
        { instrumento: 'PHQ9', indicePregunta: 1, valorPuntaje: 2 },
        { instrumento: 'PHQ9', indicePregunta: 2, valorPuntaje: 1 }
      ]
    });

    renderizarAsistente();

    // Debe posicionarse en la pregunta 3 ("Problemas para dormir o para mantenerse dormido/a, o dormir demasiado")
    expect(await screen.findByText(/Pregunta 3 de 16/i)).toBeInTheDocument();
    expect(screen.getByText(/Problemas para dormir/i)).toBeInTheDocument();
  });
});
