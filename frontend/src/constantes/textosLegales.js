export const TEXTO_RESULTADO_MINIMO =
  'Tus respuestas no sugieren síntomas significativos de depresión ni de ansiedad en las últimas dos semanas. Esto no descarta que estés pasando por otras dificultades: este cribado solo explora esas dos áreas. Si te sientes mal o preocupado/a, puedes consultar con un profesional de todas formas; no necesitas un puntaje alto para pedir ayuda.';

export const TEXTO_CUESTIONARIO_INDIVIDUAL_MINIMO =
  'Tus respuestas en este cuestionario no sugieren síntomas significativos.';

export const TEXTO_NOTA_INFORMATIVA =
  'Este resultado es orientativo. No mide todas las situaciones de salud mental (por ejemplo duelo, estrés, trauma o consumo de sustancias) y un puntaje bajo no invalida cómo te sientes.';

export const TEXTO_ERROR_REPORTE_503 =
  'El servicio de generación de reportes no se encuentra disponible temporalmente. Por favor, intenta de nuevo más tarde.';

export const TEXTOS_CONSENTIMIENTO = {
  titulo: 'Consentimiento Informado y Autorización para el Tratamiento de Datos Personales',
  version: '1.0',
  sistema: 'Sistema Web de Cribado Clínico Preliminar y Orientación en Salud Mental',
  seccionQueEs:
    'Es una herramienta de cribado (tamizaje) que te ayuda a identificar si algunos síntomas de ánimo y ansiedad que has tenido en las últimas dos semanas podrían merecer atención profesional. Usa dos cuestionarios conocidos: el PHQ-9 (ánimo) y el GAD-7 (ansiedad).',
  seccionQueNoEs: [
    'No es un diagnóstico ni reemplaza la valoración de un profesional de la salud.',
    'No es un servicio de emergencias ni de atención de crisis.',
    'No notifica a ningún profesional, institución ni persona sobre tus respuestas. Nadie será avisado si indicas que estás en riesgo.',
    'Es un proyecto académico de CESDE y no cuenta con validación clínica propia.'
  ],
  seccionResponsable: {
    responsable: 'Mariana Rico, Bryan Rafael Mendoza, Sebastián Gonzales y Yadir Morales',
    contexto: 'Proyecto académico de Técnico en Desarrollo de Software, CESDE',
    correo: 'cesde@cesde.edu.co'
  },
  seccionDatosTratados: [
    'Tu nombre y tu correo electrónico.',
    'Tus respuestas a los cuestionarios PHQ-9 y GAD-7, y los resultados calculados a partir de ellas.',
    'Registros básicos de uso (inicio de sesión, aceptación del consentimiento y descarga de reportes).'
  ],
  seccionDatosSensibles:
    'Tus respuestas sobre tu estado emocional son datos sensibles de salud. Responder es voluntario, puedes dejar la evaluación en cualquier momento con el botón "Salir", y solo se tratarán tus datos si aceptas expresamente este documento.',
  seccionDerechos:
    'Conforme a la Ley 1581 de 2012, tienes derecho a conocer, actualizar, rectificar y suprimir tus datos, así como a revocar esta autorización en cualquier momento desde tu perfil.'
};

export const TEXTOS_INFORMACION_CUESTIONARIOS = {
  titulo: 'Sobre los cuestionarios que vas a responder',
  introduccion:
    'Vas a responder dos cuestionarios breves (16 preguntas en total). Sirven para orientarte sobre si algunos síntomas de ánimo y ansiedad que has tenido recientemente podrían merecer atención profesional. No son un diagnóstico.',
  periodo: 'Las últimas 2 semanas',
  instrumentos: [
    {
      nombre: 'PHQ-9',
      explora: 'Síntomas de depresión',
      preguntas: 9,
      aspectos:
        'Interés o placer en las cosas, ánimo, sueño, energía, apetito, cómo te ves a ti mismo/a, concentración, lentitud o inquietud, y pensamientos de hacerte daño.'
    },
    {
      nombre: 'GAD-7',
      explora: 'Síntomas de ansiedad',
      preguntas: 7,
      aspectos:
        'Nerviosismo, dificultad para controlar la preocupación, preocupación excesiva, dificultad para relajarte, inquietud, irritabilidad y miedo a que algo malo pase.'
    }
  ],
  escala: [
    { opcion: 'Ningún día', puntos: '0 puntos' },
    { opcion: 'Varios días', puntos: '1 punto' },
    { opcion: 'Más de la mitad de los días', puntos: '2 puntos' },
    { opcion: 'Casi todos los días', puntos: '3 puntos' }
  ],
  limites: [
    'No diagnostican: solo un profesional de la salud puede diagnosticar tras valoración completa.',
    'No miden todo: no exploran duelo, estrés, trauma, problemas de sueño específicos ni consumo de sustancias.',
    'Un resultado bajo no invalida tu sentir: puedes buscar apoyo profesional ante cualquier malestar sin requerir un puntaje alto.',
    'No reemplazan la atención de un profesional ni un servicio de emergencias.'
  ],
  sobreItem9:
    'La pregunta 9 del PHQ-9 indaga sobre pensamientos de hacerse daño. Si respondes cualquier opción distinta de "Ningún día", el sistema desplegará de inmediato recursos y líneas de ayuda. Puedes continuar con la evaluación o salir cuando gustes. El sistema no avisa a ninguna entidad externa.'
};

export const TEXTO_ADVERTENCIA_CRISIS_COMPARTIR =
  'Compartir tu resultado con un psicólogo no activa una alerta ni garantiza que te contacten de inmediato. Si necesitas ayuda ahora, usa las líneas de apoyo.';

export const TEXTO_ADVERTENCIA_DATOS_CLINICOS_CITA =
  'No incluyas datos clínicos';

export const TEXTOS_AUTORIZACION_COMPARTIR = {
  titulo: 'Autorización para Compartir Resultados con un Profesional',
  version: '1.0',
  sistema: 'Sistema Web de Cribado Clínico Preliminar y Orientación en Salud Mental',
  advertencias: [
    'Compartir tu resultado con un psicólogo no activa una alerta ni garantiza que te contacten de inmediato. Si necesitas ayuda ahora, usa las líneas de apoyo.',
    'Este sistema realiza un cribado preliminar; no constituye un diagnóstico formal ni reemplaza la valoración clínica.',
    'El acceso es voluntario y puedes revocarlo en cualquier momento pulsando "Dejar de compartir".',
    'El profesional autorizado solo podrá ver tus puntajes, severidades e ítems respondidos, nunca tu correo electrónico.'
  ],
  declaracionCasilla:
    'He leído y acepto los términos de la autorización. Entiendo que compartir este resultado no activa alertas de emergencia ni garantiza atención inmediata, y que puedo revocar este permiso cuando lo desee.'
};
