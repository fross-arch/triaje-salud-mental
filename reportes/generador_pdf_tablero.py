"""Generador de informe PDF para el Tablero Estadistico Administrativo.

Genera un documento PDF estructurado en memoria usando ReportLab,
incorporando indicadores agregados, tablas de resumen y los 8 graficos renderizados en base64.
Aplica estricta privacidad: no incluye ningun dato personal individual.
"""

import base64
import io
from typing import Any, Dict, List, Optional

from reportlab.lib import colors
from reportlab.lib.pagesizes import letter
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.utils import ImageReader
from reportlab.pdfgen import canvas
from reportlab.platypus import (
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


class NumeradorPaginasTablero(canvas.Canvas):
    """Numerador de páginas de dos pasadas para calcular 'Página X de Y'."""

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

        self.setStrokeColor(colors.HexColor("#cbd5e1"))
        self.setLineWidth(0.6)
        self.line(40, 30, ancho - 40, 30)

        self.setFont("Helvetica", 7.5)
        self.setFillColor(colors.HexColor("#64748b"))
        self.drawString(
            40,
            20,
            "Sistema de Triaje de Salud Mental  |  Informe Estadístico Agregado y Confidencial",
        )

        texto_pagina = f"Página {self._pageNumber} de {total_paginas}"
        self.drawRightString(ancho - 40, 20, texto_pagina)
        self.restoreState()


def _crear_imagen_flowable(base64_str: Optional[str], ancho: float = 240, alto: float = 125) -> Optional[Image]:
    """Decodifica un string base64 PNG y retorna un Flowable Image de ReportLab."""
    if not base64_str:
        return None
    try:
        img_bytes = base64.b64decode(base64_str)
        img_buffer = io.BytesIO(img_bytes)
        return Image(img_buffer, width=ancho, height=alto)
    except Exception:
        return None


def construir_pdf_tablero(datos: Dict[str, Any], dias: int = 30, fecha_reporte: str = "") -> bytes:
    """Construye el PDF consolidado del tablero estadístico administrativo.

    Parámetros:
        datos: Diccionario resultante de construir_tablero (incluyendo indicadores, actividad y graficos).
        dias: Periodo analizado (0 = histórico completo, 7, 30 o 90 días).
        fecha_reporte: Fecha legible de generación del informe.
    """
    buffer = io.BytesIO()
    doc = SimpleDocTemplate(
        buffer,
        pagesize=letter,
        leftMargin=36,
        rightMargin=36,
        topMargin=36,
        bottomMargin=45,
    )

    estilos = getSampleStyleSheet()

    estilo_titulo = ParagraphStyle(
        "TituloTablero",
        parent=estilos["Heading1"],
        fontName="Helvetica-Bold",
        fontSize=17,
        leading=21,
        textColor=colors.HexColor("#0f172a"),
        spaceAfter=3,
    )

    estilo_subtitulo = ParagraphStyle(
        "SubtituloTablero",
        parent=estilos["Normal"],
        fontName="Helvetica",
        fontSize=9.5,
        leading=13,
        textColor=colors.HexColor("#475569"),
        spaceAfter=6,
    )

    estilo_seccion = ParagraphStyle(
        "SeccionTablero",
        parent=estilos["Heading2"],
        fontName="Helvetica-Bold",
        fontSize=12,
        leading=15,
        textColor=colors.HexColor("#1e3a8a"),
        spaceBefore=8,
        spaceAfter=5,
    )

    estilo_celda = ParagraphStyle(
        "CeldaTexto",
        parent=estilos["Normal"],
        fontName="Helvetica",
        fontSize=8,
        leading=10,
        textColor=colors.HexColor("#1e293b"),
    )

    estilo_celda_bold = ParagraphStyle(
        "CeldaTextoBold",
        parent=estilos["Normal"],
        fontName="Helvetica-Bold",
        fontSize=8,
        leading=10,
        textColor=colors.HexColor("#0f172a"),
    )

    estilo_pie_nota = ParagraphStyle(
        "PieNotaPrivacidad",
        parent=estilos["Normal"],
        fontName="Helvetica-Oblique",
        fontSize=7.5,
        leading=10,
        textColor=colors.HexColor("#64748b"),
        spaceBefore=6,
    )

    historia = []

    # Encabezado
    periodo_texto = "Histórico completo" if dias == 0 else f"Últimos {dias} días"
    historia.append(Paragraph("INFORME EJECUTIVO DEL TABLERO ESTADÍSTICO", estilo_titulo))
    historia.append(Paragraph(
        f"Periodo de análisis: <b>{periodo_texto}</b> &nbsp;|&nbsp; Fecha de generación: <b>{fecha_reporte}</b>",
        estilo_subtitulo,
    ))

    es_demo = datos.get("es_demostracion", False)
    if es_demo:
        banner_demo = Table(
            [[Paragraph("<font color='#000000'><b>DATOS DE DEMOSTRACIÓN — Cifras sintéticas sin vinculación a personas reales</b></font>", estilo_celda_bold)]],
            colWidths=[540],
        )
        banner_demo.setStyle(TableStyle([
            ("BACKGROUND", (0, 0), (-1, -1), colors.HexColor("#fbbf24")),
            ("ALIGN", (0, 0), (-1, -1), "CENTER"),
            ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
            ("TOPPADDING", (0, 0), (-1, -1), 4),
            ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
            ("LEFTPADDING", (0, 0), (-1, -1), 10),
            ("RIGHTPADDING", (0, 0), (-1, -1), 10),
            ("BOX", (0, 0), (-1, -1), 1.2, colors.HexColor("#d97706")),
        ]))
        historia.append(banner_demo)
        historia.append(Spacer(1, 6))

    historia.append(HRFlowable(width="100%", thickness=1, color=colors.HexColor("#cbd5e1"), spaceBefore=2, spaceAfter=8))

    indicadores = datos.get("indicadores") or {}
    minimo_grupo = datos.get("minimo_requerido", 5)

    # 1. SECCIÓN: INDICADORES PSICOMÉTRICOS Y SALUD MENTAL
    historia.append(Paragraph("1. Indicadores de Cribado Clínico (PHQ-9 y GAD-7)", estilo_seccion))

    filas_ind = [
        [
            Paragraph("Total Evaluaciones Completadas", estilo_celda_bold),
            Paragraph(str(indicadores.get("total_evaluaciones", "0")), estilo_celda),
            Paragraph("Correlación PHQ-9 vs GAD-7", estilo_celda_bold),
            Paragraph(str(indicadores.get("correlacion_phq9_gad7") or "N/D"), estilo_celda),
        ],
        [
            Paragraph("PHQ-9 Depresión (Media ± Desv.)", estilo_celda_bold),
            Paragraph(f"{indicadores.get('media_phq9', 'N/D')} ± {indicadores.get('desviacion_phq9', 'N/D')}", estilo_celda),
            Paragraph("PHQ-9 Mediana", estilo_celda_bold),
            Paragraph(str(indicadores.get("mediana_phq9", "N/D")), estilo_celda),
        ],
        [
            Paragraph("GAD-7 Ansiedad (Media ± Desv.)", estilo_celda_bold),
            Paragraph(f"{indicadores.get('media_gad7', 'N/D')} ± {indicadores.get('desviacion_gad7', 'N/D')}", estilo_celda),
            Paragraph("GAD-7 Mediana", estilo_celda_bold),
            Paragraph(str(indicadores.get("mediana_gad7", "N/D")), estilo_celda),
        ],
        [
            Paragraph("Riesgo de Crisis Reportado", estilo_celda_bold),
            Paragraph(f"{indicadores.get('total_crisis', '0')} ({indicadores.get('porcentaje_crisis', 'N/D')}%)", estilo_celda),
            Paragraph("Sin Riesgo de Crisis", estilo_celda_bold),
            Paragraph(str(indicadores.get("total_sin_crisis", "0")), estilo_celda),
        ],
    ]

    tabla_ind = Table(filas_ind, colWidths=[150, 120, 150, 120])
    tabla_ind.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, -1), colors.HexColor("#f8fafc")),
        ("GRID", (0, 0), (-1, -1), 0.5, colors.HexColor("#e2e8f0")),
        ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
        ("TOPPADDING", (0, 0), (-1, -1), 3),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 3),
    ]))
    historia.append(tabla_ind)
    historia.append(Spacer(1, 8))

    # Gráficos Psicometricos (2x2)
    graficos = datos.get("graficos") or {}
    img_phq9 = _crear_imagen_flowable(graficos.get("severidad_phq9"), ancho=265, alto=125)
    img_gad7 = _crear_imagen_flowable(graficos.get("severidad_gad7"), ancho=265, alto=125)
    img_semanas = _crear_imagen_flowable(graficos.get("evaluaciones_semana"), ancho=265, alto=125)
    img_matriz = _crear_imagen_flowable(graficos.get("phq9_vs_gad7"), ancho=265, alto=125)

    if img_phq9 and img_gad7:
        tabla_g1 = Table([[img_phq9, img_gad7]], colWidths=[270, 270])
        tabla_g1.setStyle(TableStyle([("ALIGN", (0, 0), (-1, -1), "CENTER"), ("VALIGN", (0, 0), (-1, -1), "MIDDLE")]))
        historia.append(tabla_g1)
        historia.append(Spacer(1, 4))

    if img_semanas and img_matriz:
        tabla_g2 = Table([[img_semanas, img_matriz]], colWidths=[270, 270])
        tabla_g2.setStyle(TableStyle([("ALIGN", (0, 0), (-1, -1), "CENTER"), ("VALIGN", (0, 0), (-1, -1), "MIDDLE")]))
        historia.append(tabla_g2)
        historia.append(Spacer(1, 6))

    # Salto de página para la sección de Actividad
    historia.append(PageBreak())

    # 2. SECCIÓN: ACTIVIDAD DEL SISTEMA
    historia.append(Paragraph("2. Actividad del Sistema", estilo_seccion))

    actividad = datos.get("actividad") or {}
    act_ind = actividad.get("indicadores") or {}

    filas_act = [
        [
            Paragraph("Pacientes Registrados (distintos)", estilo_celda_bold),
            Paragraph(str(act_ind.get("total_pacientes_registrados", "0")), estilo_celda),
            Paragraph("Citas Propuestas (por clínico)", estilo_celda_bold),
            Paragraph(str(act_ind.get("citas_propuestas", "0")), estilo_celda),
        ],
        [
            Paragraph("Pacientes con Evaluaciones Completadas", estilo_celda_bold),
            Paragraph(str(act_ind.get("total_pacientes_con_evaluacion", "0")), estilo_celda),
            Paragraph("Citas Confirmadas (por paciente)", estilo_celda_bold),
            Paragraph(str(act_ind.get("citas_confirmadas", "0")), estilo_celda),
        ],
        [
            Paragraph("Pacientes con Cita Confirmada", estilo_celda_bold),
            Paragraph(str(act_ind.get("total_pacientes_con_cita_confirmada", "0")), estilo_celda),
            Paragraph("Citas Rechazadas (por paciente)", estilo_celda_bold),
            Paragraph(str(act_ind.get("citas_rechazadas", "0")), estilo_celda),
        ],
        [
            Paragraph("Altas de Profesionales Clínicos", estilo_celda_bold),
            Paragraph(str(act_ind.get("clinicos_altas", "0")), estilo_celda),
            Paragraph("Citas Cumplidas", estilo_celda_bold),
            Paragraph(str(act_ind.get("citas_cumplidas", "0")), estilo_celda),
        ],
        [
            Paragraph("Activaciones / Desactivaciones de Clínicos", estilo_celda_bold),
            Paragraph(f"{act_ind.get('clinicos_activados', 0)} activ. / {act_ind.get('clinicos_desactivados', 0)} desact.", estilo_celda),
            Paragraph("Citas Canceladas / Pospuestas", estilo_celda_bold),
            Paragraph(f"{act_ind.get('citas_canceladas', 0)} canc. / {act_ind.get('citas_pospuestas', 0)} posp.", estilo_celda),
        ],
    ]

    tabla_act = Table(filas_act, colWidths=[160, 110, 160, 110])
    tabla_act.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, -1), colors.HexColor("#f8fafc")),
        ("GRID", (0, 0), (-1, -1), 0.5, colors.HexColor("#e2e8f0")),
        ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
        ("TOPPADDING", (0, 0), (-1, -1), 3),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 3),
    ]))
    historia.append(tabla_act)
    historia.append(Spacer(1, 8))

    # Gráficos de Actividad (2x2)
    act_graf = actividad.get("graficos") or {}
    img_act_semanal = _crear_imagen_flowable(act_graf.get("actividad_semanal_pacientes"), ancho=265, alto=125)
    img_estado_citas = _crear_imagen_flowable(act_graf.get("estado_citas"), ancho=265, alto=125)
    img_gestion_clinicos = _crear_imagen_flowable(act_graf.get("gestion_clinicos"), ancho=265, alto=125)
    img_comparativa = _crear_imagen_flowable(act_graf.get("comparativa_pacientes"), ancho=265, alto=125)

    if img_act_semanal and img_estado_citas:
        tabla_ag1 = Table([[img_act_semanal, img_estado_citas]], colWidths=[270, 270])
        tabla_ag1.setStyle(TableStyle([("ALIGN", (0, 0), (-1, -1), "CENTER"), ("VALIGN", (0, 0), (-1, -1), "MIDDLE")]))
        historia.append(tabla_ag1)
        historia.append(Spacer(1, 4))

    if img_gestion_clinicos and img_comparativa:
        tabla_ag2 = Table([[img_gestion_clinicos, img_comparativa]], colWidths=[270, 270])
        tabla_ag2.setStyle(TableStyle([("ALIGN", (0, 0), (-1, -1), "CENTER"), ("VALIGN", (0, 0), (-1, -1), "MIDDLE")]))
        historia.append(tabla_ag2)
        historia.append(Spacer(1, 6))

    # Nota de Privacidad al Pie
    historia.append(Paragraph(
        f"<b>Nota de Privacidad y Confidencialidad:</b> Todas las métricas de pacientes han sido agregadas aplicando un umbral mínimo de grupo "
        f"N={minimo_grupo} y reglas de supresión complementaria para prevenir la deducción residual de casos individuales. "
        f"Las métricas de gestión de personal clínico son conteos agregados exactos sin desglose individual. Este documento no contiene "
        f"identificadores personales, listas nominales ni resultados de pruebas vinculados a personas.",
        estilo_pie_nota,
    ))

    doc.build(historia, canvasmaker=NumeradorPaginasTablero)
    buffer.seek(0)
    return buffer.getvalue()
