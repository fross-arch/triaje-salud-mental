import React, { useState, useEffect, useRef } from 'react';
import { Edit2, Lock, Mail, User, AlertCircle, X } from 'lucide-react';

export function ModalEditarPaciente({
  abierto,
  paciente,
  alGuardar,
  alCerrar,
  guardando = false,
  errorExterno = ''
}) {
  const [nombreCompleto, setNombreCompleto] = useState('');
  const [correo, setCorreo] = useState('');
  const [nuevaContrasena, setNuevaContrasena] = useState('');
  const [errorLocal, setErrorLocal] = useState('');

  const primerInputRef = useRef(null);

  useEffect(() => {
    if (abierto && paciente) {
      setNombreCompleto(paciente.nombre || paciente.nombreCompleto || '');
      setCorreo(paciente.correo || '');
      setNuevaContrasena('');
      setErrorLocal('');

      primerInputRef.current?.focus();
    }
  }, [abierto, paciente]);

  useEffect(() => {
    if (!abierto) return;

    const manejarTeclaEscape = (e) => {
      if (e.key === 'Escape') {
        alCerrar();
      }
    };

    window.addEventListener('keydown', manejarTeclaEscape);
    return () => {
      window.removeEventListener('keydown', manejarTeclaEscape);
    };
  }, [abierto, alCerrar]);

  if (!abierto || !paciente) return null;

  const manejarEnvio = (e) => {
    e.preventDefault();
    setErrorLocal('');

    const contrasenaLimpia = nuevaContrasena.trim();
    if (contrasenaLimpia.length > 0 && contrasenaLimpia.length < 10) {
      setErrorLocal('La contraseña debe tener al menos 10 caracteres.');
      return;
    }

    alGuardar({
      id: paciente.id,
      nombreCompleto: nombreCompleto.trim(),
      correo: correo.trim().toLowerCase(),
      ...(contrasenaLimpia ? { nuevaContrasena: nuevaContrasena } : {})
    });
  };

  const errorMostrar = errorLocal || errorExterno;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-xs"
      role="dialog"
      aria-modal="true"
      aria-labelledby="titulo-editar-paciente"
    >
      <div className="bg-white rounded-2xl shadow-xl border border-slate-200 max-w-lg w-full p-6 animate-in fade-in zoom-in-95">
        <div className="flex items-center justify-between mb-4">
          <h2 id="titulo-editar-paciente" className="text-base font-bold text-slate-900 flex items-center gap-2">
            <Edit2 className="w-5 h-5 text-indigo-600" aria-hidden="true" />
            <span>Editar paciente: {paciente.nombre || paciente.nombreCompleto}</span>
          </h2>
          <button
            type="button"
            onClick={alCerrar}
            className="p-1 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition-colors"
            aria-label="Cerrar modal"
          >
            <X className="w-5 h-5" aria-hidden="true" />
          </button>
        </div>

        {errorMostrar && (
          <div role="alert" className="mb-4 p-3 bg-rose-50 border border-rose-200 text-rose-800 text-xs rounded-xl flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" aria-hidden="true" />
            <span>{errorMostrar}</span>
          </div>
        )}

        <form onSubmit={manejarEnvio} className="space-y-4">
          <div>
            <label htmlFor="campo-edit-paciente-nombre" className="block text-xs font-bold text-slate-700 mb-1">
              Nombre completo:
            </label>
            <div className="relative">
              <User className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" aria-hidden="true" />
              <input
                ref={primerInputRef}
                type="text"
                id="campo-edit-paciente-nombre"
                value={nombreCompleto}
                onChange={(e) => setNombreCompleto(e.target.value)}
                className="w-full pl-9 pr-3 py-2 border border-slate-300 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500"
                required
              />
            </div>
          </div>

          <div>
            <label htmlFor="campo-edit-paciente-correo" className="block text-xs font-bold text-slate-700 mb-1">
              Correo electrónico:
            </label>
            <div className="relative">
              <Mail className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" aria-hidden="true" />
              <input
                type="email"
                id="campo-edit-paciente-correo"
                value={correo}
                onChange={(e) => setCorreo(e.target.value)}
                className="w-full pl-9 pr-3 py-2 border border-slate-300 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500"
                required
              />
            </div>
          </div>

          <div>
            <label htmlFor="campo-edit-paciente-contrasena" className="block text-xs font-bold text-slate-700 mb-1">
              Nueva contraseña:
            </label>
            <div className="relative">
              <Lock className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" aria-hidden="true" />
              <input
                type="password"
                id="campo-edit-paciente-contrasena"
                value={nuevaContrasena}
                onChange={(e) => setNuevaContrasena(e.target.value)}
                placeholder="Dejar en blanco para mantener la actual. Mínimo 10 caracteres"
                className="w-full pl-9 pr-3 py-2 border border-slate-300 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
            <p className="text-[11px] text-slate-500 mt-1">
              Dejar en blanco para mantener la actual. Mínimo 10 caracteres
            </p>
          </div>

          <div className="flex justify-end gap-2.5 pt-3 border-t border-slate-100">
            <button
              type="button"
              onClick={alCerrar}
              className="px-4 py-2 border border-slate-300 hover:bg-slate-50 text-slate-700 rounded-xl text-xs font-semibold transition-colors"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={guardando}
              className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 disabled:bg-slate-300 text-white rounded-xl text-xs font-semibold shadow-xs transition-colors"
            >
              {guardando ? 'Guardando...' : 'Guardar cambios'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default ModalEditarPaciente;
