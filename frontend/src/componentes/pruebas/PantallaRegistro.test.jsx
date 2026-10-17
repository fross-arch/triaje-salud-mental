import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { PantallaRegistro } from '../../pantallas/PantallaRegistro.jsx';
import * as useAutenticacionModulo from '../../hooks/useAutenticacion.js';

describe('PantallaRegistro', () => {
  const mockRegistrar = vi.fn();
  const mockNavegar = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    vi.spyOn(useAutenticacionModulo, 'useAutenticacion').mockReturnValue({
      registrar: mockRegistrar,
      estaAutenticado: false,
      usuario: null
    });
  });

  it('muestra la etiqueta y el texto de ayuda explícito exigiendo al menos 10 caracteres', () => {
    render(
      <MemoryRouter>
        <PantallaRegistro />
      </MemoryRouter>
    );

    expect(screen.getByLabelText(/Contraseña \(mínimo 10 caracteres\)/i)).toBeInTheDocument();
    expect(screen.getByText('La contraseña debe tener al menos 10 caracteres')).toBeInTheDocument();
  });

  it('bloquea el registro y muestra error si la contraseña tiene menos de 10 caracteres (ej. 8 o 9)', async () => {
    const usuario = userEvent.setup();

    render(
      <MemoryRouter>
        <PantallaRegistro />
      </MemoryRouter>
    );

    await usuario.type(screen.getByLabelText(/Nombre completo/i), 'Paciente Juan');
    await usuario.type(screen.getByLabelText(/Correo electrónico/i), 'juan@ejemplo.com');
    await usuario.type(screen.getByLabelText(/Contraseña \(mínimo 10 caracteres\)/i), '12345678'); // 8 caracteres

    await usuario.click(screen.getByRole('button', { name: /Crear cuenta/i }));

    expect(screen.getByRole('alert')).toHaveTextContent('La contraseña debe tener al menos 10 caracteres.');
    expect(mockRegistrar).not.toHaveBeenCalled();
  });

  it('permite registrar la cuenta cuando la contraseña tiene 10 o más caracteres', async () => {
    const usuario = userEvent.setup();
    mockRegistrar.mockResolvedValueOnce({ id: 1, correo: 'juan@ejemplo.com' });

    render(
      <MemoryRouter initialEntries={['/registro']}>
        <Routes>
          <Route path="/registro" element={<PantallaRegistro />} />
          <Route path="/consentimiento" element={<div>Pantalla Consentimiento Destino</div>} />
        </Routes>
      </MemoryRouter>
    );

    await usuario.type(screen.getByLabelText(/Nombre completo/i), 'Paciente Juan');
    await usuario.type(screen.getByLabelText(/Correo electrónico/i), 'juan@ejemplo.com');
    await usuario.type(screen.getByLabelText(/Contraseña \(mínimo 10 caracteres\)/i), 'ClaveSegura10*'); // 14 caracteres

    await usuario.click(screen.getByRole('button', { name: /Crear cuenta/i }));

    expect(mockRegistrar).toHaveBeenCalledWith('Paciente Juan', 'juan@ejemplo.com', 'ClaveSegura10*');
    expect(await screen.findByText('Pantalla Consentimiento Destino')).toBeInTheDocument();
  });
});
