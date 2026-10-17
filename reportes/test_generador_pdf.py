"""Pruebas unitarias para el generador de reportes PDF y graficos psicometricos en memoria.

Reglas del proyecto:
- Convencion pytest (inicia con test_).
- Todo el codigo, identificadores y comentarios en espanol, sin tildes ni enies.
- Verificacion de no persistencia en disco y de un reporte explicativo de 3 a 5 paginas.
"""

import io
import os
from pathlib import Path

import pypdf
import pytest

from generador_pdf import construir_reporte_pdf
from graficador import generar_grafico_umbrales
from plantillas_orientacion import (
    AVISO_LEGAL_CRIBADO,
    TEXTO_NOTA_INFORMATIVA_PIE,
    TEXTO_RESULTADO_MINIMO,
)


def test_generar_grafico_en_memoria_sin_archivos_en_disco(tmp_path):
    """Verifica que el grafico de barras horizontal se genere en memoria y no cree archivos en disco."""
    ruta_directorio = Path(os.getcwd())
    archivos_antes = set(ruta_directorio.iterdir())

    flujo_grafico = generar_grafico_umbrales(puntaje_phq9=12, puntaje_gad7=9)

    archivos_despues = set(ruta_directorio.iterdir())

    # Comprobacion de que no se escribio ningun archivo nuevo en el sistema de archivos
    assert archivos_antes == archivos_despues, "La generacion del grafico no debe escribir archivos en disco."

    # Comprobacion del tipo de flujo y cabecera magica de formato PNG
    assert isinstance(flujo_grafico, io.BytesIO), "El grafico debe retornarse como flujo BytesIO en memoria."
    contenido_bytes = flujo_grafico.getvalue()
    assert len(contenido_bytes) > 0, "El flujo del grafico no debe estar vacio."
    assert contenido_bytes.startswith(b"\x89PNG"), "El flujo debe contener una imagen PNG valida."


def test_construir_reporte_pdf_explicativo_caso_estandar():
    """Verifica que el PDF se genere en memoria con cabecera valida y entre 3 y 5 paginas."""
    datos_solicitud = {
        "nombre_paciente": "Persona Evaluada Prueba",
        "fecha_evaluacion": "2026-10-13",
        "puntaje_phq9": 11,
        "puntaje_gad7": 8,
        "severidad_depresion": "MODERADA",
        "severidad_ansiedad": "LEVE",
        "riesgo_crisis": False,
        "resultado_minimo": False,
        "lineas_ayuda": [
            {
                "nombre": "Linea 106",
                "telefono": "106",
                "horario": "24/7",
                "descripcion": "Apoyo psicologico",
            }
        ],
        "respuestas_resumen": [
            {
                "instrumento": "PHQ-9",
                "item": 1,
                "texto": "Poco interes o placer en hacer cosas",
                "frecuencia": "Varios dias",
            },
            {
                "instrumento": "GAD-7",
                "item": 1,
                "texto": "Sentirse nervioso/a, intranquilo/a o con los nervios de punta",
                "frecuencia": "Mas de la mitad de los dias",
            },
        ],
    }

    bytes_pdf = construir_reporte_pdf(datos_solicitud)

    # Validacion de cabecera estandar PDF
    assert isinstance(bytes_pdf, bytes), "El reporte debe ser un flujo de bytes."
    assert bytes_pdf.startswith(b"%PDF-"), "El documento debe comenzar con la cabecera estandar %PDF-."

    # Validacion del rango de paginas del reporte explicativo
    lector_pdf = pypdf.PdfReader(io.BytesIO(bytes_pdf))
    assert 3 <= len(lector_pdf.pages) <= 5, f"El reporte debe tener entre 3 y 5 paginas, se obtuvieron {len(lector_pdf.pages)}."

    # Validacion de inclusion del aviso prominente y la nota informativa al pie
    texto_completo = " ".join([pagina.extract_text() for pagina in lector_pdf.pages])
    assert "cribado orientativo" in texto_completo.lower(), "Debe incluir el aviso de cribado orientativo."
    assert "no constituye un diagnostico" in texto_completo.lower(), "Debe declarar explicitamente que no es diagnostico."
    assert "no mide todas las situaciones" in texto_completo.lower(), "Debe contener la nota informativa de limites."


def test_construir_reporte_pdf_con_resultado_minimo():
    """Verifica que el reporte con resultado minimo muestre el texto especifico y mantenga 2 paginas."""
    datos_solicitud = {
        "nombre_paciente": "Usuario Sin Sintomas Significativos",
        "fecha_evaluacion": "2026-10-13",
        "puntaje_phq9": 2,
        "puntaje_gad7": 1,
        "severidad_depresion": "MINIMA",
        "severidad_ansiedad": "MINIMA",
        "riesgo_crisis": False,
        "resultado_minimo": True,
        "lineas_ayuda": [],
        "respuestas_resumen": [],
    }

    bytes_pdf = construir_reporte_pdf(datos_solicitud)
    lector_pdf = pypdf.PdfReader(io.BytesIO(bytes_pdf))

    assert 3 <= len(lector_pdf.pages) <= 5, "El reporte en nivel minimo debe tener entre 3 y 5 paginas."

    texto_completo = " ".join(pagina.extract_text() for pagina in lector_pdf.pages)
    assert "nivel minimo" in texto_completo.lower() or "sintomas significativos" in texto_completo.lower()


