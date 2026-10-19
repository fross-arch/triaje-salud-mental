

import re
import unicodedata
from typing import Any, Dict, List

# ---------------------------------------------------------------------------
# Escala de respuesta (identica para PHQ-9 y GAD-7)
# ---------------------------------------------------------------------------
ESCALA_RESPUESTA = [
    ("Ningun dia", "0"),
    ("Varios dias", "1"),
    ("Mas de la mitad de los dias", "2"),
    ("Casi todos los dias", "3"),
]

TEXTO_COMO_RESPONDER = (
    "Cada pregunta se respondio pensando en las <b>ultimas dos semanas</b>, eligiendo cuantos dias te "
    "afecto lo que describe. Cada opcion suma de 0 a 3 puntos y la suma de todas las preguntas da el "
    "puntaje total de cada cuestionario [1][2]. Un puntaje mas alto indica que reportaste mas molestias "
    "o con mas frecuencia; no indica por que ocurren ni significa que exista un problema de salud."
)

TEXTO_NIVELES_PUNTAJE = (
    "Los puntajes de 5, 10, 15 y 20 en el PHQ-9, y de 5, 10 y 15 en el GAD-7, son los puntos de corte "
    "propuestos por los autores para ordenar los resultados por niveles [1][2]. Sirven para orientar si "
    "conviene consultar a un profesional; no son un diagnostico ni indican un tratamiento. Cualquier "
    "decision sobre acompanamiento o tratamiento la toma un profesional de la salud despues de evaluarte."
)

TEXTO_NOTA_PREGUNTA_NUEVE = (
    "<b>Sobre la pregunta 9 del PHQ-9:</b> es una pregunta de deteccion simple sobre pensamientos de hacerse "
    "dano; no es una evaluacion del riesgo. Si respondiste cualquier opcion distinta de \"Ningun dia\", se "
    "recomienda una evaluacion adicional por parte de un profesional capacitado para valorar ese riesgo."
)

# Filas: (nivel, rango PHQ-9, rango GAD-7, que se suele sugerir). Texto orientativo, no terapeutico.
TABLA_NIVELES = [
    ("Minima", "0 a 4", "0 a 4",
     "Mantener habitos de autocuidado. Si algo te preocupa, puedes consultar de todas formas."),
    ("Leve", "5 a 9", "5 a 9",
     "Observar con atencion como evolucionan las molestias y volver a responder el cuestionario mas adelante."),
    ("Moderada", "10 a 14", "10 a 14",
     "Se recomienda una consulta de valoracion con medicina general o psicologia."),
    ("Moderadamente severa", "15 a 19", "No aplica",
     "Se recomienda una valoracion profesional oportuna para explorar opciones de acompanamiento."),
    ("Severa", "20 a 27", "15 a 21",
     "Se recomienda una valoracion profesional prioritaria; si hay mucho malestar, busca atencion cuanto antes."),
]

