export const ENUNCIADO_TEMPORAL = 'Durante las últimas 2 semanas, ¿con qué frecuencia le han molestado los siguientes problemas?';

export const OPCIONES_ESCALA = [
  { valor: 0, etiqueta: 'Ningún día' },
  { valor: 1, etiqueta: 'Varios días' },
  { valor: 2, etiqueta: 'Más de la mitad de los días' },
  { valor: 3, etiqueta: 'Casi todos los días' }
];

export const LISTA_PREGUNTAS = [
  // PHQ-9 (1 a 9)
  {
    numeroGlobal: 1,
    instrumento: 'PHQ9',
    indicePregunta: 1,
    nombreInstrumento: 'PHQ-9 (Ánimo y vitalidad)',
    texto: 'Poco interés o placer en hacer cosas'
  },
  {
    numeroGlobal: 2,
    instrumento: 'PHQ9',
    indicePregunta: 2,
    nombreInstrumento: 'PHQ-9 (Ánimo y vitalidad)',
    texto: 'Sentirse desanimado/a, deprimido/a o sin esperanza'
  },
  {
    numeroGlobal: 3,
    instrumento: 'PHQ9',
    indicePregunta: 3,
    nombreInstrumento: 'PHQ-9 (Ánimo y vitalidad)',
    texto: 'Problemas para dormir o para mantenerse dormido/a, o dormir demasiado'
  },
  {
    numeroGlobal: 4,
    instrumento: 'PHQ9',
    indicePregunta: 4,
    nombreInstrumento: 'PHQ-9 (Ánimo y vitalidad)',
    texto: 'Sentirse cansado/a o tener poca energía'
  },
  {
    numeroGlobal: 5,
    instrumento: 'PHQ9',
    indicePregunta: 5,
    nombreInstrumento: 'PHQ-9 (Ánimo y vitalidad)',
    texto: 'Poco apetito o comer en exceso'
  },
  {
    numeroGlobal: 6,
    instrumento: 'PHQ9',
    indicePregunta: 6,
    nombreInstrumento: 'PHQ-9 (Ánimo y vitalidad)',
    texto: 'Sentirse mal consigo mismo/a — o sentir que es un/a fracasado/a o que ha decepcionado a su familia o a usted mismo/a'
  },
  {
    numeroGlobal: 7,
    instrumento: 'PHQ9',
    indicePregunta: 7,
    nombreInstrumento: 'PHQ-9 (Ánimo y vitalidad)',
    texto: 'Dificultad para concentrarse en cosas tales como leer el periódico o ver televisión'
  },
  {
    numeroGlobal: 8,
    instrumento: 'PHQ9',
    indicePregunta: 8,
    nombreInstrumento: 'PHQ-9 (Ánimo y vitalidad)',
    texto: 'Moverse o hablar tan lentamente que otras personas lo han notado, o lo contrario: estar tan inquieto/a o intranquilo/a que se ha estado moviendo mucho más de lo habitual'
  },
  {
    numeroGlobal: 9,
    instrumento: 'PHQ9',
    indicePregunta: 9,
    nombreInstrumento: 'PHQ-9 (Ánimo y vitalidad)',
    texto: 'Pensamientos de que estaría mejor muerto/a o de hacerse daño de alguna manera'
  },
  // GAD-7 (1 a 7)
  {
    numeroGlobal: 10,
    instrumento: 'GAD7',
    indicePregunta: 1,
    nombreInstrumento: 'GAD-7 (Ansiedad y tensión)',
    texto: 'Sentirse nervioso/a, intranquilo/a o con los nervios de punta'
  },
  {
    numeroGlobal: 11,
    instrumento: 'GAD7',
    indicePregunta: 2,
    nombreInstrumento: 'GAD-7 (Ansiedad y tensión)',
    texto: 'No poder parar o controlar las preocupaciones'
  },
  {
    numeroGlobal: 12,
    instrumento: 'GAD7',
    indicePregunta: 3,
    nombreInstrumento: 'GAD-7 (Ansiedad y tensión)',
    texto: 'Preocuparse demasiado por diferentes cosas'
  },
  {
    numeroGlobal: 13,
    instrumento: 'GAD7',
    indicePregunta: 4,
    nombreInstrumento: 'GAD-7 (Ansiedad y tensión)',
    texto: 'Dificultad para relajarse'
  },
  {
    numeroGlobal: 14,
    instrumento: 'GAD7',
    indicePregunta: 5,
    nombreInstrumento: 'GAD-7 (Ansiedad y tensión)',
    texto: 'Estar tan inquieto/a que le cuesta permanecer quieto/a'
  },
  {
    numeroGlobal: 15,
    instrumento: 'GAD7',
    indicePregunta: 6,
    nombreInstrumento: 'GAD-7 (Ansiedad y tensión)',
    texto: 'Ponerse molesto/a o irritable con facilidad'
  },
  {
    numeroGlobal: 16,
    instrumento: 'GAD7',
    indicePregunta: 7,
    nombreInstrumento: 'GAD-7 (Ansiedad y tensión)',
    texto: 'Sentir miedo como si algo terrible fuera a pasar'
  }
];

export const TOTAL_PREGUNTAS = LISTA_PREGUNTAS.length;
