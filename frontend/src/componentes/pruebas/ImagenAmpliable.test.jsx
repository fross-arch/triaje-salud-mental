import React from 'react';
import { describe, it, expect } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ImagenAmpliable } from '../comunes/ImagenAmpliable.jsx';

describe('Componente ImagenAmpliable', () => {
  const propsBase = {
    src: 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==',
    alt: 'Gráfico estadístico de prueba',
    tituloModal: 'Distribución Estadística'
  };

  it('renderiza la imagen en miniatura con su texto alternativo y botón accesible', () => {
    render(<ImagenAmpliable {...propsBase} />);

    const botonDisparador = screen.getByRole('button', { name: /Ampliar imagen: Gráfico estadístico de prueba/i });
    expect(botonDisparador).toBeInTheDocument();

    const imagen = screen.getByRole('img', { name: 'Gráfico estadístico de prueba' });
    expect(imagen).toBeInTheDocument();
    expect(imagen).toHaveAttribute('src', propsBase.src);
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('abre el modal accesible al hacer clic en la imagen y muestra el título', async () => {
    render(<ImagenAmpliable {...propsBase} />);
    const usuario = userEvent.setup();

    const botonDisparador = screen.getByRole('button', { name: /Ampliar imagen/i });
    await usuario.click(botonDisparador);

    const dialogo = screen.getByRole('dialog');
    expect(dialogo).toBeInTheDocument();
    expect(dialogo).toHaveAttribute('aria-modal', 'true');
    expect(screen.getByText('Distribución Estadística')).toBeInTheDocument();
  });

  it('permite cerrar el modal pulsando el botón de cierre', async () => {
    render(<ImagenAmpliable {...propsBase} />);
    const usuario = userEvent.setup();

    await usuario.click(screen.getByRole('button', { name: /Ampliar imagen/i }));
    expect(screen.getByRole('dialog')).toBeInTheDocument();

    const botonCerrar = screen.getByRole('button', { name: /Cerrar vista ampliada/i });
    await usuario.click(botonCerrar);

    await waitFor(() => {
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });
  });

  it('permite cerrar el modal con la tecla Escape', async () => {
    render(<ImagenAmpliable {...propsBase} />);
    const usuario = userEvent.setup();

    await usuario.click(screen.getByRole('button', { name: /Ampliar imagen/i }));
    expect(screen.getByRole('dialog')).toBeInTheDocument();

    await usuario.keyboard('{Escape}');

    await waitFor(() => {
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });
  });

  it('permite cerrar el modal al hacer clic en el fondo oscuro (backdrop)', async () => {
    render(<ImagenAmpliable {...propsBase} />);
    const usuario = userEvent.setup();

    await usuario.click(screen.getByRole('button', { name: /Ampliar imagen/i }));
    const dialogo = screen.getByRole('dialog');
    expect(dialogo).toBeInTheDocument();

    // Clic en el backdrop (el contenedor del diálogo con el rol)
    await usuario.click(dialogo);

    await waitFor(() => {
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });
  });
});
