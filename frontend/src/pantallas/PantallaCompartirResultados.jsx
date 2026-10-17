import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { ArrowLeft, Share2, Shield, AlertTriangle, CheckCircle, AlertCircle, UserCheck, ShieldAlert } from 'lucide-react';
import { servicioClinicos } from '../servicios/servicioClinicos.js';
import { servicioTriaje } from '../servicios/servicioTriaje.js';
import { servicioConsentimiento } from '../servicios/servicioConsentimiento.js';
import { TEXTOS_AUTORIZACION_COMPARTIR, TEXTO_ADVERTENCIA_CRISIS_COMPARTIR } from '../constantes/textosLegales.js';

export function PantallaCompartirResultados({ evaluacionIdProp = null }) {
  const { id: evaluacionIdParam } = useParams();
  const navegar = useNavigate();
  const evaluacionId = evaluacionIdProp || evaluacionIdParam;

  const [clinicos, setClinicos] = useState([]);
  const [clinicoSeleccionado, setClinicoSeleccionado] = useState('');
  const [autorizacionAceptada, setAutorizacionAceptada] = useState(false);
  const [consentimientoVigente, setConsentimientoVigente] = useState(true);
  const [cargandoClinicos, setCargandoClinicos] = useState(true);
  const [enviando, setEnviando] = useState(false);
  const [mensajeExito, setMensajeExito] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    let montado = true;
    async function cargarDatos() {
      setCargandoClinicos(true);
      setError('');
      try {
        const [respuestaClinicos, respuestaConsentimiento] = await Promise.all([
          servicioClinicos.listarActivos(),
          servicioConsentimiento.consultarVigente().catch(() => ({ vigente: true }))
        ]);

        if (montado) {
          const lista = Array.isArray(respuestaClinicos) ? respuestaClinicos : [];
          setClinicos(lista);
          if (lista.length > 0) {
            setClinicoSeleccionado(String(lista[0].id));
          }
          if (respuestaConsentimiento && typeof respuestaConsentimiento.vigente === 'boolean') {
            setConsentimientoVigente(respuestaConsentimiento.vigente);
          }
        }
      } catch (err) {
        if (montado) {
          setError(err.message || 'No fue posible cargar la lista de profesionales disponibles.');
        }
      } finally {
        if (montado) {
          setCargandoClinicos(false);
        }
      }
    }

    cargarDatos();

    return () => {
      montado = false;
    };
  }, []);

  const manejarEnvio = async (e) => {
    e.preventDefault();
    if (!evaluacionId || !clinicoSeleccionado || !autorizacionAceptada) return;

    setEnviando(true);
    setError('');
    setMensajeExito('');

    try {
      await servicioTriaje.otorgarAutorizacion(evaluacionId, Number(clinicoSeleccionado), true);
      setMensajeExito('Se ha autorizado el acceso al psicólogo seleccionado de forma exitosa.');
    } catch (err) {
      setError(err.message || 'Error al autorizar el acceso a la evaluación.');
    } finally {
      setEnviando(false);
    }
  };

  const botonDeshabilitado =
    !autorizacionAceptada ||
    !clinicoSeleccionado ||
    enviando ||
    cargandoClinicos ||
    !consentimientoVigente;

  return (
    <main className="max-w-3xl mx-auto px-4 py-8 sm:py-12">
      <div className="mb-4">
        <Link
          to={`/resultados/${evaluacionId}`}
          className="inline-flex items-center gap-1.5 text-xs font-semibold text-teal-700 hover:text-teal-900 transition-colors"
        >
          <ArrowLeft className="w-4 h-4" aria-hidden="true" />
          <span>Volver a los resultados</span>
        </Link>
      </div>

      <div className="bg-white border border-slate-200 rounded-2xl p-6 sm:p-10 shadow-sm">
        <div className="flex items-center gap-3 border-b border-slate-200 pb-5 mb-6">
          <div className="w-10 h-10 rounded-xl bg-teal-100 text-teal-800 flex items-center justify-center shrink-0">
            <Share2 className="w-5 h-5 text-teal-700" aria-hidden="true" />
          </div>
          <div>
            <h1 className="text-xl sm:text-2xl font-bold text-slate-900">
              {TEXTOS_AUTORIZACION_COMPARTIR.titulo}
            </h1>
            <p className="text-xs text-slate-500 mt-0.5">
              {TEXTOS_AUTORIZACION_COMPARTIR.sistema} (Versión {TEXTOS_AUTORIZACION_COMPARTIR.version})
            </p>
          </div>
        </div>

        {!consentimientoVigente && (
          <div
            role="alert"
            aria-label="Aviso de consentimiento revocado"
            className="mb-6 p-4 bg-amber-50 border border-amber-300 rounded-xl text-amber-950 text-xs flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 shadow-xs"
          >
            <div className="flex items-start sm:items-center gap-2.5">
              <ShieldAlert className="w-5 h-5 text-amber-700 shrink-0 mt-0.5 sm:mt-0" aria-hidden="true" />
              <span>
                <strong>Consentimiento inactivo o revocado:</strong> Debes renovar tu consentimiento informado antes de enviar resultados o solicitar citas.
              </span>
            </div>
            <Link
              to="/consentimiento"
              className="px-3.5 py-1.5 bg-amber-600 hover:bg-amber-700 text-white rounded-lg text-xs font-semibold shrink-0 transition-colors"
            >
              Renovar consentimiento
            </Link>
          </div>
        )}

        {mensajeExito ? (
          <div role="status" className="p-6 bg-emerald-50 border border-emerald-200 rounded-xl text-center">
            <CheckCircle className="w-10 h-10 text-emerald-600 mx-auto mb-2" aria-hidden="true" />
            <h2 className="text-base font-bold text-emerald-900">¡Autorización Otorgada!</h2>
            <p className="text-xs text-emerald-800 mt-1 max-w-md mx-auto">
              {mensajeExito}
            </p>
            <div className="mt-6 flex justify-center gap-3">
              <Link
                to={`/resultados/${evaluacionId}`}
                className="px-4 py-2 bg-teal-600 hover:bg-teal-700 text-white rounded-xl text-xs font-semibold transition-colors"
              >
                Volver a la evaluación
              </Link>
            </div>
          </div>
        ) : (
          <form onSubmit={manejarEnvio} className="space-y-6">
            {error && (
              <div role="alert" className="p-3.5 bg-rose-50 border border-rose-200 text-rose-800 text-xs rounded-xl flex items-center gap-2">
                <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" aria-hidden="true" />
                <span>{error}</span>
              </div>
            )}

            {/* Seleccion de psicologo */}
            <div>
              <label htmlFor="selector-clinico" className="block text-sm font-bold text-slate-800 mb-2">
                Selecciona el profesional de la salud mental:
              </label>
              {cargandoClinicos ? (
                <p className="text-xs text-slate-500">Cargando profesionales activos...</p>
              ) : clinicos.length === 0 ? (
                <p className="text-xs text-amber-700 bg-amber-50 p-3 rounded-lg border border-amber-200">
                  No hay psicólogos activos disponibles en este momento.
                </p>
              ) : (
                <select
                  id="selector-clinico"
                  value={clinicoSeleccionado}
                  disabled={!consentimientoVigente}
                  onChange={(e) => setClinicoSeleccionado(e.target.value)}
                  className="w-full px-3.5 py-2.5 bg-white border border-slate-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500 disabled:bg-slate-100 disabled:text-slate-400"
                  aria-label="Seleccionar psicólogo para compartir evaluación"
                >
                  {clinicos.map((clinico) => (
                    <option key={clinico.id} value={clinico.id}>
                      {clinico.nombreCompleto}{clinico.especialidad ? ` — ${clinico.especialidad}` : ''}
                    </option>
                  ))}
                </select>
              )}
            </div>

            {/* Texto integro de autorizacion y advertencias */}
            <div className="bg-slate-50 border border-slate-200 rounded-xl p-5 text-xs text-slate-700 space-y-3 max-h-72 overflow-y-auto">
              <h2 className="font-bold text-slate-900 text-sm flex items-center gap-1.5">
                <Shield className="w-4 h-4 text-teal-600" aria-hidden="true" />
                <span>Términos y advertencias de la autorización (docs/autorizacion-compartir-v1.md)</span>
              </h2>

              <ul className="list-disc pl-5 space-y-2 text-slate-600">
                {TEXTOS_AUTORIZACION_COMPARTIR.advertencias.map((adv, idx) => (
                  <li key={idx} className="leading-relaxed">
                    {adv}
                  </li>
                ))}
              </ul>

              <div className="mt-3 p-3 bg-amber-50/80 border border-amber-200 rounded-lg text-amber-900 text-[11px] leading-relaxed flex items-start gap-2">
                <AlertTriangle className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" aria-hidden="true" />
                <span>
                  <strong>Atención prioritaria:</strong> En situaciones de emergencia vital o crisis severa, acude de inmediato al centro de urgencias de salud más próximo.
                </span>
              </div>
            </div>

            {/* Casilla obligatoria de autorizacion */}
            <div className="pt-2">
              <label className={`flex items-start gap-3 ${consentimientoVigente ? 'cursor-pointer' : 'cursor-not-allowed opacity-60'}`}>
                <input
                  type="checkbox"
                  id="casilla-autorizacion"
                  disabled={!consentimientoVigente}
                  checked={autorizacionAceptada}
                  onChange={(e) => setAutorizacionAceptada(e.target.checked)}
                  className="mt-0.5 h-4 w-4 rounded border-slate-300 text-teal-600 focus:ring-teal-500 disabled:cursor-not-allowed"
                  aria-describedby="texto-declaracion-autorizacion"
                />
                <span id="texto-declaracion-autorizacion" className="text-xs text-slate-700 leading-relaxed font-medium">
                  {TEXTOS_AUTORIZACION_COMPARTIR.declaracionCasilla}
                </span>
              </label>
            </div>

            {/* Boton de envio */}
            <div className="pt-4 border-t border-slate-200 flex flex-col sm:flex-row justify-end gap-3">
              <Link
                to={`/resultados/${evaluacionId}`}
                className="px-4 py-2.5 border border-slate-300 hover:bg-slate-50 text-slate-700 rounded-xl text-xs font-semibold text-center transition-colors"
              >
                Cancelar
              </Link>

              <button
                type="submit"
                disabled={botonDeshabilitado}
                className="px-5 py-2.5 bg-teal-600 hover:bg-teal-700 disabled:bg-slate-300 disabled:cursor-not-allowed text-white rounded-xl text-xs font-semibold shadow-sm transition-colors flex items-center justify-center gap-2"
                aria-label="Autorizar y compartir evaluación"
              >
                <Share2 className="w-4 h-4" aria-hidden="true" />
                <span>{enviando ? 'Enviando autorización...' : 'Autorizar y compartir'}</span>
              </button>
            </div>
          </form>
        )}
      </div>
    </main>
  );
}

export default PantallaCompartirResultados;
