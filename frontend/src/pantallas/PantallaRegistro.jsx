import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAutenticacion } from '../hooks/useAutenticacion.js';
import { UserPlus, User, Mail, Lock, AlertCircle } from 'lucide-react';

export function PantallaRegistro() {
  const { registrar } = useAutenticacion();
  const navegar = useNavigate();

  const [nombreCompleto, setNombreCompleto] = useState('');
  const [correo, setCorreo] = useState('');
  const [contrasena, setContrasena] = useState('');
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState('');

  const manejarEnvio = async (evento) => {
    evento.preventDefault();
    setError('');

    if (!nombreCompleto || !correo || !contrasena) {
      setError('Todos los campos son obligatorios.');
      return;
    }

    if (contrasena.length < 10) {
      setError('La contraseña debe tener al menos 10 caracteres.');
      return;
    }

    setCargando(true);
    try {
      await registrar(nombreCompleto, correo, contrasena);
      navegar('/consentimiento');
    } catch (err) {
      setError(err.message || 'Error al registrar la cuenta. Verifica los datos.');
    } finally {
      setCargando(false);
    }
  };

  return (
    <main className="max-w-md mx-auto px-4 py-12">
      <div className="bg-white border border-slate-200 rounded-2xl p-6 sm:p-8 shadow-sm">
        <div className="text-center mb-6">
          <div className="w-12 h-12 bg-teal-100 text-teal-700 rounded-full flex items-center justify-center mx-auto mb-3">
            <UserPlus className="w-6 h-6" aria-hidden="true" />
          </div>
          <h1 className="text-xl font-bold text-slate-900">Crear una cuenta</h1>
          <p className="text-sm text-slate-500 mt-1">
            Regístrate para realizar el cribado de salud mental
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
            <label htmlFor="campo-nombre" className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
              Nombre completo
            </label>
            <div className="relative">
              <User className="w-4 h-4 text-slate-400 absolute left-3 top-3" aria-hidden="true" />
              <input
                id="campo-nombre"
                type="text"
                value={nombreCompleto}
                onChange={(e) => setNombreCompleto(e.target.value)}
                required
                autoComplete="name"
                className="w-full pl-9 pr-3 py-2 bg-slate-50 border border-slate-300 rounded-lg text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
                placeholder="Tu nombre y apellidos"
              />
            </div>
          </div>

          <div>
            <label htmlFor="campo-correo-reg" className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
              Correo electrónico
            </label>
            <div className="relative">
              <Mail className="w-4 h-4 text-slate-400 absolute left-3 top-3" aria-hidden="true" />
              <input
                id="campo-correo-reg"
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
            <label htmlFor="campo-contrasena-reg" className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
              Contraseña (mínimo 10 caracteres)
            </label>
            <div className="relative">
              <Lock className="w-4 h-4 text-slate-400 absolute left-3 top-3" aria-hidden="true" />
              <input
                id="campo-contrasena-reg"
                type="password"
                value={contrasena}
                onChange={(e) => setContrasena(e.target.value)}
                required
                autoComplete="new-password"
                aria-describedby="ayuda-contrasena-reg"
                className="w-full pl-9 pr-3 py-2 bg-slate-50 border border-slate-300 rounded-lg text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
                placeholder="••••••••••"
              />
            </div>
            <p id="ayuda-contrasena-reg" className="text-xs text-slate-500 mt-1">
              La contraseña debe tener al menos 10 caracteres
            </p>
          </div>

          <button
            type="submit"
            disabled={cargando}
            className="w-full py-2.5 px-4 bg-teal-600 hover:bg-teal-700 disabled:bg-teal-400 text-white font-medium rounded-lg text-sm transition-colors shadow-sm mt-2 flex items-center justify-center gap-2"
          >
            {cargando ? (
              <>
                <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" aria-hidden="true" />
                <span>Registrando...</span>
              </>
            ) : (
              <span>Crear cuenta y continuar</span>
            )}
          </button>
        </form>

        <div className="mt-6 pt-4 border-t border-slate-200 text-center text-xs text-slate-600">
          ¿Ya tienes cuenta?{' '}
          <Link to="/iniciar-sesion" className="text-teal-700 font-semibold hover:underline">
            Inicia sesión aquí
          </Link>
        </div>
      </div>
    </main>
  );
}