# ---------------------------------------------------------------------------
# PHQ-9: 9 preguntas (puntaje 0-27) - fuente de los puntos de corte: [1]
# ---------------------------------------------------------------------------
ITEMS_PHQ9: List[Dict[str, Any]] = [
    {
        "numero": 1,
        "pregunta": "Poco interes o placer en hacer cosas",
        "fragmentos": ["poco interes"],
        "explicacion": (
            "Explora si las actividades que antes te resultaban agradables o interesantes (pasatiempos, "
            "salidas, conversaciones, estudio o trabajo) han dejado de generarte disfrute o ganas durante "
            "las ultimas dos semanas."
        ),
    },
    {
        "numero": 2,
        "pregunta": "Sentirse desanimado/a, deprimido/a o sin esperanza",
        "fragmentos": ["desanimado", "sin esperanza"],
        "explicacion": (
            "Explora si has sentido tristeza, desanimo o la sensacion de que las cosas no van a mejorar. "
            "Se refiere al estado de animo en general, no a un mal momento aislado: la pregunta es cuantos "
            "dias de las ultimas dos semanas te has sentido asi."
        ),
    },
    {
        "numero": 3,
        "pregunta": "Problemas para dormir o para mantenerse dormido/a, o dormir demasiado",
        "fragmentos": ["dormir"],
        "explicacion": (
            "Explora cambios en el sueno: dificultad para conciliarlo, despertarse varias veces o, al "
            "contrario, dormir mucho mas de lo habitual. El sueno tambien se ve afectado por el estres, "
            "los horarios y la salud fisica, por eso esta pregunta no se interpreta sola."
        ),
    },
    {
        "numero": 4,
        "pregunta": "Sentirse cansado/a o tener poca energia",
        "fragmentos": ["cansado", "poca energia"],
        "explicacion": (
            "Explora si has sentido cansancio o falta de energia para las tareas del dia a dia, incluso "
            "sin haber hecho un gran esfuerzo. El cansancio puede tener muchas causas (descanso "
            "insuficiente, carga de trabajo, condiciones de salud)."
        ),
    },
    {
        "numero": 5,
        "pregunta": "Poco apetito o comer en exceso",
        "fragmentos": ["apetito"],
        "explicacion": (
            "Explora cambios en tu forma habitual de comer: perder el apetito o comer mas de lo normal. "
            "La pregunta se refiere al cambio respecto a lo que es habitual para ti, no a la cantidad "
            "ni al tipo de comida."
        ),
    },
    {
        "numero": 6,
        "pregunta": "Sentirse mal consigo mismo/a o sentir que ha decepcionado a otros",
        "fragmentos": ["mal consigo", "decepcionado"],
        "explicacion": (
            "Explora como te has juzgado a ti mismo/a: sentirte un fracaso, culparte en exceso o sentir que "
            "has defraudado a tu familia o a personas importantes. Recoge como te has sentido contigo "
            "mismo/a, independientemente de si otras personas piensan lo mismo."
        ),
    },
    {
        "numero": 7,
        "pregunta": "Dificultad para concentrarse en actividades como leer o ver television",
        "fragmentos": ["concentr"],
        "explicacion": (
            "Explora si te ha costado mantener la atencion en tareas sencillas, como leer, ver una serie o "
            "seguir una conversacion. La concentracion tambien depende del sueno, del estres y de otros "
            "factores del dia a dia."
        ),
    },
    {
        "numero": 8,
        "pregunta": "Moverse o hablar tan despacio que otras personas lo notan, o estar muy inquieto/a",
        "fragmentos": ["moverse", "hablar tan despacio"],
        "explicacion": (
            "Explora si tu cuerpo o tu ritmo al hablar han cambiado de forma visible: mas lentos de lo "
            "normal o, al contrario, con una inquietud que te lleva a moverte mas de lo habitual. Se "
            "refiere a cambios que otras personas podrian notar."
        ),
    },
    {
        "numero": 9,
        "pregunta": "Pensamientos de que estaria mejor muerto/a o de hacerse dano de alguna manera",
        "fragmentos": ["pensamientos", "mejor muerto", "hacerse dano"],
        "explicacion": (
            "Explora si, en las ultimas dos semanas, han aparecido pensamientos de que seria mejor no estar "
            "vivo/a o de hacerse dano. Es la unica pregunta que activa un aviso especial en este informe: "
            "si marcaste cualquier opcion distinta de \"Ningun dia\", te recomendamos hablar pronto con una "
            "persona de confianza o comunicarte con las lineas de apoyo del recuadro de atencion prioritaria."
        ),
    },
]

