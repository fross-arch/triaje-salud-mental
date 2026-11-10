import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAutenticacion } from '../hooks/useAutenticacion.js';
import { servicioConsentimiento } from '../servicios/servicioConsentimiento.js';
import { ShieldCheck, FileText, AlertCircle, ExternalLink, LogOut } from 'lucide-react';
import { TEXTOS_CONSENTIMIENTO } from '../constantes/textosLegales.js';

export function PantallaConsentimiento() {
  const { cerrarSesion } = useAutenticacion();
  const navegar = useNavigate();

  const [aceptaTratamiento, setAceptaTratamiento] = useState(false);
  const [declaraMayorEdad, setDeclaraMayorEdad] = useState(false);
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    let cancelado = false;
    async function verificarConsentimiento() {
      try {
        const respuesta = await servicioConsentimiento.consultarVigente();
        if (!cancelado && respuesta && respuesta.vigente === true) {
          navegar('/introduccion', { replace: true });
        }
      } catch {
        // En caso de error de red/servidor, permite al paciente ver la pantalla
      }
    }
    verificarConsentimiento();
    return () => {
      cancelado = true;
    };
  }, [navegar]);

  const puedeContinuar = aceptaTratamiento && declaraMayorEdad;

  const manejarAceptar = async (evento) => {
    evento.preventDefault();
    setError('');

    if (!puedeContinuar) {
      setError('Debes marcar ambas casillas obligatorias para continuar.');
      return;
    }

    setCargando(true);
    try {
      await servicioConsentimiento.registrarConsentimiento(true);
      navegar('/introduccion');
    } catch (err) {
      setError(err.message || 'Error al registrar el consentimiento. Inténtalo de nuevo.');
    } finally {
      setCargando(false);
    }
  };

  const manejarRechazar = async () => {
    try {
      await cerrarSesion();
    } catch {
      // Continuar con redireccion
    }
    navegar('/iniciar-sesion');
  };

  return (
    <main className="max-w-3xl mx-auto px-4 py-8 sm:py-12">
      <div className="bg-white border border-slate-200 rounded-2xl shadow-sm p-6 sm:p-10">
        <header className="border-b border-slate-200 pb-6 mb-6">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-teal-50 text-teal-800 text-xs font-semibold mb-3">
            <ShieldCheck className="w-4 h-4 text-teal-600" aria-hidden="true" />
            <span>Marco Ético y Legal · Ley 1581 de 2012</span>
          </div>
          <h1 className="text-xl sm:text-2xl font-bold text-slate-900 leading-snug">
            {TEXTOS_CONSENTIMIENTO.titulo}
          </h1>
          <p className="text-xs text-slate-500 mt-1">
            Versión {TEXTOS_CONSENTIMIENTO.version} · {TEXTOS_CONSENTIMIENTO.sistema}
          </p>
        </header>

        {error && (
          <div
            role="alert"
            className="mb-6 bg-rose-50 border border-rose-200 text-rose-800 text-sm p-4 rounded-xl flex items-center gap-2.5"
          >
            <AlertCircle className="w-5 h-5 shrink-0 text-rose-600" aria-hidden="true" />
            <span>{error}</span>
          </div>
        )}

        <div className="space-y-6 text-slate-700 text-sm leading-relaxed">
          {/* 1. Que es */}
          <section>
            <h2 className="text-base font-bold text-slate-900 mb-2">1. ¿Qué es este sistema?</h2>
            <p className="text-slate-600">{TEXTOS_CONSENTIMIENTO.seccionQueEs}</p>
          </section>

          {/* 2. Que NO es */}
          <section className="bg-amber-50/70 border border-amber-200 rounded-xl p-4">
            <h2 className="text-base font-bold text-amber-950 mb-2">2. ¿Qué NO es este sistema?</h2>
            <ul className="list-disc list-inside space-y-1.5 text-amber-900 text-xs sm:text-sm">
              {TEXTOS_CONSENTIMIENTO.seccionQueNoEs.map((punto, index) => (
                <li key={index}>{punto}</li>
              ))}
            </ul>
          </section>

          {/* 3. Responsable y politica */}
          <section>
            <h2 className="text-base font-bold text-slate-900 mb-2">3. Responsable del tratamiento de datos</h2>
            <p className="text-slate-600">
              <strong>Responsable:</strong> {TEXTOS_CONSENTIMIENTO.seccionResponsable.responsable} · {TEXTOS_CONSENTIMIENTO.seccionResponsable.contexto}.<br />
              <strong>Contacto:</strong> {TEXTOS_CONSENTIMIENTO.seccionResponsable.correo}.
            </p>
            <p className="mt-2 text-slate-600">
              Para conocer la totalidad de los términos, consulta la{' '}
              <Link
                to="/politica-privacidad"
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-1 text-teal-700 font-semibold hover:underline"
              >
                Política de Tratamiento de Datos Personales
                <ExternalLink className="w-3.5 h-3.5" aria-hidden="true" />
              </Link>.
            </p>
          </section>

          {/* 4. Datos tratados y sensibles */}
          <section>
            <h2 className="text-base font-bold text-slate-900 mb-2">4. Datos sensibles y carácter facultativo</h2>
            <p className="text-slate-600">{TEXTOS_CONSENTIMIENTO.seccionDatosSensibles}</p>
          </section>

          {/* 5. Derechos */}
          <section>
            <h2 className="text-base font-bold text-slate-900 mb-2">5. Tus derechos (Habeas Data)</h2>
            <p className="text-slate-600">{TEXTOS_CONSENTIMIENTO.seccionDerechos}</p>
          </section>
        </div>

        {/* Declaraciones y casillas obligatorias */}
        <form onSubmit={manejarAceptar} className="mt-8 pt-6 border-t border-slate-200 space-y-4">
          <h2 className="text-sm font-bold text-slate-900 uppercase tracking-wider">
            Declaraciones y autorización obligatorias
          </h2>

          <div className="bg-slate-50 border border-slate-200 rounded-xl p-4 space-y-3.5">
            {/* Casilla 1 */}
            <div className="flex items-start gap-3">
              <input
                id="casilla-tratamiento"
                type="checkbox"
                checked={aceptaTratamiento}
                onChange={(e) => setAceptaTratamiento(e.target.checked)}
                className="w-4 h-4 mt-1 text-teal-600 border-slate-300 rounded focus:ring-teal-500 focus:ring-2 cursor-pointer"
              />
              <label htmlFor="casilla-tratamiento" className="text-xs sm:text-sm text-slate-800 leading-normal cursor-pointer">
                <strong>Autorizo de forma previa, expresa e informada</strong> el tratamiento de mis datos personales, incluidos datos sensibles de salud, para las finalidades de cribado clínico preliminar descritas.
              </label>
            </div>

            {/* Casilla 2 */}
            <div className="flex items-start gap-3">
              <input
                id="casilla-mayoria-edad"
                type="checkbox"
                checked={declaraMayorEdad}
                onChange={(e) => setDeclaraMayorEdad(e.target.checked)}
                className="w-4 h-4 mt-1 text-teal-600 border-slate-300 rounded focus:ring-teal-500 focus:ring-2 cursor-pointer"
              />
              <label htmlFor="casilla-mayoria-edad" className="text-xs sm:text-sm text-slate-800 leading-normal cursor-pointer">
                <strong>Declaro bajo gravedad de juramento</strong> que soy mayor de 18 años y que he leído y comprendido los alcances y límites de este sistema.
              </label>
            </div>
          </div>

          {/* Acciones */}
          <div className="flex flex-col sm:flex-row items-center justify-between gap-3 pt-3">
            <button
              type="button"
              onClick={manejarRechazar}
              className="w-full sm:w-auto px-5 py-2.5 border border-slate-300 hover:bg-slate-100 text-slate-700 font-medium rounded-lg text-sm transition-colors flex items-center justify-center gap-1.5"
            >
              <LogOut className="w-4 h-4" aria-hidden="true" />
              <span>No acepto y salir</span>
            </button>

            <button
              type="submit"
              disabled={!puedeContinuar || cargando}
              className="w-full sm:w-auto px-6 py-2.5 bg-teal-600 hover:bg-teal-700 disabled:bg-slate-300 disabled:cursor-not-allowed text-white font-semibold rounded-lg text-sm transition-colors shadow-sm flex items-center justify-center gap-2"
            >
              {cargando ? (
                <>
                  <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" aria-hidden="true" />
                  <span>Registrando...</span>
                </>
              ) : (
                <span>Aceptar y continuar</span>
              )}
            </button>
          </div>
        </form>
      </div>
    </main>
  );
}
