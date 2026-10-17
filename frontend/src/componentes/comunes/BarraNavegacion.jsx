import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAutenticacion } from '../../hooks/useAutenticacion.js';
import { HeartHandshake, User, LogOut, LogIn, FileText, Calendar, ClipboardList, ShieldCheck, PlusCircle } from 'lucide-react';

export function BarraNavegacion() {
  const { estaAutenticado, usuario, cerrarSesion } = useAutenticacion();
  const navegar = useNavigate();

  const manejarCerrarSesion = async () => {
    await cerrarSesion();
    navegar('/iniciar-sesion');
  };

  const rol = usuario?.rol;

  // Ruta principal del logo segun rol
  const rutaInicio = !estaAutenticado
    ? '/iniciar-sesion'
    : rol === 'CLINICO'
    ? '/clinico'
    : rol === 'ADMINISTRADOR'
    ? '/administracion'
    : '/introduccion';

  return (
    <header className="bg-white border-b border-slate-200 sticky top-0 z-30 shadow-sm">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-16">
          <Link
            to={rutaInicio}
            className="flex items-center gap-2 text-slate-800 hover:text-teal-700 transition-colors font-semibold text-lg"
            aria-label="Ir a la pagina principal"
          >
            <div className="w-9 h-9 rounded-lg bg-teal-600 flex items-center justify-center text-white">
              <HeartHandshake className="w-5 h-5" aria-hidden="true" />
            </div>
            <span>Cribado de Salud Mental</span>
          </Link>

          <nav className="flex items-center gap-1.5 sm:gap-3" aria-label="Navegacion principal">
            <Link
              to="/politica-privacidad"
              className="text-sm font-medium text-slate-600 hover:text-slate-900 px-3 py-2 rounded-md hover:bg-slate-100 flex items-center gap-1.5 transition-colors"
            >
              <FileText className="w-4 h-4 text-slate-500" aria-hidden="true" />
              <span className="hidden sm:inline">Politica de Datos</span>
            </Link>

            {estaAutenticado ? (
              <>
                {/* Opciones para PACIENTE */}
                {rol === 'PACIENTE' && (
                  <>
                    <Link
                      to="/introduccion"
                      className="text-sm font-medium text-slate-700 hover:text-teal-700 px-2.5 py-1.5 rounded-md hover:bg-slate-100 flex items-center gap-1 transition-colors"
                    >
                      <PlusCircle className="w-4 h-4 text-teal-600" aria-hidden="true" />
                      <span className="hidden md:inline">Nuevo cribado</span>
                    </Link>
                    <Link
                      to="/mis-citas"
                      className="text-sm font-medium text-slate-700 hover:text-teal-700 px-2.5 py-1.5 rounded-md hover:bg-slate-100 flex items-center gap-1 transition-colors"
                    >
                      <Calendar className="w-4 h-4 text-teal-600" aria-hidden="true" />
                      <span className="hidden md:inline">Mis citas</span>
                    </Link>
                  </>
                )}

                {/* Opciones para CLINICO */}
                {rol === 'CLINICO' && (
                  <>
                    <Link
                      to="/clinico"
                      className="text-sm font-medium text-slate-700 hover:text-teal-700 px-2.5 py-1.5 rounded-md hover:bg-slate-100 flex items-center gap-1 transition-colors"
                    >
                      <ClipboardList className="w-4 h-4 text-teal-600" aria-hidden="true" />
                      <span className="hidden md:inline">Portal Clínico</span>
                    </Link>
                    <Link
                      to="/clinico/citas"
                      className="text-sm font-medium text-slate-700 hover:text-teal-700 px-2.5 py-1.5 rounded-md hover:bg-slate-100 flex items-center gap-1 transition-colors"
                    >
                      <Calendar className="w-4 h-4 text-teal-600" aria-hidden="true" />
                      <span className="hidden md:inline">Gestión de Citas</span>
                    </Link>
                  </>
                )}

                {/* Opciones para ADMINISTRADOR (sin acceso a evaluaciones ni PDFs) */}
                {rol === 'ADMINISTRADOR' && (
                  <Link
                    to="/administracion"
                    className="text-sm font-medium text-slate-700 hover:text-teal-700 px-2.5 py-1.5 rounded-md hover:bg-slate-100 flex items-center gap-1 transition-colors"
                  >
                    <ShieldCheck className="w-4 h-4 text-indigo-600" aria-hidden="true" />
                    <span className="hidden md:inline">Gestión de Clínicos</span>
                  </Link>
                )}

                <Link
                  to="/perfil"
                  className="text-sm font-medium text-slate-700 hover:text-slate-900 px-2.5 py-1.5 rounded-md hover:bg-slate-100 flex items-center gap-1.5 transition-colors"
                  aria-label={`Ver perfil de ${usuario?.nombreCompleto || 'usuario'}`}
                >
                  <User className="w-4 h-4 text-teal-600" aria-hidden="true" />
                  <span className="hidden lg:inline">{usuario?.nombreCompleto || 'Perfil'}</span>
                </Link>

                <button
                  type="button"
                  onClick={manejarCerrarSesion}
                  className="text-sm font-medium text-slate-700 hover:text-rose-700 px-2.5 py-1.5 rounded-md hover:bg-rose-50 flex items-center gap-1.5 transition-colors"
                  aria-label="Cerrar sesion"
                >
                  <LogOut className="w-4 h-4" aria-hidden="true" />
                  <span className="hidden sm:inline">Cerrar sesión</span>
                </button>
              </>
            ) : (
              <Link
                to="/iniciar-sesion"
                className="text-sm font-medium bg-teal-600 hover:bg-teal-700 text-white px-3.5 py-2 rounded-md flex items-center gap-1.5 transition-colors shadow-sm"
              >
                <LogIn className="w-4 h-4" aria-hidden="true" />
                <span>Ingresar</span>
              </Link>
            )}
          </nav>
        </div>
      </div>
    </header>
  );
}
