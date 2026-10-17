"""Modulo para la generacion en memoria de graficos comparativos de puntajes psicometricos.

Reglas del proyecto:
- Todo en memoria (BytesIO), sin persistencia ni archivos temporales en disco.
- Todos los identificadores en espanol, sin tildes ni enies.
- Enfoque sobrio y orientativo, sin categorias diagnosticas.
- El color de la barra de resultado corresponde a la franja de severidad en la que cae el puntaje.
"""

import io
from typing import List, Tuple

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

# Paleta por nivel: (color claro de la franja, color intenso para barra/etiquetas)
PALETA_NIVELES = {
    "MINIMA": ("#d1fae5", "#059669"),
    "LEVE": ("#e0f2fe", "#0284c7"),
    "MODERADA": ("#fef3c7", "#d97706"),
    "MODERADAMENTE_SEVERA": ("#ffedd5", "#ea580c"),
    "SEVERA": ("#fee2e2", "#dc2626"),
}

# (inicio, fin, clave de nivel, etiqueta corta, rango en texto)
FRANJAS_PHQ9: List[Tuple[int, int, str, str, str]] = [
    (0, 4, "MINIMA", "Minima", "0-4"),
    (4, 9, "LEVE", "Leve", "5-9"),
    (9, 14, "MODERADA", "Moderada", "10-14"),
    (14, 19, "MODERADAMENTE_SEVERA", "Mod. severa", "15-19"),
    (19, 27, "SEVERA", "Severa", "20-27"),
]

FRANJAS_GAD7: List[Tuple[int, int, str, str, str]] = [
    (0, 4, "MINIMA", "Minima", "0-4"),
    (4, 9, "LEVE", "Leve", "5-9"),
    (9, 14, "MODERADA", "Moderada", "10-14"),
    (14, 21, "SEVERA", "Severa", "15-21"),
]


def nivel_por_puntaje(puntaje: int, franjas: List[Tuple[int, int, str, str, str]]) -> str:
    """Retorna la clave de nivel cuya franja contiene el puntaje."""
    for _, fin, clave, _, _ in franjas:
        if puntaje <= fin:
            return clave
    return franjas[-1][2]


def segmentos_barra(puntaje: int, franjas: List[Tuple[int, int, str, str, str]]) -> List[Tuple[int, int, str]]:
    """Divide la barra en tramos, uno por cada franja que el puntaje ya alcanzo o atraveso.

    Ejemplo (PHQ-9, puntaje 12): [(0, 4, "MINIMA"), (4, 9, "LEVE"), (9, 12, "MODERADA")].
    """
    tramos = []
    for inicio, fin, clave, _, _ in franjas:
        if puntaje > inicio:
            tramos.append((inicio, min(fin, puntaje), clave))
    return tramos


def _dibujar_instrumento(eje, titulo: str, puntaje: int, maximo: int, franjas) -> None:
    """Dibuja un instrumento: franjas de colores, etiquetas legibles y barra del color de su nivel."""
    puntaje_acotado = max(0, min(maximo, puntaje))

    for inicio, fin, clave, etiqueta, rango in franjas:
        color_franja, color_texto = PALETA_NIVELES[clave]
        eje.axvspan(inicio, fin, ymin=0, ymax=1, color=color_franja, alpha=0.9, zorder=1)
        eje.text(
            (inicio + fin) / 2,
            0.62,
            f"{etiqueta}\n({rango})",
            ha="center",
            va="center",
            fontsize=8.5,
            fontweight="bold",
            color=color_texto,
            linespacing=1.15,
            zorder=4,
        )

    # Barra del puntaje en tramos: cada tramo toma el color intenso de la franja que ya recorrio
    for inicio_tramo, fin_tramo, clave_tramo in segmentos_barra(puntaje_acotado, franjas):
        eje.barh(
            y=[0],
            left=[inicio_tramo],
            width=[fin_tramo - inicio_tramo],
            height=0.55,
            color=PALETA_NIVELES[clave_tramo][1],
            edgecolor="#1e293b",
            linewidth=0.7,
            zorder=3,
        )
    # Marcador vertical (visible incluso con puntaje 0) y valor numerico
    eje.plot([puntaje_acotado, puntaje_acotado], [-0.42, 0.42], color="#1e293b", linewidth=1.6, zorder=5)
    dentro_de_barra = puntaje_acotado >= maximo * 0.12
    eje.text(
        puntaje_acotado - 0.35 if dentro_de_barra else puntaje_acotado + 0.35,
        0,
        str(puntaje),
        ha="right" if dentro_de_barra else "left",
        va="center",
        fontsize=11,
        fontweight="bold",
        color="#ffffff" if dentro_de_barra else "#1e293b",
        zorder=6,
    )

    eje.set_xlim(0, maximo)
    eje.set_ylim(-0.5, 0.95)
    eje.set_yticks([])
    eje.set_title(titulo, fontsize=10.5, fontweight="bold", color="#0f172a", loc="left", pad=6)
    eje.set_xticks([0] + [fin for _, fin, _, _, _ in franjas])
    eje.tick_params(axis="x", labelsize=9, colors="#334155")
    eje.grid(axis="x", linestyle="--", alpha=0.45, color="#94a3b8", zorder=2)
    for borde in ["top", "right", "left"]:
        eje.spines[borde].set_visible(False)
    eje.spines["bottom"].set_color("#64748b")
    eje.spines["bottom"].set_linewidth(0.8)


def generar_grafico_umbrales(puntaje_phq9: int, puntaje_gad7: int) -> io.BytesIO:
    """Genera un grafico de barras horizontal comparando los puntajes contra los umbrales de severidad.

    Retorna un flujo de bytes en memoria (io.BytesIO) en formato PNG.
    """
    fig, (eje_phq9, eje_gad7) = plt.subplots(
        nrows=2,
        ncols=1,
        figsize=(7.2, 3.3),
        dpi=200,
        constrained_layout=True,
    )

    _dibujar_instrumento(
        eje_phq9, f"PHQ-9 (Animo y vitalidad)  |  Puntaje obtenido: {puntaje_phq9} / 27", puntaje_phq9, 27, FRANJAS_PHQ9
    )
    _dibujar_instrumento(
        eje_gad7, f"GAD-7 (Tension y ansiedad)  |  Puntaje obtenido: {puntaje_gad7} / 21", puntaje_gad7, 21, FRANJAS_GAD7
    )

    flujo_memoria = io.BytesIO()
    fig.savefig(
        flujo_memoria,
        format="png",
        dpi=200,
        bbox_inches="tight",
        facecolor="#ffffff",
        edgecolor="none",
    )
    plt.close(fig)
    flujo_memoria.seek(0)
    return flujo_memoria
