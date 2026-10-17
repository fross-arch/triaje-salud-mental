import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';

import { PantallaConsentimiento } from '../pantallas/PantallaConsentimiento.jsx';
import { PantallaIntroduccion } from '../pantallas/PantallaIntroduccion.jsx';
import { AsistenteTriaje } from '../pantallas/AsistenteTriaje.jsx';
import { PanelResultados } from '../pantallas/PanelResultados.jsx';
import { ContextoAutenticacion } from '../contextos/ContextoAutenticacion.jsx';
import {
  TEXTO_RESULTADO_MINIMO,
  TEXTO_NOTA_INFORMATIVA,
  TEXTOS_INFORMACION_CUESTIONARIOS
} from '../constantes/textosLegales.js';
import { servicioConsentimiento } from '../servicios/servicioConsentimiento.js';
import { servicioTriaje } from '../servicios/servicioTriaje.js';
import { servicioRecursosAyuda } from '../servicios/servicioRecursosAyuda.js';

// Mocks de servicios backend para pruebas unitarias e integradas
vi.mock('../servicios/servicioConsentimiento.js', () => ({
  servicioConsentimiento: {
    registrarConsentimiento: vi.fn(),
    consultarVigente: vi.fn(),
    revocar: vi.fn()
  }
}));

vi.mock('../servicios/servicioTriaje.js', () => ({
  servicioTriaje: {
    crearEvaluacion: vi.fn(),
    registrarRespuesta: vi.fn(),
    finalizarEvaluacion: vi.fn(),
    obtenerDetalle: vi.fn(),
    descargarPdf: vi.fn()
  }
}));

vi.mock('../servicios/servicioRecursosAyuda.js', () => ({
  servicioRecursosAyuda: {
    obtenerRecursos: vi.fn().mockResolvedValue([
      {
        nombre: 'Línea 106',
        telefono: '106',
        horario: '24 horas',
        descripcion: 'Atención en crisis y salud mental',
        ambito: 'Medellín'
      }
    ])
  }
}));