def test_construir_reporte_pdf_con_riesgo_crisis_prioriza_protocolo():
    """Verifica que cuando riesgo_crisis es True se incluya el recuadro prioritario y no se muestre resultado minimo."""
    datos_solicitud = {
        "nombre_paciente": "Usuario En Riesgo",
        "fecha_evaluacion": "2026-10-13",
        "puntaje_phq9": 16,
        "puntaje_gad7": 12,
        "severidad_depresion": "MODERADAMENTE_SEVERA",
        "severidad_ansiedad": "MODERADA",
        "riesgo_crisis": True,
        "resultado_minimo": True,  # Se pasa True explicitamente para comprobar que el generador lo anula
        "lineas_ayuda": [
            {
                "nombre": "Linea 123",
                "telefono": "123",
                "horario": "24/7",
                "descripcion": "Emergencias medicas y vitales",
            },
            {
                "nombre": "Linea 106",
                "telefono": "106",
                "horario": "24/7",
                "descripcion": "Acompanamiento psicosocial",
            },
        ],
        "respuestas_resumen": [
            {
                "instrumento": "PHQ-9",
                "texto": "Pensamientos de hacerse dano",
                "frecuencia": "Varios dias",
            }
        ],
    }

    bytes_pdf = construir_reporte_pdf(datos_solicitud)
    lector_pdf = pypdf.PdfReader(io.BytesIO(bytes_pdf))

    assert 3 <= len(lector_pdf.pages) <= 5, "El reporte con alerta de crisis debe tener entre 3 y 5 paginas."

    texto_primera_pagina = lector_pdf.pages[0].extract_text()
    assert "atencion prioritaria" in texto_primera_pagina.lower(), "Debe incluir el titulo de atencion prioritaria."
    assert "123" in texto_primera_pagina, "Debe contener el telefono de contacto de la linea de emergencia."

    texto_completo = " ".join(pagina.extract_text() for pagina in lector_pdf.pages)
    assert "resultado de cribado en nivel minimo" not in texto_completo.lower(), (
        "El mensaje de resultado minimo JAMAS debe mostrarse si existe riesgo de crisis."
    )


def _datos_reporte_completo():
    """Genera una solicitud con las 16 respuestas (PHQ-9 y GAD-7) y riesgo de crisis."""
    from explicaciones_items import ITEMS_GAD7, ITEMS_PHQ9

    respuestas = [
        {"instrumento": "PHQ-9", "texto": item["pregunta"], "frecuencia": "Casi todos los dias"}
        for item in ITEMS_PHQ9
    ] + [
        {"instrumento": "GAD-7", "texto": item["pregunta"], "frecuencia": "Varios dias"}
        for item in ITEMS_GAD7
    ]
    return {
        "nombre_paciente": "Persona Evaluada Prueba",
        "fecha_evaluacion": "2026-10-14",
        "puntaje_phq9": 27,
        "puntaje_gad7": 7,
        "severidad_depresion": "SEVERA",
        "severidad_ansiedad": "LEVE",
        "riesgo_crisis": True,
        "resultado_minimo": False,
        "lineas_ayuda": [{"nombre": "Linea 123", "telefono": "123", "horario": "24/7", "descripcion": "Emergencias"}],
        "respuestas_resumen": respuestas,
    }


def test_reporte_explica_las_16_preguntas_con_su_respuesta():
    """Verifica que se expliquen las 9 preguntas del PHQ-9 y las 7 del GAD-7, incluyendo la pregunta 9."""
    bytes_pdf = construir_reporte_pdf(_datos_reporte_completo())
    lector_pdf = pypdf.PdfReader(io.BytesIO(bytes_pdf))
    # Se normalizan los saltos de linea: el texto de una celda puede partirse en varias lineas
    texto = " ".join(" ".join(pagina.extract_text().split()) for pagina in lector_pdf.pages).lower()

    assert "que busca explorar" in texto
    assert "pensamientos de que estaria mejor" in texto, "Debe aparecer la pregunta 9 del PHQ-9."
    assert "sentir miedo como si algo terrible" in texto, "Debe aparecer la pregunta 7 del GAD-7."
    assert "casi todos los dias" in texto and "varios dias" in texto, "Debe mostrarse la respuesta de cada pregunta."


