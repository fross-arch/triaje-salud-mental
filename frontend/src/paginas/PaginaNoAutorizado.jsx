import React from 'react';
import { useNavigate } from 'react-router-dom';
import { ShieldAlert, ArrowLeft, LogOut, LogIn } from 'lucide-react';
import { useAutenticacion } from '../hooks/useAutenticacion.js';

export function PaginaNoAutorizado() {
  const { estaAutenticado, usuario, cerrarSesion } = useAutenticacion();
  const navegar = useNavigate();

  const rol = usuario?.rol;

  // Determinar ruta según el rol activo
  let rutaDestino = '/iniciar-sesion';
  if (estaAutenticado) {
    if (rol === 'ADMINISTRADOR') {
      rutaDestino = '/administracion';
    } else if (rol === 'CLINICO') {
      rutaDestino = '/clinico';
    } else {
      // PACIENTE u otros roles autenticados
      rutaDestino = '/';
    }
  }

  const manejarIrPanelPrincipal = () => {
    navegar(rutaDestino);
  };

  const manejarIniciarSesion = () => {
    navegar('/iniciar-sesion');
  };

  const manejarCerrarSesion = async () => {
    try {
      await cerrarSesion();
    } finally {
      navegar('/iniciar-sesion');
    }
  };

  return (
    <main
      className="max-w-lg mx-auto px-4 py-16 sm:py-24"
      role="region"
      aria-labelledby="titulo-no-autorizado"
    >
      <div className="bg-white border border-slate-200 rounded-2xl p-8 sm:p-10 shadow-sm text-center">
        <div
          className="w-14 h-14 bg-amber-50 text-amber-600 border border-amber-200 rounded-2xl flex items-center justify-center mx-auto mb-5"
          aria-hidden="true"
        >
          <ShieldAlert className="w-7 h-7" />
        </div>

        <span className="inline-block text-xs font-semibold tracking-wider text-slate-400 uppercase mb-2">
          Código 403 · Prohibido
        </span>

        <h1
          id="titulo-no-autorizado"
          className="text-2xl font-bold text-slate-900 tracking-tight mb-3"
        >
          Acceso No Autorizado
        </h1>

        <p className="text-sm text-slate-600 leading-relaxed mb-6">
          No cuentas con los permisos requeridos para ingresar a esta sección o tu rol actual no tiene acceso.
        </p>

        {estaAutenticado && rol && (
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-slate-100 text-slate-700 text-xs font-medium mb-8">
            <span>Sesión iniciada como: {rol}</span>
          </div>
        )}

        <div className="flex flex-col sm:flex-row items-center justify-center gap-3">
          {estaAutenticado ? (
            <>
              <button
                type="button"
                onClick={manejarIrPanelPrincipal}
                className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-6 py-2.5 bg-teal-600 hover:bg-teal-700 text-white rounded-xl text-sm font-semibold transition-colors shadow-sm focus:outline-none focus:ring-2 focus:ring-teal-500 focus:ring-offset-2"
                aria-label="Ir a mi panel principal"
              >
                <ArrowLeft className="w-4 h-4" aria-hidden="true" />
                <span>Ir a mi panel principal</span>
              </button>

              <button
                type="button"
                onClick={manejarCerrarSesion}
                className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-5 py-2.5 border border-slate-300 hover:bg-slate-50 text-slate-700 rounded-xl text-sm font-medium transition-colors shadow-sm focus:outline-none focus:ring-2 focus:ring-slate-400 focus:ring-offset-2"
                aria-label="Cerrar sesión"
              >
                <LogOut className="w-4 h-4 text-slate-500" aria-hidden="true" />
                <span>Cerrar sesión</span>
              </button>
            </>
          ) : (
            <button
              type="button"
              onClick={manejarIniciarSesion}
              className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-6 py-2.5 bg-teal-600 hover:bg-teal-700 text-white rounded-xl text-sm font-semibold transition-colors shadow-sm focus:outline-none focus:ring-2 focus:ring-teal-500 focus:ring-offset-2"
              aria-label="Iniciar sesión"
            >
              <LogIn className="w-4 h-4" aria-hidden="true" />
              <span>Iniciar sesión</span>
            </button>
          )}
        </div>
      </div>
    </main>
  );
}

export default PaginaNoAutorizado;