describe('VerificacionFronterasClinicas', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const envolverConAutenticacion = (children) => (
    <ContextoAutenticacion.Provider
      value={{
        usuario: { id: 'usr-1', nombreCompleto: 'Usuario Clínico', correo: 'clinico@salud.org' },
        cerrarSesion: vi.fn(),
        estaAutenticado: true
      }}
    >
      {children}
    </ContextoAutenticacion.Provider>
  );

  /**
   * 1. debeMostrarPantallaIntroduccionAntesDePreguntas
   * Verificar que tras el consentimiento se muestre el componente de introducción con el texto
   * explicativo de 'docs/informacion-cuestionarios-v1.md' y el enlace permanente 'Sobre los cuestionarios'.
   */
  it('debeMostrarPantallaIntroduccionAntesDePreguntas', async () => {
    servicioConsentimiento.registrarConsentimiento.mockResolvedValueOnce({ id: 'cons-1' });
    servicioTriaje.crearEvaluacion.mockResolvedValueOnce({ id: 'eval-clinica-456' });

    const usuario = userEvent.setup();

    render(
      envolverConAutenticacion(
        <MemoryRouter initialEntries={['/consentimiento']}>
          <Routes>
            <Route path="/consentimiento" element={<PantallaConsentimiento />} />
            <Route path="/introduccion" element={<PantallaIntroduccion />} />
            <Route path="/asistente/:id" element={<AsistenteTriaje />} />
          </Routes>
        </MemoryRouter>
      )
    );

    // 1. Confirmar que estamos en consentimiento y marcar ambas casillas obligatorias
    const casillaTratamiento = screen.getByLabelText(/Autorizo de forma previa, expresa e informada/i);
    const casillaMayoriaEdad = screen.getByLabelText(/Declaro bajo gravedad de juramento/i);
    const botonContinuarConsentimiento = screen.getByRole('button', { name: /Aceptar y continuar/i });

    await usuario.click(casillaTratamiento);
    await usuario.click(casillaMayoriaEdad);
    await usuario.click(botonContinuarConsentimiento);

    expect(servicioConsentimiento.registrarConsentimiento).toHaveBeenCalledWith(true);

    // 2. Verificar que se despliega PantallaIntroduccion con el texto explicativo de docs/informacion-cuestionarios-v1.md
    expect(
      await screen.findByRole('heading', { level: 1, name: TEXTOS_INFORMACION_CUESTIONARIOS.titulo })
    ).toBeInTheDocument();
    expect(screen.getByText(TEXTOS_INFORMACION_CUESTIONARIOS.introduccion)).toBeInTheDocument();
    expect(screen.getByText('PHQ-9')).toBeInTheDocument();
    expect(screen.getByText('GAD-7')).toBeInTheDocument();
    expect(screen.getByText(TEXTOS_INFORMACION_CUESTIONARIOS.periodo)).toBeInTheDocument();

    // Comprobar que en la introducción aún NO se presentan las preguntas de los cuestionarios
    expect(screen.queryByRole('heading', { level: 1, name: /Poco interés o placer en hacer cosas/i })).not.toBeInTheDocument();

    // 3. Iniciar el asistente desde la pantalla de introducción
    const botonIniciarAsistente = screen.getByRole('button', { name: /Iniciar asistente de cribado/i });
    await usuario.click(botonIniciarAsistente);

    expect(servicioTriaje.crearEvaluacion).toHaveBeenCalledTimes(1);

    // 4. En la pantalla del asistente, verificar la primera pregunta y la presencia del enlace/botón permanente 'Sobre los cuestionarios'
    expect(
      await screen.findByRole('heading', { level: 1, name: /Poco interés o placer en hacer cosas/i })
    ).toBeInTheDocument();

    const botonSobreCuestionarios = screen.getByRole('button', { name: /Sobre los cuestionarios/i });
    expect(botonSobreCuestionarios).toBeInTheDocument();
    expect(botonSobreCuestionarios).toHaveAttribute('aria-label', 'Informacion sobre los cuestionarios');

    // Comprobar que el enlace permanente abre efectivamente la información explicativa de los instrumentos
    await usuario.click(botonSobreCuestionarios);
    expect(
      screen.getByRole('dialog', { name: TEXTOS_INFORMACION_CUESTIONARIOS.titulo })
    ).toBeInTheDocument();
  });

  /**
   * 2. debeMostrarMensajeResultadoMinimoSiCorresponde
   * Simular respuesta del backend con severidadDepresion='MINIMA', severidadAnsiedad='MINIMA',
   * riesgoCrisis=false y resultadoMinimo=true. Comprobar que 'PanelResultados' renderice el texto exacto:
   * "Tus respuestas no sugieren síntomas significativos de depresión ni de ansiedad en las últimas dos semanas...".
   */
  it('debeMostrarMensajeResultadoMinimoSiCorresponde', () => {
    const respuestaBackendMinima = {
      id: 'eval-res-minimo',
      severidadDepresion: 'MINIMA',
      severidadAnsiedad: 'MINIMA',
      riesgoCrisis: false,
      resultadoMinimo: true,
      puntajePhq9: 2,
      puntajeGad7: 1,
      fechaCreacion: '2026-10-14T10:00:00Z'
    };

    render(
      <MemoryRouter>
        <PanelResultados datosEvaluacion={respuestaBackendMinima} />
      </MemoryRouter>
    );

    // Comprobar texto exacto requerido
    expect(screen.getByText(TEXTO_RESULTADO_MINIMO)).toBeInTheDocument();
    expect(
      screen.getByText(/Tus respuestas no sugieren síntomas significativos de depresión ni de ansiedad en las últimas dos semanas/i)
    ).toBeInTheDocument();

    // Accesibilidad: validar contenedor con rol semántico region
    expect(
      screen.getByRole('region', { name: /mensaje de resultado minimo/i })
    ).toBeInTheDocument();

    // No debe figurar ningún aviso de crisis
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  /**
   * 3. debeProhibirMensajeResultadoMinimoSiHayRiesgoCrisis
   * Simular respuesta con puntajes bajos pero con riesgoCrisis=true. Comprobar que aparezca 'AvisoCrisis'
   * y que 'MensajeResultadoMinimo' NO exista en el DOM bajo ninguna circunstancia.
   */
  it('debeProhibirMensajeResultadoMinimoSiHayRiesgoCrisis', async () => {
    const respuestaBackendCrisisPuntajesBajos = {
      id: 'eval-res-crisis-puntajes-bajos',
      severidadDepresion: 'MINIMA',
      severidadAnsiedad: 'MINIMA',
      riesgoCrisis: true,
      resultadoMinimo: true, // Forzamos true para comprobar que riesgoCrisis bloquea estrictamente el mensaje
      puntajePhq9: 2,
      puntajeGad7: 1,
      fechaCreacion: '2026-10-14T10:00:00Z'
    };

    render(
      <MemoryRouter>
        <PanelResultados datosEvaluacion={respuestaBackendCrisisPuntajesBajos} />
      </MemoryRouter>
    );

    // Debe renderizarse el componente AvisoCrisis de manera prioritaria con rol alert
    expect(
      await screen.findByRole('alert', { name: /Aviso prioritario de apoyo y lineas de ayuda/i })
    ).toBeInTheDocument();
    expect(await screen.findByText(/Línea 106/i)).toBeInTheDocument();

    // REGLA CLÍNICA CRÍTICA: MensajeResultadoMinimo NO debe existir en el DOM bajo ninguna circunstancia
    expect(screen.queryByText(TEXTO_RESULTADO_MINIMO)).not.toBeInTheDocument();
    expect(
      screen.queryByText(/Tus respuestas no sugieren síntomas significativos de depresión ni de ansiedad/i)
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole('region', { name: /mensaje de resultado minimo/i })
    ).not.toBeInTheDocument();
  });

  /**
   * 4. debeMostrarNotaInformativaEnTodosLosResultados
   * Comprobar que el texto "Este resultado es orientativo. No mide todas las situaciones de salud mental..."
   * esté visible en todos los estados de resultado posibles.
   */
  it('debeMostrarNotaInformativaEnTodosLosResultados', () => {
    const estadosPosibles = [
      {
        nombre: 'Severidad mínima sin crisis',
        evaluacion: {
          id: 'eval-est-minimo',
          severidadDepresion: 'MINIMA',
          severidadAnsiedad: 'MINIMA',
          riesgoCrisis: false,
          resultadoMinimo: true,
          puntajePhq9: 2,
          puntajeGad7: 1
        }
      },
      {
        nombre: 'Severidad leve',
        evaluacion: {
          id: 'eval-est-leve',
          severidadDepresion: 'LEVE',
          severidadAnsiedad: 'LEVE',
          riesgoCrisis: false,
          resultadoMinimo: false,
          puntajePhq9: 7,
          puntajeGad7: 6
        }
      },
      {
        nombre: 'Severidad moderada',
        evaluacion: {
          id: 'eval-est-moderada',
          severidadDepresion: 'MODERADA',
          severidadAnsiedad: 'MODERADA',
          riesgoCrisis: false,
          resultadoMinimo: false,
          puntajePhq9: 12,
          puntajeGad7: 11
        }
      },
      {
        nombre: 'Severidad moderadamente severa',
        evaluacion: {
          id: 'eval-est-mod-sev',
          severidadDepresion: 'MODERADAMENTE_SEVERA',
          severidadAnsiedad: 'MODERADA',
          riesgoCrisis: false,
          resultadoMinimo: false,
          puntajePhq9: 16,
          puntajeGad7: 12
        }
      },
      {
        nombre: 'Severidad severa con riesgo de crisis',
        evaluacion: {
          id: 'eval-est-severa-crisis',
          severidadDepresion: 'SEVERA',
          severidadAnsiedad: 'SEVERA',
          riesgoCrisis: true,
          resultadoMinimo: false,
          puntajePhq9: 23,
          puntajeGad7: 18
        }
      },
      {
        nombre: 'Severidad mínima con riesgo de crisis activado (ítem 9)',
        evaluacion: {
          id: 'eval-est-minima-crisis',
          severidadDepresion: 'MINIMA',
          severidadAnsiedad: 'MINIMA',
          riesgoCrisis: true,
          resultadoMinimo: false,
          puntajePhq9: 3,
          puntajeGad7: 2
        }
      },
      {
        nombre: 'Severidad disociada (depresión moderada y ansiedad mínima)',
        evaluacion: {
          id: 'eval-est-disociada',
          severidadDepresion: 'MODERADA',
          severidadAnsiedad: 'MINIMA',
          riesgoCrisis: false,
          resultadoMinimo: false,
          puntajePhq9: 13,
          puntajeGad7: 2
        }
      }
    ];

    for (const estado of estadosPosibles) {
      const { unmount } = render(
        <MemoryRouter>
          <PanelResultados datosEvaluacion={estado.evaluacion} />
        </MemoryRouter>
      );

      // Texto exacto obligatorio de la NotaInformativa
      expect(screen.getByText(TEXTO_NOTA_INFORMATIVA)).toBeInTheDocument();

      // Validación con regex del inicio del enunciado requerido
      expect(
        screen.getByText(/Este resultado es orientativo\. No mide todas las situaciones de salud mental/i)
      ).toBeInTheDocument();

      // Accesibilidad: rol note
      expect(
        screen.getByRole('note', { name: /Nota informativa sobre el alcance del cribado/i })
      ).toBeInTheDocument();

      unmount();
    }
  });

  /**
   * 5. debeBloquearPasoSinAmbasCasillasEnConsentimiento
   * Comprobar que en 'PantallaConsentimiento' el botón para continuar permanezca deshabilitado
   * si falta marcar la confirmación de mayoría de edad (>= 18 años) o la autorización de tratamiento de datos.
   */
  it('debeBloquearPasoSinAmbasCasillasEnConsentimiento', async () => {
    const usuario = userEvent.setup();

    render(
      envolverConAutenticacion(
        <MemoryRouter>
          <PantallaConsentimiento />
        </MemoryRouter>
      )
    );

    const botonContinuar = screen.getByRole('button', { name: /Aceptar y continuar/i });
    const casillaTratamiento = screen.getByLabelText(/Autorizo de forma previa, expresa e informada/i);
    const casillaMayoriaEdad = screen.getByLabelText(/Declaro bajo gravedad de juramento/i);

    // 1. Estado inicial: ambas casillas desmarcadas -> botón deshabilitado
    expect(casillaTratamiento).not.toBeChecked();
    expect(casillaMayoriaEdad).not.toBeChecked();
    expect(botonContinuar).toBeDisabled();

    // 2. Marcar únicamente autorización de tratamiento de datos -> sigue deshabilitado
    await usuario.click(casillaTratamiento);
    expect(casillaTratamiento).toBeChecked();
    expect(casillaMayoriaEdad).not.toBeChecked();
    expect(botonContinuar).toBeDisabled();

    // 3. Desmarcar tratamiento y marcar únicamente confirmación de mayoría de edad -> sigue deshabilitado
    await usuario.click(casillaTratamiento);
    await usuario.click(casillaMayoriaEdad);
    expect(casillaTratamiento).not.toBeChecked();
    expect(casillaMayoriaEdad).toBeChecked();
    expect(botonContinuar).toBeDisabled();

    // 4. Marcar ambas casillas obligatorias -> el botón se habilita
    await usuario.click(casillaTratamiento);
    expect(casillaTratamiento).toBeChecked();
    expect(casillaMayoriaEdad).toBeChecked();
    expect(botonContinuar).toBeEnabled();

    // 5. Desmarcar nuevamente una de las casillas (mayoría de edad) -> vuelve a bloquearse
    await usuario.click(casillaMayoriaEdad);
    expect(casillaTratamiento).toBeChecked();
    expect(casillaMayoriaEdad).not.toBeChecked();
    expect(botonContinuar).toBeDisabled();
  });
});
