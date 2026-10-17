import React from 'react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { PresentadorAvatar } from '../avatar/PresentadorAvatar.jsx';

describe('PresentadorAvatar', () => {
  beforeEach(() => {
    window.sessionStorage.clear();
    vi.restoreAllMocks();
  });

  afterEach(() => {
    window.sessionStorage.clear();
  });

  it('renderiza correctamente el estado BIENVENIDA con atributos ARIA accesibles', () => {
    render(<PresentadorAvatar estado="BIENVENIDA" />);

    const svgElemento = screen.getByTestId('svg-presentador-avatar');
    expect(svgElemento).toBeInTheDocument();
    expect(svgElemento).toHaveAttribute('aria-hidden', 'true');
    expect(svgElemento).toHaveAttribute('data-estado-visual', 'BIENVENIDA');
    expect(screen.getByTestId('variante-bienvenida')).toBeInTheDocument();
  });

  it('renderiza correctamente el estado PREGUNTANDO por defecto', () => {
    render(<PresentadorAvatar />);

    const svgElemento = screen.getByTestId('svg-presentador-avatar');
    expect(svgElemento).toBeInTheDocument();
    expect(svgElemento).toHaveAttribute('aria-hidden', 'true');
    expect(svgElemento).toHaveAttribute('data-estado-visual', 'PREGUNTANDO');
    expect(screen.getByTestId('variante-preguntando')).toBeInTheDocument();
  });

  it('renderiza correctamente el estado CRISIS de forma sobria y serena', () => {
    render(<PresentadorAvatar estado="CRISIS" />);

    const svgElemento = screen.getByTestId('svg-presentador-avatar');
    expect(svgElemento).toBeInTheDocument();
    expect(svgElemento).toHaveAttribute('aria-hidden', 'true');
    expect(svgElemento).toHaveAttribute('data-estado-visual', 'CRISIS');
    expect(screen.getByTestId('variante-crisis')).toBeInTheDocument();
  });

  it('renderiza correctamente el estado FINALIZADO con expresion reflexiva', () => {
    render(<PresentadorAvatar estado="FINALIZADO" />);

    const svgElemento = screen.getByTestId('svg-presentador-avatar');
    expect(svgElemento).toBeInTheDocument();
    expect(svgElemento).toHaveAttribute('aria-hidden', 'true');
    expect(svgElemento).toHaveAttribute('data-estado-visual', 'FINALIZADO');
    expect(screen.getByTestId('variante-finalizado')).toBeInTheDocument();
  });

  it('fuerza inmediatamente el estado a CRISIS cuando riesgoCrisis es true ignorando otra prop de estado', () => {
    render(<PresentadorAvatar estado="BIENVENIDA" riesgoCrisis={true} />);

    const svgElemento = screen.getByTestId('svg-presentador-avatar');
    expect(svgElemento).toHaveAttribute('data-estado-visual', 'CRISIS');
    expect(screen.getByTestId('variante-crisis')).toBeInTheDocument();
    expect(screen.queryByTestId('variante-bienvenida')).not.toBeInTheDocument();
    // En crisis se desactivan estrictamente las microanimaciones
    expect(svgElemento).toHaveAttribute('data-animaciones-activas', 'false');
  });

  it('desactiva las microanimaciones cuando el estado es CRISIS', () => {
    render(<PresentadorAvatar estado="CRISIS" />);

    const svgElemento = screen.getByTestId('svg-presentador-avatar');
    expect(svgElemento).toHaveAttribute('data-animaciones-activas', 'false');
  });

  it('alterna la visibilidad con el boton accesible cambiando el atributo aria-pressed y ocultando el SVG', async () => {
    render(<PresentadorAvatar estado="PREGUNTANDO" />);

    const usuarioInteractivo = userEvent.setup();
    const botonAlternar = screen.getByTestId('boton-alternar-avatar');

    // Inicialmente esta visible
    expect(botonAlternar).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByTestId('svg-presentador-avatar')).toBeInTheDocument();
    expect(botonAlternar).toHaveTextContent(/Ocultar guía/i);

    // Ocultar avatar
    await usuarioInteractivo.click(botonAlternar);

    expect(botonAlternar).toHaveAttribute('aria-pressed', 'false');
    expect(screen.queryByTestId('svg-presentador-avatar')).not.toBeInTheDocument();
    expect(botonAlternar).toHaveTextContent(/Mostrar guía/i);

    // Volver a mostrar avatar
    await usuarioInteractivo.click(botonAlternar);

    expect(botonAlternar).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByTestId('svg-presentador-avatar')).toBeInTheDocument();
    expect(botonAlternar).toHaveTextContent(/Ocultar guía/i);
  });

  it('recuerda la preferencia de visibilidad en sessionStorage', async () => {
    const { unmount } = render(<PresentadorAvatar estado="PREGUNTANDO" />);

    const usuarioInteractivo = userEvent.setup();
    const botonAlternar = screen.getByTestId('boton-alternar-avatar');

    // El usuario oculta el avatar
    await usuarioInteractivo.click(botonAlternar);
    expect(window.sessionStorage.getItem('triaje_avatar_visible')).toBe('false');

    unmount();

    // Al volver a montar, debe iniciar oculto segun sessionStorage
    render(<PresentadorAvatar estado="PREGUNTANDO" />);

    const botonReabierto = screen.getByTestId('boton-alternar-avatar');
    expect(botonReabierto).toHaveAttribute('aria-pressed', 'false');
    expect(screen.queryByTestId('svg-presentador-avatar')).not.toBeInTheDocument();
  });

  it('respeta prefers-reduced-motion desactivando las animaciones si la preferencia del SO esta activa', () => {
    // Simulamos la media query con prefers-reduced-motion activo
    window.matchMedia = vi.fn().mockImplementation((consulta) => ({
      matches: consulta.includes('prefers-reduced-motion: reduce'),
      media: consulta,
      onchange: null,
      addListener: vi.fn(),
      removeListener: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn()
    }));

    render(<PresentadorAvatar estado="PREGUNTANDO" />);

    const svgElemento = screen.getByTestId('svg-presentador-avatar');
    expect(svgElemento).toHaveAttribute('data-animaciones-activas', 'false');
  });
});
