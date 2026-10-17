import React from 'react';
import { Link } from 'react-router-dom';
import { FileText, ArrowLeft, Shield } from 'lucide-react';

export function PantallaPoliticaPrivacidad() {
  return (
    <main className="max-w-4xl mx-auto px-4 py-8 sm:py-12">
      <div className="bg-white border border-slate-200 rounded-2xl p-6 sm:p-10 shadow-sm">
        <div className="mb-6">
          <Link
            to="/iniciar-sesion"
            className="text-xs font-semibold text-teal-700 hover:text-teal-900 inline-flex items-center gap-1 mb-4"
          >
            <ArrowLeft className="w-3.5 h-3.5" aria-hidden="true" />
            <span>Volver</span>
          </Link>

          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-teal-50 text-teal-800 text-xs font-semibold mb-2">
            <Shield className="w-3.5 h-3.5 text-teal-600" aria-hidden="true" />
            <span>Ley 1581 de 2012 · Versión 2.0</span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-bold text-slate-900">
            Política de Tratamiento de Datos Personales
          </h1>
          <p className="text-xs text-slate-500 mt-1">
            Sistema Web de Cribado Clínico Preliminar y Orientación en Salud Mental
          </p>
        </div>

        <div className="space-y-6 text-slate-700 text-sm leading-relaxed border-t border-slate-200 pt-6">
          <section>
            <h2 className="text-base font-bold text-slate-900 mb-1">1. Identificación de los responsables</h2>
            <p className="text-slate-600">
              <strong>Responsables:</strong> Mariana Rico, Bryan Rafael Mendoza, Sebastián Gonzales y Yadir Morales<br />
              <strong>Contexto:</strong> Proyecto académico de Técnico en Desarrollo de Software, CESDE<br />
              <strong>Domicilio:</strong> Medellín, Colombia
            </p>
          </section>

          <section>
            <h2 className="text-base font-bold text-slate-900 mb-1">2. Marco normativo</h2>
            <p className="text-slate-600">
              Esta política se rige por la Ley 1581 de 2012, el Decreto 1377 de 2013 (compilado en el Decreto 1074 de 2015) y demás normas concordantes sobre protección de datos personales en la República de Colombia.
            </p>
          </section>

          <section>
            <h2 className="text-base font-bold text-slate-900 mb-1">3. Definiciones básicas</h2>
            <ul className="list-disc list-inside space-y-1 text-slate-600 text-xs sm:text-sm">
              <li><strong>Dato personal:</strong> Información vinculada o asociable a una persona determinada.</li>
              <li><strong>Dato sensible:</strong> Afecta la intimidad del titular o su uso indebido puede generar discriminación (en este sistema: datos sobre salud emocional).</li>
              <li><strong>Tratamiento:</strong> Cualquier operación sobre los datos (recolección, almacenamiento, uso o supresión).</li>
            </ul>
          </section>

          <section>
            <h2 className="text-base font-bold text-slate-900 mb-1">4. Datos recolectados y carácter facultativo</h2>
            <p className="text-slate-600 mb-2">
              Se recolectan: nombre completo, correo electrónico, respuestas a los instrumentos PHQ-9 y GAD-7, puntajes y niveles de severidad derivados, y registros de auditoría de seguridad.
            </p>
            <p className="text-slate-600 bg-teal-50 border border-teal-200 p-3 rounded-xl text-xs">
              <strong>Carácter facultativo:</strong> Responder los cuestionarios sobre tu estado emocional es estrictamente voluntario. Tienes derecho a decidir libremente si deseas contestar o no.
            </p>
          </section>

          <section>
            <h2 className="text-base font-bold text-slate-900 mb-1">5. Finalidades del tratamiento</h2>
            <ol className="list-decimal list-inside space-y-1 text-slate-600 text-xs sm:text-sm">
              <li>Calcular y presentar al titular los resultados de su cribado preliminar.</li>
              <li>Generar reportes en PDF a solicitud exclusiva del titular.</li>
              <li>Permitir la consulta de su historial de evaluaciones en la plataforma.</li>
              <li>Garantizar la seguridad y auditoría técnica del sistema.</li>
            </ol>
            <p className="mt-2 text-xs text-slate-500">
              Los datos no se venden, no se transfieren a terceros ni se usan con propósitos publicitarios.
            </p>
          </section>

          <section>
            <h2 className="text-base font-bold text-slate-900 mb-1">6. Mayores de 18 años</h2>
            <p className="text-slate-600">
              El servicio está dirigido exclusivamente a personas mayores de edad (18 años o más).
            </p>
          </section>

          <section>
            <h2 className="text-base font-bold text-slate-900 mb-1">7. Derechos del titular y canales de ejercicio</h2>
            <p className="text-slate-600">
              Puedes revocar el consentimiento y suprimir la totalidad de tus datos directamente desde tu sección de Perfil dentro de la plataforma. Si tienes una evaluación en curso, deberás finalizarla o cancelarla antes de poder revocar el consentimiento.
            </p>
          </section>

          <section className="bg-slate-50 border border-slate-200 rounded-xl p-4">
            <h2 className="text-base font-bold text-slate-900 mb-1">8. Transparencia y límites del sistema</h2>
            <ul className="list-disc list-inside space-y-1 text-slate-600 text-xs">
              <li>Es una herramienta de cribado preliminar, no emite diagnósticos médicos ni psicológicos.</li>
              <li>No notifica a profesionales ni a servicios de emergencia sobre las respuestas.</li>
              <li>Es un proyecto académico sin validación clínica propia.</li>
            </ul>
          </section>
        </div>
      </div>
    </main>
  );
}
