import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ProveedorAutenticacion } from './contextos/ContextoAutenticacion.jsx';
import { useAutenticacion } from './hooks/useAutenticacion.js';
import { BarraNavegacion } from './componentes/comunes/BarraNavegacion.jsx';
import { RutaProtegidaPorRol } from './componentes/comunes/RutaProtegidaPorRol.jsx';
import { PantallaInicioSesion } from './pantallas/PantallaInicioSesion.jsx';
import { PantallaRegistro } from './pantallas/PantallaRegistro.jsx';
import { PantallaConsentimiento } from './pantallas/PantallaConsentimiento.jsx';
import { PantallaIntroduccion } from './pantallas/PantallaIntroduccion.jsx';
import { AsistenteTriaje } from './pantallas/AsistenteTriaje.jsx';
import { PanelResultados } from './pantallas/PanelResultados.jsx';
import { PantallaCompartirResultados } from './pantallas/PantallaCompartirResultados.jsx';
import { MisCitas } from './pantallas/MisCitas.jsx';
import { PanelClinico } from './pantallas/PanelClinico.jsx';
import { DetalleEvaluacionClinico } from './pantallas/DetalleEvaluacionClinico.jsx';
import { GestionCitasClinico } from './pantallas/GestionCitasClinico.jsx';
import { PanelAdministracion } from './pantallas/PanelAdministracion.jsx';
import { PantallaPerfil } from './pantallas/PantallaPerfil.jsx';
import { PantallaPoliticaPrivacidad } from './pantallas/PantallaPoliticaPrivacidad.jsx';
import { PaginaNoAutorizado } from './paginas/PaginaNoAutorizado.jsx';
import { AccesoDenegado } from './pantallas/AccesoDenegado.jsx';

function RedireccionInicio() {
  const { estaAutenticado, usuario, cargando } = useAutenticacion();

  if (cargando) {
    return (
      <div className="min-h-[50vh] flex flex-col items-center justify-center gap-3" role="status" aria-live="polite">
        <div className="w-8 h-8 border-4 border-teal-600 border-t-transparent rounded-full animate-spin" aria-hidden="true" />
        <span className="text-slate-600 text-sm">Cargando...</span>
      </div>
    );
  }

  if (!estaAutenticado) {
    return <Navigate to="/iniciar-sesion" replace />;
  }

  if (usuario?.rol === 'ADMINISTRADOR') {
    return <Navigate to="/administracion" replace />;
  }
  if (usuario?.rol === 'CLINICO') {
    return <Navigate to="/clinico" replace />;
  }
  return <Navigate to="/introduccion" replace />;
}

export function App() {
  return (
    <ProveedorAutenticacion>
      <BrowserRouter
        future={{
          v7_startTransition: true,
          v7_relativeSplatPath: true,
        }}
      >
        <div className="min-h-screen flex flex-col bg-slate-50 text-slate-900">
          <BarraNavegacion />
          <div className="flex-1">
            <Routes>
              {/* Rutas de redirección inicial según rol */}
              <Route path="/" element={<RedireccionInicio />} />
              <Route path="/inicio" element={<RedireccionInicio />} />

              {/* Rutas publicas */}
              <Route path="/iniciar-sesion" element={<PantallaInicioSesion />} />
              <Route path="/registro" element={<PantallaRegistro />} />
              <Route path="/politica-privacidad" element={<PantallaPoliticaPrivacidad />} />
              <Route path="/no-autorizado" element={<PaginaNoAutorizado />} />
              <Route path="/acceso-denegado" element={<PaginaNoAutorizado />} />

              {/* Rutas protegidas para PACIENTE */}
              <Route element={<RutaProtegidaPorRol rolesPermitidos={['PACIENTE']} />}>
                <Route path="/consentimiento" element={<PantallaConsentimiento />} />
                <Route path="/introduccion" element={<PantallaIntroduccion />} />
                <Route path="/asistente/:id" element={<AsistenteTriaje />} />
                <Route path="/resultados/:id" element={<PanelResultados />} />
                <Route path="/resultados/:id/compartir" element={<PantallaCompartirResultados />} />
                <Route path="/mis-citas" element={<MisCitas />} />
              </Route>

              {/* Rutas protegidas para CLINICO */}
              <Route element={<RutaProtegidaPorRol rolesPermitidos={['CLINICO']} />}>
                <Route path="/clinico" element={<PanelClinico />} />
                <Route path="/clinico/evaluaciones/:id" element={<DetalleEvaluacionClinico />} />
                <Route path="/clinico/citas" element={<GestionCitasClinico />} />
              </Route>

              {/* Rutas protegidas para ADMINISTRADOR (sin acceso a evaluaciones ni PDFs) */}
              <Route element={<RutaProtegidaPorRol rolesPermitidos={['ADMINISTRADOR']} />}>
                <Route path="/administracion" element={<PanelAdministracion />} />
              </Route>

              {/* Rutas para cualquier rol autenticado */}
              <Route element={<RutaProtegidaPorRol rolesPermitidos={['PACIENTE', 'CLINICO', 'ADMINISTRADOR']} />}>
                <Route path="/perfil" element={<PantallaPerfil />} />
              </Route>

              {/* Redireccion por defecto */}
              <Route path="*" element={<Navigate to="/iniciar-sesion" replace />} />
            </Routes>
          </div>
          <footer className="border-t border-slate-200 py-6 text-center text-xs text-slate-500 bg-white">
            <p>Sistema Web de Cribado Clínico Preliminar y Orientación en Salud Mental · Proyecto Académico CESDE</p>
            <p className="mt-1 text-[11px] text-slate-600">Herramienta orientativa. No constituye diagnóstico ni reemplaza la valoración profesional.</p>
          </footer>
        </div>
      </BrowserRouter>
    </ProveedorAutenticacion>
  );
}

export default App;
