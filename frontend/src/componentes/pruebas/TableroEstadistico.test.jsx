import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { PanelAdministracion } from '../../pantallas/PanelAdministracion.jsx';
import { servicioAdministracion } from '../../servicios/servicioAdministracion.js';
import { servicioTablero } from '../../servicios/servicioTablero.js';

vi.mock('../../servicios/servicioAdministracion.js', () => ({
  servicioAdministracion: {
    listarClinicos: vi.fn(),
    listarPacientes: vi.fn(),
  }
}));

vi.mock('../../servicios/servicioTablero.js', () => ({
  servicioTablero: {
    obtenerTablero: vi.fn(),
    exportarTablero: vi.fn(),
  }
}));

describe('Pestaña Tablero Estadístico en PanelAdministracion', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    servicioAdministracion.listarClinicos.mockResolvedValue([]);
    servicioAdministracion.listarPacientes.mockResolvedValue([]);
    window.URL.createObjectURL = vi.fn(() => 'blob:mock-url');
    window.URL.revokeObjectURL = vi.fn();
  });

  it('permite cambiar a la pestaña Tablero y muestra datos de indicadores y gráficos base64', async () => {
    servicioTablero.obtenerTablero.mockResolvedValue({
      datos_insuficientes: false,
      mensaje: null,
      total_evaluaciones: 15,
      minimo_requerido: 5,
      indicadores: {
        total_evaluaciones: 15,
        media_phq9: 8.5,
        mediana_phq9: 8.0,
        desviacion_phq9: 2.1,
        media_gad7: 6.2,
        mediana_gad7: 6.0,
        desviacion_gad7: 1.8,
        correlacion_phq9_gad7: 0.75,
        total_crisis: '<5', // Cifra enmascarada que debe mostrarse como texto
        porcentaje_crisis: null,
        minimo_grupo: 5
      },
      graficos: {
        severidad_phq9: 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=',
        severidad_gad7: 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=',
        evaluaciones_semana: 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=',
        phq9_vs_gad7: 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII='
      }
    });

    render(<PanelAdministracion />);

    // Buscar y hacer clic en la pestaña Tablero
    const botonTablero = screen.getByRole('button', { name: /tablero/i });
    expect(botonTablero).toBeInTheDocument();
    fireEvent.click(botonTablero);

    // Debe llamar a servicioTablero con el periodo por defecto (30)
    await waitFor(() => {
      expect(servicioTablero.obtenerTablero).toHaveBeenCalledWith(30);
    });

    // Validar indicadores
    expect(await screen.findByText('15')).toBeInTheDocument();
    expect(screen.getByText('8.5')).toBeInTheDocument();
    expect(screen.getByText('6.2')).toBeInTheDocument();
    expect(screen.getByText('0.75')).toBeInTheDocument();
    // Validar que <5 se muestra tal cual como string
    expect(screen.getByText('<5')).toBeInTheDocument();
    expect(screen.getByText(/Protegido por umbral/i)).toBeInTheDocument();

    // Validar gráficos renderizados
    const imagenes = screen.getAllByRole('img');
    expect(imagenes.length).toBeGreaterThanOrEqual(4);
    expect(screen.getByAltText(/severidad de depresión en el instrumento PHQ-9/i)).toBeInTheDocument();
  });

  it('muestra mensaje amable de datos insuficientes cuando no se alcanza el umbral mínimo N', async () => {
    servicioTablero.obtenerTablero.mockResolvedValue({
      datos_insuficientes: true,
      mensaje: 'Para proteger la confidencialidad, se requiere un mínimo de 5 evaluaciones.',
      total_evaluaciones: 2,
      minimo_requerido: 5,
      indicadores: null,
      graficos: null
    });

    render(<PanelAdministracion />);
    fireEvent.click(screen.getByRole('button', { name: /tablero/i }));

    expect(await screen.findByText(/Datos insuficientes para agregación/i)).toBeInTheDocument();
    expect(screen.getByText(/requiere un mínimo de 5 evaluaciones/i)).toBeInTheDocument();
    expect(screen.getByText(/Evaluaciones registradas: 2/i)).toBeInTheDocument();
  });

  it('muestra mensaje claro cuando el servicio de reportes responde con error 503', async () => {
    const error503 = new Error('El servicio de generación de reportes no está disponible en este momento. Por favor, intente más tarde.');
    error503.status = 503;
    servicioTablero.obtenerTablero.mockRejectedValue(error503);

    render(<PanelAdministracion />);
    fireEvent.click(screen.getByRole('button', { name: /tablero/i }));

    expect(await screen.findByText(/No fue posible consultar el tablero/i)).toBeInTheDocument();
    expect(screen.getByText(/El servicio de generación de reportes y estadísticas no está disponible/i)).toBeInTheDocument();
  });

  it('permite alternar el periodo entre 7, 30, 90 días y Todo el historial (dias=0)', async () => {
    servicioTablero.obtenerTablero.mockResolvedValue({
      datos_insuficientes: true,
      mensaje: 'Insuficiente',
      total_evaluaciones: 0,
      minimo_requerido: 5,
      indicadores: null,
      graficos: null
    });

    render(<PanelAdministracion />);
    fireEvent.click(screen.getByRole('button', { name: /tablero/i }));

    await waitFor(() => {
      expect(servicioTablero.obtenerTablero).toHaveBeenCalledWith(30);
    });

    // Seleccionar 7 días
    fireEvent.click(screen.getByRole('button', { name: '7 días' }));
    await waitFor(() => {
      expect(servicioTablero.obtenerTablero).toHaveBeenCalledWith(7);
    });

    // Seleccionar Todo el historial (debe enviar 0)
    fireEvent.click(screen.getByRole('button', { name: 'Todo el historial' }));
    await waitFor(() => {
      expect(servicioTablero.obtenerTablero).toHaveBeenCalledWith(0);
    });
  });

  it('muestra banner visible e inamovible de DATOS DE DEMOSTRACIÓN cuando es_demostracion es true', async () => {
    servicioTablero.obtenerTablero.mockResolvedValue({
      datos_insuficientes: false,
      es_demostracion: true,
      mensaje: null,
      total_evaluaciones: 120,
      minimo_requerido: 5,
      indicadores: {
        total_evaluaciones: 120,
        media_phq9: 8.8,
        mediana_phq9: 8.0,
        desviacion_phq9: 2.3,
        media_gad7: 6.5,
        mediana_gad7: 6.0,
        desviacion_gad7: 1.9,
        correlacion_phq9_gad7: 0.72,
        total_crisis: 14,
        porcentaje_crisis: 11.7,
        minimo_grupo: 5
      },
      graficos: {
        severidad_phq9: 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=',
        severidad_gad7: 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=',
        evaluaciones_semana: 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=',
        phq9_vs_gad7: 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII='
      }
    });

    render(<PanelAdministracion />);
    fireEvent.click(screen.getByRole('button', { name: /tablero/i }));

    const bannerDemo = await screen.findByTestId('banner-demo');
    expect(bannerDemo).toBeInTheDocument();
    expect(screen.getByText(/DATOS DE DEMOSTRACIÓN - no corresponden a personas reales/i)).toBeInTheDocument();
  });

  it('NO muestra banner de modo demostración cuando es_demostracion es false', async () => {
    servicioTablero.obtenerTablero.mockResolvedValue({
      datos_insuficientes: false,
      es_demostracion: false,
      mensaje: null,
      total_evaluaciones: 25,
      minimo_requerido: 5,
      indicadores: {
        total_evaluaciones: 25,
        media_phq9: 7.1,
        mediana_phq9: 7.0,
        desviacion_phq9: 2.0,
        media_gad7: 5.5,
        mediana_gad7: 5.0,
        desviacion_gad7: 1.7,
        correlacion_phq9_gad7: 0.65,
        total_crisis: 0,
        porcentaje_crisis: 0.0,
        minimo_grupo: 5
      },
      graficos: {
        severidad_phq9: 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=',
        severidad_gad7: 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=',
        evaluaciones_semana: 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=',
        phq9_vs_gad7: 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII='
      }
    });

    render(<PanelAdministracion />);
    fireEvent.click(screen.getByRole('button', { name: /tablero/i }));

    await screen.findByText('25');
    expect(screen.queryByTestId('banner-demo')).not.toBeInTheDocument();
    expect(screen.queryByText(/DATOS DE DEMOSTRACIÓN/i)).not.toBeInTheDocument();
  });

  it('permite descargar el tablero en formato CSV llamando al servicio y creando enlace de descarga', async () => {
    servicioTablero.obtenerTablero.mockResolvedValue({
      datos_insuficientes: false,
      es_demostracion: false,
      indicadores: { total_evaluaciones: 10, media_phq9: 5, total_crisis: 0 },
      graficos: {}
    });
    const blobCsv = new Blob(['\uFEFFfecha;metrica\n'], { type: 'text/csv;charset=utf-8;' });
    servicioTablero.exportarTablero.mockResolvedValue(blobCsv);

    render(<PanelAdministracion />);
    fireEvent.click(screen.getByRole('button', { name: /tablero/i }));

    const botonCsv = await screen.findByRole('button', { name: /descargar csv/i });
    expect(botonCsv).toBeInTheDocument();
    fireEvent.click(botonCsv);

    await waitFor(() => {
      expect(servicioTablero.exportarTablero).toHaveBeenCalledWith(30, 'csv');
    });
  });

  it('permite descargar el informe en formato PDF llamando al servicio', async () => {
    servicioTablero.obtenerTablero.mockResolvedValue({
      datos_insuficientes: false,
      es_demostracion: false,
      indicadores: { total_evaluaciones: 10, media_phq9: 5, total_crisis: 0 },
      graficos: {}
    });
    const blobPdf = new Blob(['%PDF-1.4'], { type: 'application/pdf' });
    servicioTablero.exportarTablero.mockResolvedValue(blobPdf);

    render(<PanelAdministracion />);
    fireEvent.click(screen.getByRole('button', { name: /tablero/i }));

    const botonPdf = await screen.findByRole('button', { name: /informe pdf/i });
    expect(botonPdf).toBeInTheDocument();
    fireEvent.click(botonPdf);

    await waitFor(() => {
      expect(servicioTablero.exportarTablero).toHaveBeenCalledWith(30, 'pdf');
    });
  });

  it('muestra aviso de error si la descarga de exportación falla', async () => {
    servicioTablero.obtenerTablero.mockResolvedValue({
      datos_insuficientes: false,
      es_demostracion: false,
      indicadores: { total_evaluaciones: 10, media_phq9: 5, total_crisis: 0 },
      graficos: {}
    });
    servicioTablero.exportarTablero.mockRejectedValue(new Error('Fallo temporal al generar el archivo'));

    render(<PanelAdministracion />);
    fireEvent.click(screen.getByRole('button', { name: /tablero/i }));

    const botonCsv = await screen.findByRole('button', { name: /descargar csv/i });
    fireEvent.click(botonCsv);

    expect(await screen.findByText(/Fallo temporal al generar el archivo/i)).toBeInTheDocument();
  });

  it('renderiza la sección de Actividad del sistema con sus indicadores y los 4 gráficos con botón de descarga PNG', async () => {
    const pngDummy = 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=';
    servicioTablero.obtenerTablero.mockResolvedValue({
      datos_insuficientes: false,
      es_demostracion: false,
      indicadores: {
        total_evaluaciones: 20,
        media_phq9: 6.0,
        mediana_phq9: 6.0,
        desviacion_phq9: 1.5,
        media_gad7: 5.0,
        mediana_gad7: 5.0,
        desviacion_gad7: 1.2,
        correlacion_phq9_gad7: 0.5,
        total_crisis: 0,
        porcentaje_crisis: 0
      },
      graficos: {
        severidad_phq9: pngDummy,
        severidad_gad7: pngDummy,
        evaluaciones_semana: pngDummy,
        phq9_vs_gad7: pngDummy
      },
      actividad: {
        indicadores: {
          total_pacientes_registrados: '150',
          total_pacientes_con_evaluacion: '120',
          total_pacientes_con_cita_confirmada: '45',
          citas_propuestas: '60',
          citas_confirmadas: '45',
          citas_rechazadas: '<5',
          citas_cumplidas: '30',
          citas_canceladas: '<5',
          citas_pospuestas: '0',
          clinicos_altas: 10,
          clinicos_activados: 2,
          clinicos_desactivados: 1,
          clinicos_eliminados: 0,
          consentimientos_aceptados: '150',
          consentimientos_revocados: '<5',
          autorizaciones_otorgadas: '50',
          autorizaciones_revocadas: '<5',
          cuentas_eliminadas: '0'
        },
        graficos: {
          actividad_semanal_pacientes: pngDummy,
          estado_citas: pngDummy,
          gestion_clinicos: pngDummy,
          comparativa_pacientes: pngDummy
        }
      }
    });

    render(<PanelAdministracion />);
    fireEvent.click(screen.getByRole('button', { name: /tablero/i }));

    // Verificar encabezado de actividad
    expect(await screen.findByText(/Actividad del sistema/i)).toBeInTheDocument();
    expect(screen.getByText(/Métricas agregadas de flujo de usuarios, consultas y gestión del personal/i)).toBeInTheDocument();

    // Indicadores de actividad
    expect(screen.getByText('150')).toBeInTheDocument();
    expect(screen.getByText('Pacientes con Evaluación')).toBeInTheDocument();
    expect(screen.getByText('120')).toBeInTheDocument();
    expect(screen.getByText('Pacientes con Cita Confirmada')).toBeInTheDocument();
    expect(screen.getAllByText('45').length).toBe(2);
    expect(screen.getByText('Altas de Clínicos')).toBeInTheDocument();
    expect(screen.getByText('10')).toBeInTheDocument();

    // Gráficos de actividad por texto alternativo
    expect(screen.getByAltText(/Evolución temporal semanal de eventos de pacientes/i)).toBeInTheDocument();
    expect(screen.getByAltText(/Distribución de citas según su estado actual/i)).toBeInTheDocument();
    expect(screen.getByAltText(/Métricas de gestión de cuentas del personal clínico/i)).toBeInTheDocument();
    expect(screen.getByAltText(/Comparativa de pacientes únicos en cada etapa/i)).toBeInTheDocument();

    // Botones de descarga de imagen PNG (debe haber al menos 8)
    const botonesDescargaImagen = screen.getAllByRole('button', { name: /descargar imagen/i });
    expect(botonesDescargaImagen.length).toBe(8);
  });
});
