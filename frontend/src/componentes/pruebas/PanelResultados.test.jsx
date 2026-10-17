import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { PanelResultados } from '../../pantallas/PanelResultados.jsx';
import {
  TEXTO_RESULTADO_MINIMO,
  TEXTO_NOTA_INFORMATIVA,
  TEXTO_CUESTIONARIO_INDIVIDUAL_MINIMO
} from '../../constantes/textosLegales.js';
import { servicioRecursosAyuda } from '../../servicios/servicioRecursosAyuda.js';

vi.mock('../../servicios/servicioTriaje.js', () => ({
  servicioTriaje: {
    obtenerDetalle: vi.fn(),
    descargarPdf: vi.fn()
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

describe('PanelResultados', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const renderizarPanel = (datosEvaluacion) => {
    return render(
      <MemoryRouter>
        <PanelResultados datosEvaluacion={datosEvaluacion} />
      </MemoryRouter>
    );
  };

  it('muestra la NotaInformativa obligatoria en todos los casos de evaluacion', () => {
    const evaluacionModerada = {
      id: 'eval-1',
      puntajePhq9: 12,
      puntajeGad7: 10,
      severidadDepresion: 'MODERADA',
      severidadAnsiedad: 'MODERADA',
      riesgoCrisis: false,
      resultadoMinimo: false,
      fechaCreacion: '2026-10-13T10:00:00Z'
    };

    renderizarPanel(evaluacionModerada);

    // Nota informativa obligatoria
    expect(screen.getByText(TEXTO_NOTA_INFORMATIVA)).toBeInTheDocument();
    // No debe mostrar mensaje de resultado minimo
    expect(screen.queryByText(TEXTO_RESULTADO_MINIMO)).not.toBeInTheDocument();
  });

  it('muestra MensajeResultadoMinimo con texto exacto cuando resultadoMinimo es true y riesgoCrisis es false', () => {
    const evaluacionMinimaSinCrisis = {
      id: 'eval-2',
      puntajePhq9: 2,
      puntajeGad7: 1,
      severidadDepresion: 'MINIMA',
      severidadAnsiedad: 'MINIMA',
      riesgoCrisis: false,
      resultadoMinimo: true,
      fechaCreacion: '2026-10-13T10:00:00Z'
    };

    renderizarPanel(evaluacionMinimaSinCrisis);

    // Mensaje de resultado minimo debe estar presente
    expect(screen.getByText(TEXTO_RESULTADO_MINIMO)).toBeInTheDocument();
    // Nota informativa debe estar presente
    expect(screen.getByText(TEXTO_NOTA_INFORMATIVA)).toBeInTheDocument();
    // Aviso de crisis no debe figurar
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('excluye estrictamente MensajeResultadoMinimo si riesgoCrisis es true, desplegando AvisoCrisis y NotaInformativa', async () => {
    const evaluacionConCrisis = {
      id: 'eval-3',
      puntajePhq9: 3,
      puntajeGad7: 2,
      severidadDepresion: 'MINIMA',
      severidadAnsiedad: 'MINIMA',
      riesgoCrisis: true,
      resultadoMinimo: false,
      fechaCreacion: '2026-10-13T10:00:00Z'
    };

    renderizarPanel(evaluacionConCrisis);

    // REGLA CRITICA: Mensaje de resultado minimo NUNCA debe aparecer si riesgoCrisis es true
    expect(screen.queryByText(TEXTO_RESULTADO_MINIMO)).not.toBeInTheDocument();

    // AvisoCrisis debe montarse
    expect(await screen.findByRole('alert')).toBeInTheDocument();
    expect(await screen.findByText(/Línea 106/i)).toBeInTheDocument();

    // NotaInformativa debe seguir presente
    expect(screen.getByText(TEXTO_NOTA_INFORMATIVA)).toBeInTheDocument();
  });

  it('muestra leyenda individual cuando solo uno de los dos instrumentos tiene severidad MINIMA', () => {
    const evaluacionParcial = {
      id: 'eval-4',
      puntajePhq9: 14,
      puntajeGad7: 3,
      severidadDepresion: 'MODERADA',
      severidadAnsiedad: 'MINIMA',
      riesgoCrisis: false,
      resultadoMinimo: false,
      fechaCreacion: '2026-10-13T10:00:00Z'
    };

    renderizarPanel(evaluacionParcial);

    // Leyenda en la tarjeta de ansiedad
    expect(screen.getByText(TEXTO_CUESTIONARIO_INDIVIDUAL_MINIMO)).toBeInTheDocument();

    // Pero el resultado global no es minimo
    expect(screen.queryByText(TEXTO_RESULTADO_MINIMO)).not.toBeInTheDocument();

    // Nota informativa siempre presente
    expect(screen.getByText(TEXTO_NOTA_INFORMATIVA)).toBeInTheDocument();
  });

  it('prohíbe estrictamente MensajeResultadoMinimo si riesgoCrisis es true incluso si resultadoMinimo viene en true', async () => {
    const evaluacionAnomalaConCrisis = {
      id: 'eval-5',
      puntajePhq9: 1,
      puntajeGad7: 1,
      severidadDepresion: 'MINIMA',
      severidadAnsiedad: 'MINIMA',
      riesgoCrisis: true,
      resultadoMinimo: true, // Caso borde donde backend pudiera marcar true pero hay crisis
      fechaCreacion: '2026-10-13T10:00:00Z'
    };

    renderizarPanel(evaluacionAnomalaConCrisis);

    // Debe respetarse la regla clínica crítica del Plan Maestro v4
    expect(screen.queryByText(TEXTO_RESULTADO_MINIMO)).not.toBeInTheDocument();
    expect(await screen.findByRole('alert')).toBeInTheDocument();
    expect(screen.getByText(TEXTO_NOTA_INFORMATIVA)).toBeInTheDocument();
  });

  it('mapea correctamente las propiedades camelCase devueltas por Spring Boot desde obtenerDetalle', async () => {
    const { servicioTriaje } = await import('../../servicios/servicioTriaje.js');
    servicioTriaje.obtenerDetalle.mockResolvedValueOnce({
      evaluacion: {
        id: 'eval-spring-boot',
        puntajePhq9: 4,
        puntajeGad7: 2,
        severidadDepresion: 'MINIMA',
        severidadAnsiedad: 'MINIMA',
        riesgoCrisis: false,
        resultadoMinimo: true,
        fechaCreacion: '2026-10-13T12:00:00Z'
      },
      respuestas: []
    });

    const { Routes, Route } = await import('react-router-dom');

    render(
      <MemoryRouter initialEntries={['/resultados/eval-spring-boot']}>
        <Routes>
          <Route path="/resultados/:id" element={<PanelResultados />} />
        </Routes>
      </MemoryRouter>
    );

    // Esperar a que se resuelva la carga asíncrona
    expect(await screen.findByText(TEXTO_RESULTADO_MINIMO)).toBeInTheDocument();
    expect(screen.getByText(TEXTO_NOTA_INFORMATIVA)).toBeInTheDocument();
    expect(screen.getByText('4')).toBeInTheDocument();
    expect(screen.getByText('2')).toBeInTheDocument();
  });

  it('muestra notificación amigable cuando la descarga del PDF falla con HTTP 503', async () => {
    const { userEvent } = await import('@testing-library/user-event');
    const { servicioTriaje } = await import('../../servicios/servicioTriaje.js');

    const error503 = new Error('Error HTTP 503');
    error503.estado = 503;
    servicioTriaje.descargarPdf.mockRejectedValueOnce(error503);

    const evaluacionNormal = {
      id: 'eval-descarga-503',
      puntajePhq9: 8,
      puntajeGad7: 7,
      severidadDepresion: 'LEVE',
      severidadAnsiedad: 'LEVE',
      riesgoCrisis: false,
      resultadoMinimo: false,
      fechaCreacion: '2026-10-13T10:00:00Z'
    };

    renderizarPanel(evaluacionNormal);

    const usuario = userEvent.setup();
    const botonDescargar = screen.getByRole('button', { name: /Descargar reporte de resultados en formato PDF/i });

    await usuario.click(botonDescargar);

    // Debe mostrar la alerta amigable con el texto exacto requerido
    const alerta503 = await screen.findByRole('alert');
    expect(alerta503).toHaveTextContent(
      'El servicio de generación de reportes no se encuentra disponible temporalmente. Por favor, intenta de nuevo más tarde.'
    );

    // El usuario puede cerrar la notificación
    const botonCerrarAlerta = screen.getByRole('button', { name: /Cerrar notificación de error de descarga/i });
    await usuario.click(botonCerrarAlerta);

    expect(screen.queryByText(/El servicio de generación de reportes no se encuentra disponible temporalmente/i)).not.toBeInTheDocument();
  });
});