# ---------------------------------------------------------------------------
# GAD-7: 7 preguntas (puntaje 0-21) - fuente de los puntos de corte: [2]
# ---------------------------------------------------------------------------
ITEMS_GAD7: List[Dict[str, Any]] = [
    {
        "numero": 1,
        "pregunta": "Sentirse nervioso/a, intranquilo/a o con los nervios de punta",
        "fragmentos": ["nervios de punta", "nervioso", "intranquilo"],
        "explicacion": (
            "Explora si has sentido nerviosismo, tension o una sensacion de estar \"al limite\" durante las "
            "ultimas dos semanas. Es la forma mas general de preguntar por la ansiedad: una inquietud "
            "interna, con o sin un motivo claro."
        ),
    },
    {
        "numero": 2,
        "pregunta": "No poder dejar de preocuparse o no poder controlar la preocupacion",
        "fragmentos": ["dejar de preocup", "controlar"],
        "explicacion": (
            "Explora si las preocupaciones te han parecido dificiles de detener, como pensamientos que "
            "vuelven una y otra vez aunque quieras dejar de pensar en ellos."
        ),
    },
    {
        "numero": 3,
        "pregunta": "Preocuparse demasiado por diferentes cosas",
        "fragmentos": ["demasiado", "diferentes cosas"],
        "explicacion": (
            "Explora si tus preocupaciones han estado repartidas en varios temas a la vez (salud, estudios, "
            "dinero, familia, futuro) y no solo en una situacion concreta. Preocuparse es normal; la "
            "pregunta mira si ha sido mas de lo que consideras habitual para ti."
        ),
    },
    {
        "numero": 4,
        "pregunta": "Dificultad para relajarse",
        "fragmentos": ["relajar"],
        "explicacion": (
            "Explora si te ha costado descansar y soltar la tension, incluso en momentos tranquilos o "
            "durante tu tiempo libre."
        ),
    },
    {
        "numero": 5,
        "pregunta": "Estar tan inquieto/a que es dificil permanecer sentado/a",
        "fragmentos": ["inquieto", "sentado"],
        "explicacion": (
            "Explora la inquietud del cuerpo: sentir que no puedes quedarte quieto/a, la necesidad de "
            "moverte, caminar o cambiar de posicion con frecuencia."
        ),
    },
    {
        "numero": 6,
        "pregunta": "Molestarse o ponerse irritable facilmente",
        "fragmentos": ["irritable", "molestar"],
        "explicacion": (
            "Explora si has estado de mal genio o con poca paciencia, con reacciones de molestia mas rapidas "
            "de lo habitual. La irritabilidad tambien puede ser una forma en que se expresa la tension "
            "emocional."
        ),
    },
    {
        "numero": 7,
        "pregunta": "Sentir miedo como si algo terrible pudiera pasar",
        "fragmentos": ["miedo", "algo terrible"],
        "explicacion": (
            "Explora si has tenido la sensacion de que algo malo iba a ocurrir, un presentimiento de peligro "
            "o de que algo terrible podia pasar, aun cuando no hubiera una razon clara."
        ),
    },
]

CATALOGO_ITEMS: Dict[str, List[Dict[str, Any]]] = {
    "PHQ9": ITEMS_PHQ9,
    "GAD7": ITEMS_GAD7,
}

# ---------------------------------------------------------------------------
# Textos de seccion que usan citas numericas [n] (ver REFERENCIAS)
# ---------------------------------------------------------------------------
TEXTO_INTRO_PHQ9 = (
    "El PHQ-9 reune nueve preguntas sobre animo, interes, sueno, energia, apetito, autoestima, "
    "concentracion, ritmo corporal y pensamientos de hacerse dano; su puntaje total va de 0 a 27 [1]. "
    "A continuacion se explica que busca explorar cada pregunta y se muestra la frecuencia que "
    "marcaste. Estas explicaciones son descriptivas: ninguna pregunta se interpreta de forma aislada."
)

TEXTO_INTRO_GAD7 = (
    "El GAD-7 reune siete preguntas sobre nerviosismo, preocupacion, dificultad para relajarse, "
    "inquietud, irritabilidad y miedo anticipatorio; su puntaje total va de 0 a 21 [2]. "
    "Igual que en el cuestionario anterior, se explica que busca explorar cada pregunta y se muestra "
    "la frecuencia que marcaste."
)

