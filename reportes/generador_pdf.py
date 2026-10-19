
import io
from typing import Any, Dict, List

from reportlab.lib import colors
from reportlab.lib.utils import ImageReader
from reportlab.lib.pagesizes import letter
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.pdfgen import canvas
from reportlab.platypus import (
    CondPageBreak,
    HRFlowable,
    Image,
    KeepTogether,
    PageBreak,
    Paragraph,
    SimpleDocTemplate,
    Spacer,
    Table,
    TableStyle,
)

from explicaciones_items import (
    ESCALA_RESPUESTA,
    ITEMS_GAD7,
    ITEMS_PHQ9,
    REFERENCIAS,
    TABLA_NIVELES,
    TEXTO_COMO_RESPONDER,
    TEXTO_INTRO_GAD7,
    TEXTO_INTRO_PHQ9,
    TEXTO_NIVELES_PUNTAJE,
    TEXTO_NOTA_PREGUNTA_NUEVE,
    TEXTO_ORIGEN_Y_VALIDACION,
    asociar_respuestas,
    interpretar_respuesta,
)
from graficador import PALETA_NIVELES, generar_grafico_umbrales
from plantillas_orientacion import (
    AVISO_LEGAL_CRIBADO,
    TEXTO_NOTA_INFORMATIVA_PIE,
    TEXTO_PROTOCOLO_CRISIS_ENCABEZADO,
    TEXTO_PSICOEDUCACION_NEUTRAL,
    TEXTO_RESULTADO_MINIMO,
    TEXTO_SOBRE_ESTE_CRIBADO,
    obtener_orientacion_institucional,
)


class NumeradorPaginas(canvas.Canvas):
    """Lienzo de ReportLab que realiza dos pasadas para calcular y dibujar el pie de pagina con el total exacto."""

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.paginas: List[Dict[str, Any]] = []

    def showPage(self):
        self.paginas.append(dict(self.__dict__))
        self._startPage()

    def save(self):
        total_paginas = len(self.paginas)
        for estado in self.paginas:
            self.__dict__.update(estado)
            self.dibujar_pie_pagina(total_paginas)
            super().showPage()
        super().save()

    def dibujar_pie_pagina(self, total_paginas: int):
        self.saveState()
        ancho, alto = letter

        # Linea divisoria superior del pie de pagina
        self.setStrokeColor(colors.HexColor("#cbd5e1"))
        self.setLineWidth(0.6)
        self.line(40, 32, ancho - 40, 32)

        # Texto identificativo del sistema
        self.setFont("Helvetica", 7.5)
        self.setFillColor(colors.HexColor("#64748b"))
        self.drawString(
            40,
            22,
            "Sistema Web de Cribado Clinico Preliminar y Orientacion en Salud Mental  |  Documento confidencial",
        )

        # Numeracion de pagina
        texto_pagina = f"Pagina {self._pageNumber} de {total_paginas}"
        self.drawRightString(ancho - 40, 22, texto_pagina)

        self.restoreState()


def normalizar_severidad(valor: Any) -> str:
    """Convierte la severidad a su clave de texto plano (ej. "SEVERA"), venga como str o como Enum.

    Evita que aparezcan nombres internos como "SeveridadDepresionEnum.SEVERA" en el reporte.
    """
    if valor is None:
        return "MINIMA"
    texto = str(getattr(valor, "value", valor))
    # Defensa extra: si llega "NombreEnum.CLAVE", se conserva solo la clave
    if "." in texto:
        texto = texto.split(".")[-1]
    return texto.strip().upper() or "MINIMA"


def formatear_etiqueta_severidad(severidad: str) -> str:
    """Retorna una representacion textual legible en espanol para la severidad psicometrica."""
    mapeo = {
        "MINIMA": "Minima",
        "LEVE": "Leve",
        "MODERADA": "Moderada",
        "MODERADAMENTE_SEVERA": "Moderadamente severa",
        "SEVERA": "Severa",
    }
    return mapeo.get(severidad.upper(), severidad)


