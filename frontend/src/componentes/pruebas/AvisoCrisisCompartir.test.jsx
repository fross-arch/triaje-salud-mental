import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { AvisoCrisis } from '../triaje/AvisoCrisis.jsx';
import { AvisoCrisisCompartir } from '../triaje/AvisoCrisisCompartir.jsx';
import { TEXTO_ADVERTENCIA_CRISIS_COMPARTIR } from '../../constantes/textosLegales.js';

describe('AvisoCrisisCompartir', () => {
  it('valida que junto al aviso de crisis se renderice el texto de advertencia exacto sin ocultar el aviso prioritario', () => {
    render(
      <MemoryRouter>
        <div>
          <AvisoCrisis
            mostrarBotonContinuar={false}
            recursosPersonalizados={[
              { nombre: 'Línea 106', telefono: '106', horario: '24 horas', descripcion: 'Atención en crisis' }
            ]}
          />
          <AvisoCrisisCompartir evaluacionId="eval-crisis-99" />
        </div>
      </MemoryRouter>
    );

    // 1. El aviso de crisis principal permanece visible y prioritario
    const avisoCrisisPrincipal = screen.getByRole('alert', {
      name: /aviso prioritario de apoyo y lineas de ayuda/i
    });
    expect(avisoCrisisPrincipal).toBeInTheDocument();
    expect(screen.getByText(/canales de orientación y apoyo profesional/i)).toBeInTheDocument();
    expect(screen.getByText('Línea 106')).toBeInTheDocument();

    // 2. Se renderiza el texto de advertencia exacto
    const textoExacto =
      'Compartir tu resultado con un psicólogo no activa una alerta ni garantiza que te contacten de inmediato. Si necesitas ayuda ahora, usa las líneas de apoyo.';

    expect(screen.getByText(textoExacto)).toBeInTheDocument();
    expect(TEXTO_ADVERTENCIA_CRISIS_COMPARTIR).toBe(textoExacto);

    // 3. El componente secundario no oculta el aviso de crisis y contiene la opción de compartir
    expect(screen.getByRole('link', { name: /compartir con un psicólogo/i })).toBeInTheDocument();
  });
});