TEXTO_ORIGEN_Y_VALIDACION = (
    "<b>Origen y validacion de los cuestionarios:</b> el PHQ-9 fue presentado por Kroenke, Spitzer y "
    "Williams en 2001 como una medida breve de la severidad de los sintomas depresivos [1], a partir de "
    "un cuestionario mas amplio pensado para la atencion primaria [5]. El GAD-7 fue desarrollado por "
    "Spitzer, Kroenke, Williams y Lowe en 2006 como una medida breve de ansiedad [2]. En Colombia, la "
    "version en espanol del PHQ-9 cuenta con un estudio de validacion en atencion primaria en Bucaramanga "
    "[3], y el GAD-7 con un estudio de validez y confiabilidad en profesionales medicos colombianos [4]. "
    "Estos estudios describen como funcionan los cuestionarios en esas poblaciones; no reemplazan la "
    "valoracion de un profesional."
)

# Referencias verificadas (formato Vancouver abreviado). Los titulos conservan su forma original.
REFERENCIAS: List[str] = [
    "Kroenke K, Spitzer RL, Williams JBW. The PHQ-9: validity of a brief depression severity measure. "
    "J Gen Intern Med. 2001;16(9):606-613. doi:10.1046/j.1525-1497.2001.016009606.x",
    "Spitzer RL, Kroenke K, Williams JBW, Löwe B. A brief measure for assessing generalized anxiety "
    "disorder: the GAD-7. Arch Intern Med. 2006;166(10):1092-1097. doi:10.1001/archinte.166.10.1092",
    "Cassiani-Miranda CA, Cuadros-Cruz AK, Torres-Pinzón H, Scoppetta O, Pinzón-Tarrazona JH, "
    "López-Fuentes WY, et al. Validez del Cuestionario de salud del paciente-9 (PHQ-9) para cribado de "
    "depresión en adultos usuarios de Atención Primaria en Bucaramanga, Colombia. Rev Colomb Psiquiatr. "
    "2021;50(1):11-21.",
    "Camargo L, Herrera-Pino J, Shelach S, Soto-Añari M, Porto MF, Alonso M, et al. Escala de ansiedad "
    "generalizada GAD-7 en profesionales médicos colombianos durante pandemia de COVID-19: validez de "
    "constructo y confiabilidad. Rev Colomb Psiquiatr. 2023;52:245-250.",
    "Spitzer RL, Kroenke K, Williams JBW. Validation and utility of a self-report version of PRIME-MD: "
    "the PHQ Primary Care Study. JAMA. 1999;282(18):1737-1744. doi:10.1001/jama.282.18.1737",
]


# ---------------------------------------------------------------------------
# Asociacion de las respuestas recibidas con el catalogo de preguntas
# ---------------------------------------------------------------------------
def _normalizar(texto: Any) -> str:
    """Minusculas y sin tildes, para comparar textos sin depender de la ortografia."""
    texto = unicodedata.normalize("NFKD", str(texto or ""))
    return texto.encode("ascii", "ignore").decode("ascii").lower().strip()


def _clave_instrumento(valor: Any) -> str:
    """Devuelve "PHQ9", "GAD7" o cadena vacia segun el nombre del instrumento recibido."""
    limpio = re.sub(r"[^a-z0-9]", "", _normalizar(valor))
    if "phq" in limpio:
        return "PHQ9"
    if "gad" in limpio:
        return "GAD7"
    return ""


def _buscar_numero_por_texto(clave_instrumento: str, texto_normalizado: str) -> int:
    """Busca la pregunta cuyo texto coincide con la respuesta recibida. Retorna 0 si no hay coincidencia."""
    if not texto_normalizado:
        return 0
    for item in CATALOGO_ITEMS[clave_instrumento]:
        if any(fragmento in texto_normalizado for fragmento in item["fragmentos"]):
            return item["numero"]
    return 0