def construir_reporte_pdf(datos_reporte: Dict[str, Any]) -> bytes:
    """Construye en memoria el documento PDF de cribado (varias paginas) a partir de los datos provistos."""
    flujo_pdf = io.BytesIO()

    # Documento tamano Carta con margenes optimizados para 2 paginas
    documento = SimpleDocTemplate(
        flujo_pdf,
        pagesize=letter,
        leftMargin=40,
        rightMargin=40,
        topMargin=36,
        bottomMargin=42,
    )

    ancho_util = letter[0] - 80  # 612 - 80 = 532 puntos

    hoja_estilos = getSampleStyleSheet()

    # Definicion de estilos tipograficos sobrios
    estilo_titulo_sistema = ParagraphStyle(
        "TituloSistema",
        parent=hoja_estilos["Normal"],
        fontName="Helvetica-Bold",
        fontSize=12,
        leading=15,
        textColor=colors.HexColor("#0f766e"),
    )

    estilo_subtitulo_reporte = ParagraphStyle(
        "SubtituloReporte",
        parent=hoja_estilos["Normal"],
        fontName="Helvetica",
        fontSize=9,
        leading=12,
        textColor=colors.HexColor("#334155"),
    )

    estilo_aviso_legal = ParagraphStyle(
        "AvisoLegal",
        parent=hoja_estilos["Normal"],
        fontName="Helvetica-Bold",
        fontSize=8,
        leading=11,
        textColor=colors.HexColor("#1e293b"),
        alignment=1,  # Centrado
    )

    estilo_encabezado_seccion = ParagraphStyle(
        "EncabezadoSeccion",
        parent=hoja_estilos["Normal"],
        fontName="Helvetica-Bold",
        fontSize=10,
        leading=13,
        textColor=colors.HexColor("#0f172a"),
        spaceBefore=4,
        spaceAfter=3,
    )

    estilo_texto_parrafo = ParagraphStyle(
        "TextoParrafo",
        parent=hoja_estilos["Normal"],
        fontName="Helvetica",
        fontSize=8,
        leading=11.5,
        textColor=colors.HexColor("#1e293b"),
        spaceAfter=4,
    )

    estilo_texto_parrafo_destacado = ParagraphStyle(
        "TextoParrafoDestacado",
        parent=hoja_estilos["Normal"],
        fontName="Helvetica",
        fontSize=8,
        leading=11.5,
        textColor=colors.HexColor("#0f172a"),
    )

    estilo_tabla_encabezado = ParagraphStyle(
        "TablaEncabezado",
        parent=hoja_estilos["Normal"],
        fontName="Helvetica-Bold",
        fontSize=8,
        leading=10,
        textColor=colors.white,
    )

    estilo_tabla_celda = ParagraphStyle(
        "TablaCelda",
        parent=hoja_estilos["Normal"],
        fontName="Helvetica",
        fontSize=7.5,
        leading=9.5,
        textColor=colors.HexColor("#1e293b"),
    )

    estilo_tabla_celda_destacada = ParagraphStyle(
        "TablaCeldaDestacada",
        parent=hoja_estilos["Normal"],
        fontName="Helvetica-Bold",
        fontSize=7.5,
        leading=9.5,
        textColor=colors.HexColor("#0f172a"),
    )

    estilo_alerta_crisis_titulo = ParagraphStyle(
        "AlertaCrisisTitulo",
        parent=hoja_estilos["Normal"],
        fontName="Helvetica-Bold",
        fontSize=8.5,
        leading=11,
        textColor=colors.HexColor("#991b1b"),
    )

    estilo_alerta_crisis_texto = ParagraphStyle(
        "AlertaCrisisTexto",
        parent=hoja_estilos["Normal"],
        fontName="Helvetica",
        fontSize=7.5,
        leading=10,
        textColor=colors.HexColor("#7f1d1d"),
    )

    estilo_nota_pie_recuadro = ParagraphStyle(
        "NotaPieRecuadro",
        parent=hoja_estilos["Normal"],
        fontName="Helvetica-Oblique",
        fontSize=7.5,
        leading=10.5,
        textColor=colors.HexColor("#334155"),
    )

    elementos = []

    # Extraer campos de la solicitud
    nombre_paciente = str(datos_reporte.get("nombre_paciente", "Persona usuaria"))
    fecha_evaluacion = str(datos_reporte.get("fecha_evaluacion", ""))
    puntaje_phq9 = int(datos_reporte.get("puntaje_phq9", 0))
    puntaje_gad7 = int(datos_reporte.get("puntaje_gad7", 0))
    severidad_depresion = normalizar_severidad(datos_reporte.get("severidad_depresion"))
    severidad_ansiedad = normalizar_severidad(datos_reporte.get("severidad_ansiedad"))
    riesgo_crisis = bool(datos_reporte.get("riesgo_crisis", False))
    # Invariante de seguridad clinica: si hay riesgo de crisis, resultado_minimo nunca puede ser True
    resultado_minimo = False if riesgo_crisis else bool(datos_reporte.get("resultado_minimo", False))
    lineas_ayuda = datos_reporte.get("lineas_ayuda") or []
    respuestas_resumen = datos_reporte.get("respuestas_resumen") or []

    # =========================================================================
    # PAGINA 1: IDENTIFICACION, TABLA PSICOMETRICA, GRAFICO Y SINTOMAS REPORTADOS
    # =========================================================================

    # 1. Encabezado institucional
    tabla_encabezado_contenido = [
        [
            Paragraph("Sistema Web de Cribado Clinico Preliminar", estilo_titulo_sistema),
            Paragraph(f"<b>Fecha de emision:</b> {fecha_evaluacion}", estilo_subtitulo_reporte),
        ],
        [
            Paragraph("Informe Orientativo de Salud Mental (PHQ-9 y GAD-7)", estilo_subtitulo_reporte),
            Paragraph(f"<b>Persona evaluada:</b> {nombre_paciente}", estilo_subtitulo_reporte),
        ],
    ]
    tabla_encabezado = Table(tabla_encabezado_contenido, colWidths=[ancho_util * 0.65, ancho_util * 0.35])
    tabla_encabezado.setStyle(
        TableStyle(
            [
                ("VALIGN", (0, 0), (-1, -1), "TOP"),
                ("LEFTPADDING", (0, 0), (-1, -1), 0),
                ("RIGHTPADDING", (0, 0), (-1, -1), 0),
                ("TOPPADDING", (0, 0), (-1, -1), 1),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 1),
            ]
        )
    )
    elementos.append(tabla_encabezado)
    elementos.append(Spacer(1, 4))

    # 2. Aviso prominente legal obligatorio de cribado no diagnostico
    tabla_aviso_legal = Table(
        [[Paragraph(AVISO_LEGAL_CRIBADO, estilo_aviso_legal)]],
        colWidths=[ancho_util],
    )
    tabla_aviso_legal.setStyle(
        TableStyle(
            [
                ("BACKGROUND", (0, 0), (-1, -1), colors.HexColor("#f8fafc")),
                ("BOX", (0, 0), (-1, -1), 1, colors.HexColor("#94a3b8")),
                ("TOPPADDING", (0, 0), (-1, -1), 4),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
                ("LEFTPADDING", (0, 0), (-1, -1), 8),
                ("RIGHTPADDING", (0, 0), (-1, -1), 8),
            ]
        )
    )
    elementos.append(tabla_aviso_legal)
    elementos.append(Spacer(1, 5))

    # 3. Tabla de resultados psicometricos
    elementos.append(Paragraph("1. Resumen de Instrumentos Aplicados", estilo_encabezado_seccion))

    filas_resultados = [
        [
            Paragraph("Instrumento", estilo_tabla_encabezado),
            Paragraph("Area de exploracion", estilo_tabla_encabezado),
            Paragraph("Puntaje obtenido", estilo_tabla_encabezado),
            Paragraph("Severidad preliminar", estilo_tabla_encabezado),
        ],
        [
            Paragraph("<b>PHQ-9</b>", estilo_tabla_celda_destacada),
            Paragraph("Animo, vitalidad e interes (ultimas 2 semanas)", estilo_tabla_celda),
            Paragraph(f"<b>{puntaje_phq9}</b> de 27 puntos", estilo_tabla_celda),
            Paragraph(f"<b>{formatear_etiqueta_severidad(severidad_depresion)}</b>", estilo_tabla_celda_destacada),
        ],
        [
            Paragraph("<b>GAD-7</b>", estilo_tabla_celda_destacada),
            Paragraph("Tension, preocupacion y ansiedad (ultimas 2 semanas)", estilo_tabla_celda),
            Paragraph(f"<b>{puntaje_gad7}</b> de 21 puntos", estilo_tabla_celda),
            Paragraph(f"<b>{formatear_etiqueta_severidad(severidad_ansiedad)}</b>", estilo_tabla_celda_destacada),
        ],
    ]
    tabla_resultados = Table(
        filas_resultados,
        colWidths=[ancho_util * 0.18, ancho_util * 0.44, ancho_util * 0.20, ancho_util * 0.18],
    )
    tabla_resultados.setStyle(
        TableStyle(
            [
                ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#0f766e")),
                ("BACKGROUND", (0, 1), (-1, 1), colors.HexColor("#f8fafc")),
                ("BACKGROUND", (0, 2), (-1, 2), colors.white),
                ("BACKGROUND", (3, 1), (3, 1), colors.HexColor(PALETA_NIVELES.get(severidad_depresion, PALETA_NIVELES["MINIMA"])[0])),
                ("BACKGROUND", (3, 2), (3, 2), colors.HexColor(PALETA_NIVELES.get(severidad_ansiedad, PALETA_NIVELES["MINIMA"])[0])),
                ("GRID", (0, 0), (-1, -1), 0.5, colors.HexColor("#cbd5e1")),
                ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
                ("TOPPADDING", (0, 0), (-1, -1), 3),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 3),
                ("LEFTPADDING", (0, 0), (-1, -1), 5),
                ("RIGHTPADDING", (0, 0), (-1, -1), 5),
            ]
        )
    )
    elementos.append(tabla_resultados)
    elementos.append(Spacer(1, 4))

    # 4. Grafico de barras horizontal psicometrico en memoria
    flujo_grafico = generar_grafico_umbrales(puntaje_phq9, puntaje_gad7)
    # Se conserva la proporcion real del grafico (antes se forzaba un alto fijo y el texto se aplastaba)
    ancho_px, alto_px = ImageReader(flujo_grafico).getSize()
    flujo_grafico.seek(0)
    # Ancho completo de la pagina y alto reducido (la proporcion la define la figura de graficador.py)
    imagen_grafico = Image(flujo_grafico, width=ancho_util, height=ancho_util * alto_px / ancho_px)
    elementos.append(imagen_grafico)
    elementos.append(Spacer(1, 4))

    # 5. Protocolo prioritario de crisis (si riesgo_crisis es True)
    if riesgo_crisis:
        contenido_crisis = [
            Paragraph("ATENCION PRIORITARIA: PROTOCOLO DE APOYO Y LINEAS DE CONTACTO", estilo_alerta_crisis_titulo),
            Paragraph(TEXTO_PROTOCOLO_CRISIS_ENCABEZADO, estilo_alerta_crisis_texto),
        ]

        if lineas_ayuda:
            filas_lineas = [
                [
                    Paragraph("Recurso / Linea", estilo_tabla_encabezado),
                    Paragraph("Contacto", estilo_tabla_encabezado),
                    Paragraph("Horario", estilo_tabla_encabezado),
                    Paragraph("Orientacion", estilo_tabla_encabezado),
                ]
            ]
            for linea in lineas_ayuda[:3]:  # Limitar a 3 lineas principales para balance visual
                nombre_linea = linea.get("nombre", "Linea de ayuda")
                telefono_linea = linea.get("telefono", "")
                horario_linea = linea.get("horario", "24/7")
                descripcion_linea = linea.get("descripcion", "")
                filas_lineas.append(
                    [
                        Paragraph(f"<b>{nombre_linea}</b>", estilo_tabla_celda_destacada),
                        Paragraph(f"<b>{telefono_linea}</b>", estilo_tabla_celda_destacada),
                        Paragraph(horario_linea, estilo_tabla_celda),
                        Paragraph(descripcion_linea, estilo_tabla_celda),
                    ]
                )
            tabla_lineas = Table(
                filas_lineas,
                colWidths=[(ancho_util - 12) * p for p in (0.28, 0.22, 0.16, 0.34)],
            )
            tabla_lineas.setStyle(
                TableStyle(
                    [
                        ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#991b1b")),
                        ("BACKGROUND", (0, 1), (-1, -1), colors.HexColor("#fff1f2")),
                        ("GRID", (0, 0), (-1, -1), 0.5, colors.HexColor("#fca5a5")),
                        ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
                        ("TOPPADDING", (0, 0), (-1, -1), 2),
                        ("BOTTOMPADDING", (0, 0), (-1, -1), 2),
                        ("LEFTPADDING", (0, 0), (-1, -1), 4),
                        ("RIGHTPADDING", (0, 0), (-1, -1), 4),
                    ]
                )
            )
            contenido_crisis.append(Spacer(1, 3))
            contenido_crisis.append(tabla_lineas)

        tabla_crisis_contenedor = Table(
            [[contenido_crisis]],
            colWidths=[ancho_util],
        )
        tabla_crisis_contenedor.setStyle(
            TableStyle(
                [
                    ("BACKGROUND", (0, 0), (-1, -1), colors.HexColor("#fff5f5")),
                    ("BOX", (0, 0), (-1, -1), 1, colors.HexColor("#ef4444")),
                    ("TOPPADDING", (0, 0), (-1, -1), 4),
                    ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
                    ("LEFTPADDING", (0, 0), (-1, -1), 6),
                    ("RIGHTPADDING", (0, 0), (-1, -1), 6),
                ]
            )
        )
        elementos.append(tabla_crisis_contenedor)
        elementos.append(Spacer(1, 4))

    # 6. Como se interpretan los puntajes (escala de respuesta + puntos de corte)
    seccion_dos = []
    seccion_dos.append(Paragraph("2. Como se Responden y se Interpretan los Cuestionarios", estilo_encabezado_seccion))
    seccion_dos.append(Paragraph(TEXTO_COMO_RESPONDER, estilo_texto_parrafo))

    filas_escala = [[Paragraph(f"<b>{etiqueta}</b> = {puntos} punto(s)", estilo_tabla_celda) for etiqueta, puntos in ESCALA_RESPUESTA]]
    tabla_escala = Table(filas_escala, colWidths=[ancho_util / 4] * 4)
    tabla_escala.setStyle(
        TableStyle(
            [
                ("BACKGROUND", (0, 0), (-1, -1), colors.HexColor("#f8fafc")),
                ("GRID", (0, 0), (-1, -1), 0.5, colors.HexColor("#cbd5e1")),
                ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
                ("TOPPADDING", (0, 0), (-1, -1), 3),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 3),
            ]
        )
    )
    seccion_dos.append(tabla_escala)
    seccion_dos.append(Spacer(1, 5))

    filas_niveles = [
        [
            Paragraph("Nivel", estilo_tabla_encabezado),
            Paragraph("PHQ-9 (0 a 27)", estilo_tabla_encabezado),
            Paragraph("GAD-7 (0 a 21)", estilo_tabla_encabezado),
            Paragraph("Que se suele sugerir (orientativo)", estilo_tabla_encabezado),
        ]
    ]
    claves_niveles = ["MINIMA", "LEVE", "MODERADA", "MODERADAMENTE_SEVERA", "SEVERA"]
    for (nivel, rango_phq9, rango_gad7, sugerencia), clave_nivel in zip(TABLA_NIVELES, claves_niveles):
        filas_niveles.append(
            [
                Paragraph(f"<b>{nivel}</b>", estilo_tabla_celda_destacada),
                Paragraph(rango_phq9, estilo_tabla_celda),
                Paragraph(rango_gad7, estilo_tabla_celda),
                Paragraph(sugerencia, estilo_tabla_celda),
            ]
        )
    tabla_niveles = Table(
        filas_niveles,
        colWidths=[ancho_util * 0.19, ancho_util * 0.14, ancho_util * 0.14, ancho_util * 0.53],
        repeatRows=1,
    )
    estilo_niveles = [
        ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#334155")),
        ("GRID", (0, 0), (-1, -1), 0.5, colors.HexColor("#cbd5e1")),
        ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
        ("TOPPADDING", (0, 0), (-1, -1), 2.5),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 2.5),
        ("LEFTPADDING", (0, 0), (-1, -1), 5),
        ("RIGHTPADDING", (0, 0), (-1, -1), 5),
    ]
    # La columna de nivel toma el color claro de su franja, igual que en el grafico
    for posicion_fila, clave_nivel in enumerate(claves_niveles, start=1):
        estilo_niveles.append(("BACKGROUND", (0, posicion_fila), (0, posicion_fila), colors.HexColor(PALETA_NIVELES[clave_nivel][0])))
    tabla_niveles.setStyle(TableStyle(estilo_niveles))
    seccion_dos.append(tabla_niveles)
    seccion_dos.append(Spacer(1, 3))
    seccion_dos.append(Paragraph(TEXTO_NIVELES_PUNTAJE, estilo_texto_parrafo))
    seccion_dos.append(Paragraph(TEXTO_NOTA_PREGUNTA_NUEVE, estilo_texto_parrafo))
    seccion_dos.append(Spacer(1, 4))

    elementos.append(KeepTogether(seccion_dos))

    # 7. Explicacion de cada pregunta (PHQ-9 y GAD-7) con la frecuencia que marco la persona
    respuestas_por_instrumento = asociar_respuestas(respuestas_resumen)

    def construir_tabla_preguntas(
        catalogo: List[Dict[str, Any]],
        respuestas: Dict[int, str],
        resaltar_item_nueve: bool,
        es_phq9: bool = False,
    ):
        filas = [
            [
                Paragraph("Pregunta del cuestionario", estilo_tabla_encabezado),
                Paragraph("Que busca explorar", estilo_tabla_encabezado),
                Paragraph("Tu respuesta y que significa", estilo_tabla_encabezado),
            ]
        ]
        for item in catalogo:
            frecuencia_item = respuestas.get(item["numero"], "No disponible")
            es_nueve = es_phq9 and item["numero"] == 9
            interpretacion = interpretar_respuesta(respuestas.get(item["numero"]), es_nueve)
            filas.append(
                [
                    Paragraph(f"<b>{item['numero']}.</b> {item['pregunta']}", estilo_tabla_celda),
                    Paragraph(item["explicacion"], estilo_tabla_celda),
                    Paragraph(f"<b>{frecuencia_item}</b><br/>{interpretacion}", estilo_tabla_celda),
                ]
            )
        tabla = Table(filas, colWidths=[ancho_util * 0.24, ancho_util * 0.42, ancho_util * 0.34], repeatRows=1)
        estilo_tabla = [
            ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#334155")),
            ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.HexColor("#f8fafc"), colors.white]),
            ("GRID", (0, 0), (-1, -1), 0.5, colors.HexColor("#cbd5e1")),
            ("VALIGN", (0, 0), (-1, -1), "TOP"),
            ("TOPPADDING", (0, 0), (-1, -1), 3),
            ("BOTTOMPADDING", (0, 0), (-1, -1), 3),
            ("LEFTPADDING", (0, 0), (-1, -1), 5),
            ("RIGHTPADDING", (0, 0), (-1, -1), 5),
        ]
        if resaltar_item_nueve:
            # La fila de la pregunta 9 se resalta suavemente cuando existe aviso de crisis
            estilo_tabla.append(("BACKGROUND", (0, len(catalogo)), (-1, len(catalogo)), colors.HexColor("#fff1f2")))
        tabla.setStyle(TableStyle(estilo_tabla))
        return tabla

    elementos.append(CondPageBreak(160))
    elementos.append(Paragraph("3. Que Significa Cada Pregunta: PHQ-9 (Animo y Vitalidad)", estilo_encabezado_seccion))
    elementos.append(Paragraph(TEXTO_INTRO_PHQ9, estilo_texto_parrafo))
    elementos.append(construir_tabla_preguntas(ITEMS_PHQ9, respuestas_por_instrumento["PHQ9"], riesgo_crisis, es_phq9=True))
    elementos.append(Spacer(1, 8))

    # La seccion del GAD-7 se mantiene completa en una sola pagina (7 preguntas) para no dejar filas huerfanas
    elementos.append(
        KeepTogether(
            [
                Paragraph("4. Que Significa Cada Pregunta: GAD-7 (Tension y Ansiedad)", estilo_encabezado_seccion),
                Paragraph(TEXTO_INTRO_GAD7, estilo_texto_parrafo),
                construir_tabla_preguntas(ITEMS_GAD7, respuestas_por_instrumento["GAD7"], False),
            ]
        )
    )
    elementos.append(Spacer(1, 8))

    # =========================================================================
    # ORIENTACION INSTITUCIONAL, PSICOEDUCACION, MARCO DEL CRIBADO Y REFERENCIAS
    # =========================================================================

    elementos.append(CondPageBreak(200))
    elementos.append(Paragraph("5. Orientacion y Recomendaciones Institucionales", estilo_encabezado_seccion))

    if resultado_minimo and not riesgo_crisis:
        # Texto de resultado minimo en lugar de orientacion por niveles
        tabla_minimo = Table(
            [
                [
                    Paragraph("<b>Resultado de Cribado en Nivel Minimo</b>", estilo_tabla_celda_destacada),
                ],
                [
                    Paragraph(TEXTO_RESULTADO_MINIMO, estilo_texto_parrafo),
                ],
            ],
            colWidths=[ancho_util],
        )
        tabla_minimo.setStyle(
            TableStyle(
                [
                    ("BACKGROUND", (0, 0), (-1, -1), colors.HexColor("#f0fdf4")),
                    ("BOX", (0, 0), (-1, -1), 1, colors.HexColor("#86efac")),
                    ("TOPPADDING", (0, 0), (-1, -1), 6),
                    ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
                    ("LEFTPADDING", (0, 0), (-1, -1), 8),
                    ("RIGHTPADDING", (0, 0), (-1, -1), 8),
                ]
            )
        )
        elementos.append(tabla_minimo)
    else:
        # Orientacion institucional por niveles (medicina general, psicologia, EPS)
        texto_orientacion = obtener_orientacion_institucional(severidad_depresion, severidad_ansiedad)
        parrafos_orientacion = [
            Paragraph("<b>Pautas de atencion y acompanamiento profesional segun niveles observados:</b>", estilo_tabla_celda_destacada),
            Spacer(1, 3),
        ]
        for linea_texto in texto_orientacion.split("\n\n"):
            parrafos_orientacion.append(Paragraph(linea_texto, estilo_texto_parrafo))
            parrafos_orientacion.append(Spacer(1, 2))

        parrafos_orientacion.append(
            Paragraph(
                "<b>Ruta institucional de atencion recomendada:</b> Puedes solicitar cita de valoracion inicial "
                "por medicina general en tu entidad promotora de salud (EPS) o bienestar universitario para gestionar "
                "la remision a psicologia, o consultar los servicios de salud comunitarios de tu municipio.",
                estilo_texto_parrafo,
            )
        )

        tabla_orientacion = Table([[parrafos_orientacion]], colWidths=[ancho_util])
        tabla_orientacion.setStyle(
            TableStyle(
                [
                    ("BACKGROUND", (0, 0), (-1, -1), colors.HexColor("#f8fafc")),
                    ("BOX", (0, 0), (-1, -1), 1, colors.HexColor("#cbd5e1")),
                    ("TOPPADDING", (0, 0), (-1, -1), 6),
                    ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
                    ("LEFTPADDING", (0, 0), (-1, -1), 8),
                    ("RIGHTPADDING", (0, 0), (-1, -1), 8),
                ]
            )
        )
        elementos.append(tabla_orientacion)

    elementos.append(Spacer(1, 8))

    # 4. Psicoeducacion general y neutral (pluralismo terapeutico sin derivar juicios del puntaje)
    elementos.append(Paragraph("6. Psicoeducacion General y Perspectivas de Acompanamiento", estilo_encabezado_seccion))
    elementos.append(Paragraph(TEXTO_PSICOEDUCACION_NEUTRAL, estilo_texto_parrafo))
    elementos.append(Spacer(1, 8))

    # 5. Seccion "Sobre este cribado" (PHQ-9, GAD-7, marco de 2 semanas y limites del sistema)
    elementos.append(Paragraph("7. Sobre este Cribado (Alcances, Metodologia y Limites)", estilo_encabezado_seccion))
    elementos.append(Paragraph(TEXTO_SOBRE_ESTE_CRIBADO, estilo_texto_parrafo))

    # Lista con viñetas de aclaraciones operativas
    elementos.append(
        Paragraph(
            "• <b>Marco temporal:</b> Las preguntas evaluan de manera exclusiva las manifestaciones experimentadas en las ultimas dos semanas.<br/>"
            "• <b>No es un diagnostico:</b> Unicamente un profesional de la salud debidamente acreditado puede diagnosticar despues de una evaluacion clinica integral.<br/>"
            "• <b>No reemplaza servicios de emergencia:</b> Ante urgencias de salud mental o riesgo vital, debe acudirse de inmediato a una institucion hospitalaria.<br/>"
            "• <b>Autonomia del usuario:</b> Los resultados presentados son de caracter informativo y no son compartidos con terceros sin tu autorizacion expresa.",
            estilo_texto_parrafo,
        )
    )

    elementos.append(Paragraph(TEXTO_ORIGEN_Y_VALIDACION, estilo_texto_parrafo))
    elementos.append(Spacer(1, 10))

    # Nota informativa fija al pie (obligatoria en todos los reportes)
    tabla_nota_pie = Table(
        [
            [
                Paragraph(
                    f"<b>Nota informativa:</b> {TEXTO_NOTA_INFORMATIVA_PIE}",
                    estilo_nota_pie_recuadro,
                )
            ]
        ],
        colWidths=[ancho_util],
    )
    tabla_nota_pie.setStyle(
        TableStyle(
            [
                ("BACKGROUND", (0, 0), (-1, -1), colors.HexColor("#f1f5f9")),
                ("BOX", (0, 0), (-1, -1), 0.8, colors.HexColor("#94a3b8")),
                ("TOPPADDING", (0, 0), (-1, -1), 6),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
                ("LEFTPADDING", (0, 0), (-1, -1), 8),
                ("RIGHTPADDING", (0, 0), (-1, -1), 8),
            ]
        )
    )
    elementos.append(tabla_nota_pie)

    # Referencias bibliograficas
    estilo_referencia = ParagraphStyle(
        "Referencia",
        parent=hoja_estilos["Normal"],
        fontName="Helvetica",
        fontSize=7,
        leading=9.5,
        textColor=colors.HexColor("#334155"),
        leftIndent=14,
        firstLineIndent=-14,
        spaceAfter=2,
    )
    bloque_referencias = [Paragraph("8. Referencias Bibliograficas", estilo_encabezado_seccion)]
    for posicion, referencia in enumerate(REFERENCIAS, start=1):
        bloque_referencias.append(Paragraph(f"[{posicion}] {referencia}", estilo_referencia))
    elementos.append(Spacer(1, 8))
    elementos.append(KeepTogether(bloque_referencias))

    # Construccion del documento con lienzo numerador en memoria
    documento.build(elementos, canvasmaker=NumeradorPaginas)

    flujo_pdf.seek(0)
    return flujo_pdf.getvalue()
