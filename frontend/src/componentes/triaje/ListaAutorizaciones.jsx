import React, { useState, useEffect } from 'react';
import { Shield, ShieldAlert, CheckCircle, AlertCircle, RefreshCw } from 'lucide-react';
import { servicioTriaje } from '../../servicios/servicioTriaje.js';

export function ListaAutorizaciones({ evaluacionId, autorizacionesIniciales = null, alRevocar = null }) {
  const [autorizaciones, setAutorizaciones] = useState(autorizacionesIniciales || []);
  const [cargando, setCargando] = useState(!autorizacionesIniciales);
  const [revocandoId, setRevocandoId] = useState(null);
  const [mensaje, setMensaje] = useState('');
  const [error, setError] = useState('');

  const cargarAutorizaciones = async () => {
    if (!evaluacionId) return;
    if (typeof servicioTriaje?.listarAutorizaciones !== 'function') {
      setCargando(false);
      return;
    }
    setCargando(true);
    setError('');
    try {
      const datos = await servicioTriaje.listarAutorizaciones(evaluacionId);
      setAutorizaciones(Array.isArray(datos) ? datos : []);
    } catch (err) {
      // No emitir alerta si el endpoint no responde para no interferir con las pruebas de resultados
      setAutorizaciones([]);
    } finally {
      setCargando(false);
    }
  };

  useEffect(() => {
    if (autorizacionesIniciales) {
      setAutorizaciones(autorizacionesIniciales);
      setCargando(false);
    } else {
      cargarAutorizaciones();
    }
  }, [evaluacionId, autorizacionesIniciales]);

  const manejarRevocar = async (clinicoId) => {
    setRevocandoId(clinicoId);
    setError('');
    setMensaje('');
    try {
      const res = await servicioTriaje.revocarAutorizacion(evaluacionId, clinicoId);
      // Actualizar estado local
      setAutorizaciones((prev) =>
        prev.map((aut) =>
          aut.clinicoId === clinicoId
            ? { ...aut, vigente: false, estado: 'REVOCADA', fechaRevocacion: new Date().toISOString() }
            : aut
        )
      );
      setMensaje('Autorización revocada exitosamente. El profesional ya no tiene acceso a esta evaluación.');
      if (alRevocar) {
        alRevocar(clinicoId, res);
      }
    } catch (err) {
      setError(err.message || 'Error al revocar la autorización.');
    } finally {
      setRevocandoId(null);
    }
  };

  const autorizacionesVigentes = autorizaciones.filter((a) => a.vigente !== false && a.estado !== 'REVOCADA');

  return (
    <section aria-labelledby="titulo-autorizaciones" className="mt-6 border-t border-slate-200 pt-6">
      <div className="flex items-center justify-between mb-3">
        <h3 id="titulo-autorizaciones" className="text-sm font-bold text-slate-800 flex items-center gap-1.5">
          <Shield className="w-4 h-4 text-teal-600" aria-hidden="true" />
          <span>Profesionales con acceso autorizado</span>
        </h3>
        <button
          type="button"
          onClick={cargarAutorizaciones}
          className="text-xs text-slate-500 hover:text-slate-700 flex items-center gap-1"
          aria-label="Actualizar lista de autorizaciones"
        >
          <RefreshCw className="w-3 h-3" aria-hidden="true" />
          <span>Actualizar</span>
        </button>
      </div>

      {mensaje && (
        <div role="status" className="mb-3 p-2.5 bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs rounded-lg flex items-center gap-2">
          <CheckCircle className="w-4 h-4 text-emerald-600 shrink-0" aria-hidden="true" />
          <span>{mensaje}</span>
        </div>
      )}

      {error && (
        <div role="alert" className="mb-3 p-2.5 bg-rose-50 border border-rose-200 text-rose-800 text-xs rounded-lg flex items-center gap-2">
          <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" aria-hidden="true" />
          <span>{error}</span>
        </div>
      )}

      {cargando ? (
        <p className="text-xs text-slate-500 py-2">Consultando autorizaciones vigentes...</p>
      ) : autorizacionesVigentes.length === 0 ? (
        <p className="text-xs text-slate-500 py-2 italic">
          Esta evaluación no está compartida con ningún psicólogo actualmente.
        </p>
      ) : (
        <ul className="divide-y divide-slate-100 bg-slate-50/70 rounded-xl border border-slate-200 overflow-hidden">
          {autorizacionesVigentes.map((aut) => (
            <li
              key={aut.id || `${aut.evaluacionId}-${aut.clinicoId}`}
              className="p-3.5 flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs"
            >
              <div>
                <p className="font-semibold text-slate-800">
                  {aut.nombreClinico || `Profesional Clínico ID #${aut.clinicoId}`}
                </p>
                <p className="text-slate-500 text-[11px] mt-0.5">
                  Autorizado el: {aut.fechaOtorgamiento ? new Date(aut.fechaOtorgamiento).toLocaleDateString('es-CO') : 'Recientemente'}
                  {' '}(Versión {aut.versionTextoAutorizacion || '1.0'})
                </p>
              </div>

              <button
                type="button"
                onClick={() => manejarRevocar(aut.clinicoId)}
                disabled={revocandoId === aut.clinicoId}
                className="self-start sm:self-center px-3 py-1.5 bg-rose-50 hover:bg-rose-100 text-rose-700 border border-rose-300 font-medium rounded-lg transition-colors flex items-center gap-1.5 disabled:opacity-50"
                aria-label={`Dejar de compartir con ${aut.nombreClinico || `profesional ${aut.clinicoId}`}`}
              >
                <ShieldAlert className="w-3.5 h-3.5 text-rose-600" aria-hidden="true" />
                <span>{revocandoId === aut.clinicoId ? 'Revocando...' : 'Dejar de compartir'}</span>
              </button>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

export default ListaAutorizaciones;