def test_reporte_incluye_referencias_bibliograficas_verificadas():
    """Verifica que el reporte cierre con las referencias bibliograficas del PHQ-9 y GAD-7."""
    bytes_pdf = construir_reporte_pdf(_datos_reporte_completo())
    lector_pdf = pypdf.PdfReader(io.BytesIO(bytes_pdf))
    texto = " ".join(pagina.extract_text() for pagina in lector_pdf.pages)

    assert "Referencias Bibliograficas" in texto
    assert "J Gen Intern Med" in texto and "2001;16(9):606-613" in texto
    assert "Arch Intern Med" in texto and "2006;166(10):1092-1097" in texto
    assert "Rev Colomb Psiquiatr" in texto


def test_asociacion_de_respuestas_por_numero_texto_y_posicion():
    """Verifica que las respuestas se asignen a su pregunta por item, por texto o por posicion."""
    from explicaciones_items import asociar_respuestas

    resultado = asociar_respuestas(
        [
            {"instrumento": "PHQ-9", "item": 3, "texto": "cualquiera", "frecuencia": "Varios dias"},
            {"instrumento": "PHQ-9", "texto": "Pensamientos de hacerse dano", "frecuencia": "Casi todos los dias"},
            {"instrumento": "GAD-7", "frecuencia": "Mas de la mitad de los dias"},
        ]
    )
    assert resultado["PHQ9"][3] == "Varios dias"
    assert resultado["PHQ9"][9] == "Casi todos los dias"
    assert resultado["GAD7"][1] == "Mas de la mitad de los dias"


def test_interpretacion_de_respuesta_segun_frecuencia():
    """Verifica la explicacion breve de cada frecuencia y el aviso especial de la pregunta 9."""
    from explicaciones_items import interpretar_respuesta

    assert "No suma puntos" in interpretar_respuesta("Ningun dia")
    assert "1 punto" in interpretar_respuesta("Varios dias")
    assert "2 puntos" in interpretar_respuesta("Mas de la mitad de los dias")
    assert "3 puntos" in interpretar_respuesta("Casi todos los dias")
    assert "atencion prioritaria" in interpretar_respuesta("Varios dias", es_pregunta_nueve_phq9=True)
    assert "atencion prioritaria" not in interpretar_respuesta("Ningun dia", es_pregunta_nueve_phq9=True)
    assert "no se recibio" in interpretar_respuesta(None).lower()


def test_color_de_barra_corresponde_al_nivel_del_puntaje():
    """Verifica que el nivel (y por tanto el color) cambie segun el puntaje en cada instrumento."""
    from graficador import FRANJAS_GAD7, FRANJAS_PHQ9, nivel_por_puntaje

    assert nivel_por_puntaje(2, FRANJAS_PHQ9) == "MINIMA"
    assert nivel_por_puntaje(12, FRANJAS_PHQ9) == "MODERADA"
    assert nivel_por_puntaje(17, FRANJAS_PHQ9) == "MODERADAMENTE_SEVERA"
    assert nivel_por_puntaje(27, FRANJAS_PHQ9) == "SEVERA"
    assert nivel_por_puntaje(15, FRANJAS_GAD7) == "SEVERA"
    assert nivel_por_puntaje(9, FRANJAS_GAD7) == "LEVE"


def test_barra_se_divide_en_tramos_por_cada_nivel_recorrido():
    """Verifica que la barra tenga un tramo de color por cada franja que el puntaje alcanzo o atraveso."""
    from graficador import FRANJAS_GAD7, FRANJAS_PHQ9, segmentos_barra

    assert segmentos_barra(0, FRANJAS_PHQ9) == []
    assert segmentos_barra(3, FRANJAS_PHQ9) == [(0, 3, "MINIMA")]
    assert segmentos_barra(12, FRANJAS_PHQ9) == [(0, 4, "MINIMA"), (4, 9, "LEVE"), (9, 12, "MODERADA")]
    assert [clave for _, _, clave in segmentos_barra(27, FRANJAS_PHQ9)] == [
        "MINIMA", "LEVE", "MODERADA", "MODERADAMENTE_SEVERA", "SEVERA",
    ]
    assert [clave for _, _, clave in segmentos_barra(21, FRANJAS_GAD7)] == ["MINIMA", "LEVE", "MODERADA", "SEVERA"]


def test_tabla_de_niveles_orienta_sin_diagnosticar_ni_prescribir():
    """Verifica la columna de sugerencias y la nota de la pregunta 9, sin lenguaje de tratamiento farmacologico."""
    bytes_pdf = construir_reporte_pdf(_datos_reporte_completo())
    lector_pdf = pypdf.PdfReader(io.BytesIO(bytes_pdf))
    texto = " ".join(" ".join(pagina.extract_text().split()) for pagina in lector_pdf.pages).lower()

    assert "que se suele sugerir" in texto
    assert "sobre la pregunta 9" in texto and "evaluacion adicional" in texto
    assert "5, 10, 15 y 20" in texto
    for termino_prohibido in ("farmacoterapia", "diagnostico provisional", "inicio inmediato"):
        assert termino_prohibido not in texto, f"El reporte no debe incluir: {termino_prohibido}"
