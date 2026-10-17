import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAutenticacion } from '../../hooks/useAutenticacion.js';

export function EnrutadorProtegido() {
  const { estaAutenticado, cargando } = useAutenticacion();

  if (cargando) {
    return (
      <div className="min-h-[50vh] flex flex-col items-center justify-center gap-3" role="status" aria-live="polite">
        <div className="w-8 h-8 border-4 border-teal-600 border-t-transparent rounded-full animate-spin" aria-hidden="true" />
        <span className="text-slate-600 text-sm">Verificando sesion...</span>
      </div>
    );
  }

  if (!estaAutenticado) {
    return <Navigate to="/iniciar-sesion" replace />;
  }

  return <Outlet />;
}