def asociar_respuestas(respuestas_resumen: List[Any]) -> Dict[str, Dict[int, str]]:
    """Agrupa las frecuencias recibidas por instrumento y numero de pregunta.

    Prioridad para identificar la pregunta: 1) numero de item enviado, 2) coincidencia con el texto,
    3) posicion de llegada dentro del instrumento. Retorna {"PHQ9": {1: "Varios dias"}, "GAD7": {...}}.
    """
    resultado: Dict[str, Dict[int, str]] = {"PHQ9": {}, "GAD7": {}}
    posicion_por_instrumento = {"PHQ9": 0, "GAD7": 0}

    for elemento in respuestas_resumen or []:
        if not isinstance(elemento, dict):
            continue

        clave = _clave_instrumento(elemento.get("instrumento"))
        texto = elemento.get("texto") or elemento.get("texto_sintoma") or elemento.get("sintoma") or ""
        texto_normalizado = _normalizar(texto)
        frecuencia = (
            elemento.get("frecuencia")
            or elemento.get("frecuencia_reportada")
            or elemento.get("respuesta")
            or "Reportado"
        )

        candidatos = [clave] if clave else ["PHQ9", "GAD7"]
        numero = 0
        destino = clave

        item_enviado = elemento.get("item")
        if clave and isinstance(item_enviado, int) and 1 <= item_enviado <= len(CATALOGO_ITEMS[clave]):
            numero = item_enviado
        else:
            for candidato in candidatos:
                numero = _buscar_numero_por_texto(candidato, texto_normalizado)
                if numero:
                    destino = candidato
                    break

        if clave:
            posicion_por_instrumento[clave] += 1
            if not numero and posicion_por_instrumento[clave] <= len(CATALOGO_ITEMS[clave]):
                numero = posicion_por_instrumento[clave]

        if destino and numero:
            resultado[destino][numero] = str(frecuencia)

    return resultado


# ---------------------------------------------------------------------------
# Interpretacion breve de la respuesta marcada (identica para todas las preguntas)
# ---------------------------------------------------------------------------
INTERPRETACION_RESPUESTA = {
    0: "No reportaste esto en las ultimas dos semanas. No suma puntos.",
    1: "Aparecio solo algunos dias, sin ser lo habitual. Suma 1 punto.",
    2: "Estuvo presente en mas de la mitad de los dias. Suma 2 puntos.",
    3: "Estuvo presente casi a diario durante las ultimas dos semanas. Suma 3 puntos.",
}

INTERPRETACION_SIN_DATO = "No se recibio la respuesta a esta pregunta, por eso no se interpreta."

INTERPRETACION_PREGUNTA_NUEVE = (
    "Esta respuesta activa el aviso de atencion prioritaria: te recomendamos hablar pronto con una "
    "persona de confianza o con las lineas de apoyo."
)


def valor_de_frecuencia(frecuencia: Any) -> int:
    """Convierte el texto de frecuencia en su valor 0-3. Retorna -1 si no se reconoce."""
    texto = _normalizar(frecuencia)
    if "casi todos" in texto:
        return 3
    if "mas de la mitad" in texto:
        return 2
    if "varios" in texto:
        return 1
    if "ningun" in texto or "nunca" in texto:
        return 0
    return -1


def interpretar_respuesta(frecuencia: Any, es_pregunta_nueve_phq9: bool = False) -> str:
    """Retorna una explicacion breve de que significa la respuesta marcada y como suma al puntaje."""
    valor = valor_de_frecuencia(frecuencia)
    if valor < 0:
        return INTERPRETACION_SIN_DATO
    texto = INTERPRETACION_RESPUESTA[valor]
    if es_pregunta_nueve_phq9 and valor >= 1:
        texto = f"{texto} {INTERPRETACION_PREGUNTA_NUEVE}"
    return texto
