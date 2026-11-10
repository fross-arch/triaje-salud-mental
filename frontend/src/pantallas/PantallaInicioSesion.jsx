import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAutenticacion } from '../hooks/useAutenticacion.js';
import { servicioConsentimiento } from '../servicios/servicioConsentimiento.js';
import { LogIn, Mail, Lock, AlertCircle } from 'lucide-react';

export function PantallaInicioSesion() {
  const { iniciarSesion } = useAutenticacion();
  const navegar = useNavigate();

  const [correo, setCorreo] = useState('');
  const [contrasena, setContrasena] = useState('');
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState('');

  const manejarEnvio = async (evento) => {
    evento.preventDefault();
    setError('');

    if (!correo || !contrasena) {
      setError('Por favor ingresa tu correo y contraseña.');
      return;
    }

    setCargando(true);
    try {
      const resultado = await iniciarSesion(correo, contrasena);
      const rol = resultado?.usuario?.rol;

      if (rol === 'ADMINISTRADOR') {
        navegar('/administracion');
        return;
      }

      if (rol === 'CLINICO') {
        navegar('/clinico');
        return;
      }

      if (rol === 'PACIENTE') {
        // Verificar si ya cuenta con consentimiento vigente únicamente para pacientes
        try {
          const respuestaConsentimiento = await servicioConsentimiento.consultarVigente();
          if (respuestaConsentimiento && respuestaConsentimiento.vigente === true) {
            navegar('/introduccion');
          } else if (respuestaConsentimiento && respuestaConsentimiento.vigente === false) {
            navegar('/consentimiento');
          } else {
            setError('No fue posible determinar el estado de tu consentimiento. Por favor, intenta de nuevo.');
          }
        } catch (errConsentimiento) {
          setError(
            errConsentimiento.message ||
            'Error al verificar el estado del consentimiento informado. Por favor, intenta nuevamente.'
          );
        }
        return;
      }

      navegar('/inicio');
    } catch (err) {
      setError(err.message || 'Error al iniciar sesión. Revisa tus credenciales.');
    } finally {
      setCargando(false);
    }
  };

  return (
    <main className="max-w-md mx-auto px-4 py-12">
      <div className="bg-white border border-slate-200 rounded-2xl p-6 sm:p-8 shadow-sm">
        <div className="text-center mb-6">
          <div className="w-12 h-12 bg-teal-100 text-teal-700 rounded-full flex items-center justify-center mx-auto mb-3">
            <LogIn className="w-6 h-6" aria-hidden="true" />
          </div>
          <h1 className="text-xl font-bold text-slate-900">Iniciar sesión</h1>
          <p className="text-sm text-slate-500 mt-1">
            Accede al sistema de cribado preliminar en salud mental
          </p>
        </div>

        {error && (
          <div
            role="alert"
            className="mb-4 bg-rose-50 border border-rose-200 text-rose-700 text-sm p-3 rounded-xl flex items-center gap-2"
          >
            <AlertCircle className="w-4 h-4 shrink-0" aria-hidden="true" />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={manejarEnvio} className="space-y-4" noValidate>
          <div>
            <label htmlFor="campo-correo" className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
              Correo electrónico
            </label>
            <div className="relative">
              <Mail className="w-4 h-4 text-slate-400 absolute left-3 top-3" aria-hidden="true" />
              <input
                id="campo-correo"
                type="email"
                value={correo}
                onChange={(e) => setCorreo(e.target.value)}
                required
                autoComplete="email"
                className="w-full pl-9 pr-3 py-2 bg-slate-50 border border-slate-300 rounded-lg text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
                placeholder="usuario@ejemplo.com"
              />
            </div>
          </div>

          <div>
            <label htmlFor="campo-contrasena" className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
              Contraseña
            </label>
            <div className="relative">
              <Lock className="w-4 h-4 text-slate-400 absolute left-3 top-3" aria-hidden="true" />
              <input
                id="campo-contrasena"
                type="password"
                value={contrasena}
                onChange={(e) => setContrasena(e.target.value)}
                required
                autoComplete="current-password"
                className="w-full pl-9 pr-3 py-2 bg-slate-50 border border-slate-300 rounded-lg text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
                placeholder="••••••••"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={cargando}
            className="w-full py-2.5 px-4 bg-teal-600 hover:bg-teal-700 disabled:bg-teal-400 text-white font-medium rounded-lg text-sm transition-colors shadow-sm mt-2 flex items-center justify-center gap-2"
          >
            {cargando ? (
              <>
                <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" aria-hidden="true" />
                <span>Iniciando sesión...</span>
              </>
            ) : (
              <span>Ingresar</span>
            )}
          </button>
        </form>

        <div className="mt-6 pt-4 border-t border-slate-200 text-center text-xs text-slate-600">
          ¿No tienes una cuenta aún?{' '}
          <Link to="/registro" className="text-teal-700 font-semibold hover:underline">
            Crear cuenta nueva
          </Link>
        </div>
      </div>
    </main>
  );
}
