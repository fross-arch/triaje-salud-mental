"""Modulo con plantillas y textos de orientacion para reportes de cribado.

Reglas del proyecto:
- Sistema de cribado orientativo, jamas diagnostico (sin clasificaciones DSM-5 ni CIE-11).
- Todos los identificadores en espanol, sin tildes ni enies.
"""

AVISO_LEGAL_CRIBADO = (
    "Este informe es un cribado orientativo y no constituye un diagnostico clinico."
)

TEXTO_RESULTADO_MINIMO = (
    "Tus respuestas no sugieren sintomas significativos de depresion ni de ansiedad "
    "en las ultimas dos semanas. Esto no descarta que estes pasando por otras dificultades: "
    "este cribado solo explora esas dos areas. Si te sientes mal o preocupado/a, puedes consultar "
    "con un profesional de todas formas; no necesitas un puntaje alto para pedir ayuda."
)

TEXTO_NOTA_INFORMATIVA_PIE = (
    "Este resultado es orientativo. No mide todas las situaciones de salud mental "
    "(por ejemplo duelo, estres, trauma o consumo de sustancias) y un puntaje bajo "
    "no invalida como te sientes."
)

TEXTO_PSICOEDUCACION_NEUTRAL = (
    "El malestar psicologico y emocional no responde a formulas fijas ni se reduce "
    "a una medicion numerica; es una experiencia humana ligada a historias de vida, "
    "contextos cotidianos y momentos singulares. Para su comprension y acompanamiento "
    "existen diversas perspectivas profesionales y terapeuticas con fundamento solido "
    "(entre ellas la cognitiva, la psicodinamica, la humanista y la sistemica). Este cribado "
    "no privilegia ni prescribe una orientacion especifica, ni deduce juicios sobre la personalidad "
    "a partir de los puntajes obtenidos."
)

TEXTO_SOBRE_ESTE_CRIBADO = (
    "Los cuestionarios PHQ-9 y GAD-7 son herramientas breves de tamizaje utilizadas "
    "internacionalmente para evaluar la presencia y frecuencia de manifestaciones vinculadas "
    "al animo, la vitalidad y la tension durante las ultimas dos semanas. Su funcion es exclusivamente "
    "orientativa y preventiva: permiten identificar si amerita consultar con un profesional "
    "de la salud, pero no determinan la presencia de un trastorno ni reemplazan la evaluacion "
    "integral, el criterio profesional ni la atencion en servicios de urgencias."
)

TEXTO_PROTOCOLO_CRISIS_ENCABEZADO = (
    "Atencion prioritaria y recursos de contacto inmediato: Si estas experimentando pensamientos "
    "de hacerte dano, desesperanza profunda o una situacion de angustia desbordante, por favor "
    "comunicate de inmediato con las lineas de apoyo o acude al servicio de urgencias mas cercano."
)

# Textos orientativos institucionales clasificados por severidad
TEXTOS_ORIENTACION_DEPRESION = {
    "MINIMA": (
        "El nivel observado se ubica en el rango minimo. No se identifican indicadores significativos "
        "en las ultimas dos semanas. Mantener habitos saludables de descanso, actividad fisica y conexion "
        "con personas de confianza favorece el bienestar continuado."
    ),
    "LEVE": (
        "El nivel observado sugiere la presencia de inquietudes o variaciones leves en el animo y la energia. "
        "Se sugiere prestar atencion al autocuidado, dialogar con personas cercanas y considerar una orientacion "
        "en medicina general o psicologia preventiva si estas sensaciones persisten o generan incomodidad."
    ),
    "MODERADA": (
        "El nivel observado muestra variaciones moderadas que pueden estar incidiendo en la concentracion, "
        "el sueno o la rutina cotidiana. Es recomendable programar una consulta con medicina general o "
        "solicitar una cita de valoracion psicologica a traves de tu EPS o centro de salud universitario."
    ),
    "MODERADAMENTE_SEVERA": (
        "El nivel observado evidencia un impacto considerable en el bienestar personal y en las actividades diarias. "
        "Se aconseja acudir de manera oportuna a una valoracion integral por medicina general o salud mental "
        "en tu entidad prestadora de salud (EPS), con el fin de explorar alternativas de acompanamiento profesional."
    ),
    "SEVERA": (
        "El nivel observado indica un grado alto de malestar e interferencia en las actividades habituales. "
        "Es altamente prioritario solicitar acompanamiento profesional calificado en tu EPS o centro de salud, "
        "o acercarte a una consulta prioritaria para recibir orientacion adecuada."
    ),
}

TEXTOS_ORIENTACION_ANSIEDAD = {
    "MINIMA": (
        "El indicador de ansiedad se encuentra en el rango minimo para las ultimas dos semanas. "
        "Continuar con espacios de pausa, recreacion y descanso favorece la autorregulacion emocional."
    ),
    "LEVE": (
        "Se identifican manifestaciones leves de tension o preocupacion ocasional. Se sugiere incorporar pausas activas, "
        "practicas de respiracion y reflexionar sobre los focos de preocupacion habituales."
    ),
    "MODERADA": (
        "Se observa una presencia frecuente de inquietud, tension o dificultad para relajarse. Es pertinente "
        "solicitar valoracion con un profesional de la salud o psicologia en tu EPS para adquirir herramientas de manejo."
    ),
    "SEVERA": (
        "Se observa un nivel alto de tension y preocupacion que puede limitar el desarrollo de tus actividades. "
        "Se recomienda fuertemente buscar una valoracion oportuna por medicina general o psicologia clinica "
        "a traves de tu entidad de salud."
    ),
}


def obtener_orientacion_institucional(severidad_depresion: str, severidad_ansiedad: str) -> str:
    """Retorna un texto consolidado de orientacion institucional segun las severidades reportadas."""
    clave_depresion = severidad_depresion.upper() if severidad_depresion else "MINIMA"
    clave_ansiedad = severidad_ansiedad.upper() if severidad_ansiedad else "MINIMA"

    texto_dep = TEXTOS_ORIENTACION_DEPRESION.get(
        clave_depresion,
        TEXTOS_ORIENTACION_DEPRESION["MINIMA"],
    )
    texto_ans = TEXTOS_ORIENTACION_ANSIEDAD.get(
        clave_ansiedad,
        TEXTOS_ORIENTACION_ANSIEDAD["MINIMA"],
    )

    return (
        f"Indicador de animo y vitalidad (PHQ-9): {texto_dep}\n\n"
        f"Indicador de tension y preocupacion (GAD-7): {texto_ans}"
    )
