"""Modulo de generacion de tablero estadistico anonimizado y agregado.

Utiliza pandas para agregaciones, numpy para calculos metricos y
matplotlib (backend Agg) para renderizado en memoria de graficos en Base64.
Aplica de forma estricta las reglas de umbral minimo de grupo (supresion de celdas pequeñas / grupo minimo N)
y supresion complementaria para evitar la deduccion de casos individuales por resta.
"""

import base64
import io
from typing import Any, Dict, List, Optional, Union

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

# Orden psicometrico estandar
CATEGORIAS_PHQ9 = ["MINIMA", "LEVE", "MODERADA", "MODERADAMENTE_SEVERA", "SEVERA"]
CATEGORIAS_GAD7 = ["MINIMA", "LEVE", "MODERADA", "SEVERA"]

ETIQUETAS_PHQ9 = {
    "MINIMA": "Mínima (0-4)",
    "LEVE": "Leve (5-9)",
    "MODERADA": "Moderada (10-14)",
    "MODERADAMENTE_SEVERA": "Mod. severa (15-19)",
    "SEVERA": "Severa (20-27)",
    "OTROS": "Otros (< umbral)",
}

ETIQUETAS_GAD7 = {
    "MINIMA": "Mínima (0-4)",
    "LEVE": "Leve (5-9)",
    "MODERADA": "Moderada (10-14)",
    "SEVERA": "Severa (15-21)",
    "OTROS": "Otros (< umbral)",
}

# Paleta accesible de alto contraste
COLORES_NIVELES = {
    "MINIMA": "#059669",               # Esmeralda
    "LEVE": "#0284c7",                 # Azul
    "MODERADA": "#d97706",             # Ámbar
    "MODERADAMENTE_SEVERA": "#ea580c", # Naranja
    "SEVERA": "#dc2626",               # Rojo
    "OTROS": "#64748b",                # Gris pizarra
}


def _figura_a_base64(fig: plt.Figure) -> str:
    """Convierte una figura de matplotlib a un string en base64 sin persistir en disco."""
    buffer = io.BytesIO()
    try:
        fig.savefig(
            buffer,
            format="png",
            dpi=160,
            bbox_inches="tight",
            facecolor="#ffffff",
            edgecolor="none",
        )
        buffer.seek(0)
        return base64.b64encode(buffer.getvalue()).decode("utf-8")
    finally:
        plt.close(fig)
        buffer.close()


def _formatear_cifra_privacidad(conteo: int, minimo_grupo: int) -> str:
    """Aplica la regla de umbral minimo de grupo: si 0 < conteo < minimo_grupo, muestra '<N'."""
    if conteo == 0:
        return "0"
    if conteo < minimo_grupo:
        return f"<{minimo_grupo}"
    return str(conteo)


def _preparar_categorias_agrupadas(
    serie: pd.Series, categorias_ordenadas: List[str], minimo_grupo: int
) -> pd.DataFrame:
    """Agrupa categorias con menos de N casos en 'OTROS' aplicando supresion complementaria.

    Para evitar la 'deduccion por resta' (donde un atacante calcula el conteo de una unica
    categoria pequeña restando las categorias visibles del total general), si solo UNA
    categoria queda por debajo del umbral N o si 'OTROS' suma menos de N, se incorpora
    la siguiente categoria de menor conteo a 'OTROS' (supresion secundaria). Asi, 'OTROS'
    siempre agrega al menos dos categorias y evita la desanonimizacion residual.
    """
    conteos = serie.value_counts()
    categorias_visibles = []
    categorias_suprimidas = []
    conteo_otros = 0

    for cat in categorias_ordenadas:
        val = int(conteos.get(cat, 0))
        if val >= minimo_grupo:
            categorias_visibles.append((cat, val))
        elif val > 0:
            categorias_suprimidas.append((cat, val))
            conteo_otros += val

    # Si hay categorias suprimidas y solo hay 1 categoria en 'OTROS' (o conteo_otros < minimo_grupo)
    # y quedan al menos 2 categorias visibles, aplicamos supresion complementaria
    if categorias_suprimidas and (len(categorias_suprimidas) < 2 or conteo_otros < minimo_grupo):
        if len(categorias_visibles) > 1:
            # Ordenar las visibles por menor conteo para agrupar la mas pequeña en 'OTROS'
            categorias_visibles.sort(key=lambda x: x[1])
            categoria_complementaria = categorias_visibles.pop(0)
            categorias_suprimidas.append(categoria_complementaria)
            conteo_otros += categoria_complementaria[1]

    # Reconstruir en el orden psicometrico estandar
    filas = []
    mapa_visibles = dict(categorias_visibles)
    for cat in categorias_ordenadas:
        if cat in mapa_visibles:
            filas.append({"categoria": cat, "conteo": mapa_visibles[cat]})

    if conteo_otros > 0:
        filas.append({"categoria": "OTROS", "conteo": conteo_otros})

    df_resultado = pd.DataFrame(filas)
    if df_resultado.empty:
        df_resultado = pd.DataFrame(columns=["categoria", "conteo"])
    return df_resultado


def _graficar_barras_severidad(
    df_cat: pd.DataFrame,
    titulo: str,
    etiquetas_map: Dict[str, str],
    minimo_grupo: int,
) -> str:
    """Genera grafico de barras horizontal para distribucion de severidades."""
    fig, eje = plt.subplots(figsize=(6.5, 3.4), constrained_layout=True)

    if df_cat.empty:
        eje.text(0.5, 0.5, "Sin datos disponibles", ha="center", va="center", color="#64748b")
        eje.axis("off")
        return _figura_a_base64(fig)

    categorias = df_cat["categoria"].tolist()
    conteos = df_cat["conteo"].tolist()
    etiquetas = [etiquetas_map.get(c, c) for c in categorias]
    colores = [COLORES_NIVELES.get(c, "#64748b") for c in categorias]

    posiciones = np.arange(len(categorias))
    barras = eje.barh(posiciones, conteos, height=0.55, color=colores, edgecolor="#334155", linewidth=0.7)

    # Etiquetas de datos al final de cada barra
    max_x = max(conteos) if conteos else 10
    eje.set_xlim(0, max_x * 1.25 + 1)

    for i, barra in enumerate(barras):
        val = conteos[i]
        texto = _formatear_cifra_privacidad(val, minimo_grupo)
        eje.text(
            val + (max_x * 0.02) + 0.1,
            barra.get_y() + barra.get_height() / 2,
            texto,
            va="center",
            ha="left",
            fontsize=9.5,
            fontweight="bold",
            color="#1e293b",
        )

    eje.set_yticks(posiciones)
    eje.set_yticklabels(etiquetas, fontsize=9, color="#0f172a", fontweight="medium")
    eje.invert_yaxis()  # Para que el orden psicometrico comience arriba
    eje.set_xlabel("Número de evaluaciones", fontsize=9, color="#334155")
    eje.set_title(titulo, fontsize=10.5, fontweight="bold", color="#0f172a", loc="left", pad=10)
    eje.grid(axis="x", linestyle="--", alpha=0.5, color="#cbd5e1")
    for spine in ["top", "right", "left"]:
        eje.spines[spine].set_visible(False)
    eje.spines["bottom"].set_color("#94a3b8")

    return _figura_a_base64(fig)


def _graficar_evaluaciones_semanales(df: pd.DataFrame, minimo_grupo: int) -> str:
    """Genera grafico de linea temporal agrupando semanas consecutivas hasta alcanzar el umbral minimo N.

    No enmascara semanas sueltas con '<N' para evitar deduccion por resta.
    Agrupa semanas consecutivas hasta que cada bloque tenga al menos N evaluaciones;
    si la ultima agrupacion queda incompleta (< N), se une al bloque anterior.
    """
    fig, eje = plt.subplots(figsize=(7.0, 3.4), constrained_layout=True)

    df_tiempo = df.copy()
    df_tiempo["fecha_dt"] = pd.to_datetime(df_tiempo["fecha"])
    df_tiempo = df_tiempo.set_index("fecha_dt").sort_index()

    # Agrupación semanal iniciando en lunes y etiquetada con el lunes de inicio
    serie_semanal = df_tiempo.resample("W-MON", closed="left", label="left").size()

    # Agrupar semanas consecutivas en bloques con al menos minimo_grupo evaluaciones
    bloques = []
    conteo_acum = 0
    fecha_inicio_bloque = None
    fecha_fin_bloque = None

    for fecha, conteo in serie_semanal.items():
        if conteo == 0 and fecha_inicio_bloque is None:
            continue
        if fecha_inicio_bloque is None:
            fecha_inicio_bloque = fecha
        fecha_fin_bloque = fecha
        conteo_acum += int(conteo)

        if conteo_acum >= minimo_grupo:
            bloques.append({
                "inicio": fecha_inicio_bloque,
                "fin": fecha_fin_bloque,
                "conteo": conteo_acum,
            })
            conteo_acum = 0
            fecha_inicio_bloque = None
            fecha_fin_bloque = None

    # Si quedo un remanente menor al umbral al final
    if conteo_acum > 0:
        if bloques:
            # Unir la ultima agrupacion incompleta al bloque anterior
            bloques[-1]["fin"] = fecha_fin_bloque
            bloques[-1]["conteo"] += conteo_acum
        else:
            # Si solo hay un bloque acumulando todo
            bloques.append({
                "inicio": fecha_inicio_bloque,
                "fin": fecha_fin_bloque,
                "conteo": conteo_acum,
            })

    if not bloques:
        eje.text(0.5, 0.5, "Sin datos disponibles", ha="center", va="center", color="#64748b")
        eje.axis("off")
        return _figura_a_base64(fig)

    etiquetas_x = []
    valores_y = []
    for b in bloques:
        inicio_str = b["inicio"].strftime("%d/%m")
        fin_str = b["fin"].strftime("%d/%m")
        etiqueta = inicio_str if inicio_str == fin_str else f"{inicio_str}-{fin_str}"
        etiquetas_x.append(etiqueta)
        valores_y.append(b["conteo"])

    x = np.arange(len(etiquetas_x))
    eje.plot(
        x,
        valores_y,
        marker="o",
        markersize=6,
        color="#3b82f6",
        linewidth=2.2,
        label="Evaluaciones completadas",
    )
    eje.fill_between(x, valores_y, color="#3b82f6", alpha=0.15)

    max_y = max(valores_y) if valores_y else 5
    eje.set_ylim(0, max_y * 1.25 + 1)

    for i, val in enumerate(valores_y):
        eje.text(
            x[i],
            val + (max_y * 0.04) + 0.2,
            str(val),
            ha="center",
            va="bottom",
            fontsize=8.5,
            fontweight="bold",
            color="#1e3a8a",
        )

    eje.set_xticks(x)
    if len(etiquetas_x) > 10:
        paso = len(etiquetas_x) // 8
        etiquetas_mostradas = [etiquetas_x[i] if i % paso == 0 else "" for i in range(len(etiquetas_x))]
        eje.set_xticklabels(etiquetas_mostradas, fontsize=8, color="#334155", rotation=30)
    else:
        eje.set_xticklabels(etiquetas_x, fontsize=8.5, color="#334155")

    eje.set_xlabel("Periodo semanal (inicio - fin)", fontsize=9, color="#334155")
    eje.set_ylabel("Evaluaciones", fontsize=9, color="#334155")
    eje.set_title("Evolución temporal por bloques semanales (mínimo N casos)", fontsize=10.5, fontweight="bold", color="#0f172a", loc="left", pad=10)
    eje.grid(axis="y", linestyle="--", alpha=0.5, color="#cbd5e1")
    for spine in ["top", "right"]:
        eje.spines[spine].set_visible(False)
    eje.spines["bottom"].set_color("#94a3b8")
    eje.spines["left"].set_color("#94a3b8")

    return _figura_a_base64(fig)


def _graficar_matriz_frecuencias(df: pd.DataFrame, minimo_grupo: int) -> str:
    """Genera matriz de frecuencias cruzadas PHQ-9 (depresion) vs GAD-7 (ansiedad).

    Aplica supresion primaria y complementaria de celdas para que ninguna cifra
    enmascarada (< N) pueda deducirse restando totales de fila, columna o general.
    """
    fig, eje = plt.subplots(figsize=(6.8, 3.8), constrained_layout=True)

    # Construccion de tabla cruzada completa
    tabla_cruzada = pd.crosstab(
        df["severidad_depresion"],
        df["severidad_ansiedad"],
        dropna=False,
    )

    # Reindexar para asegurar orden logico y todas las categorias
    tabla_cruzada = tabla_cruzada.reindex(
        index=CATEGORIAS_PHQ9,
        columns=CATEGORIAS_GAD7,
        fill_value=0,
    )

    matriz = tabla_cruzada.to_numpy()
    filas_n, cols_n = matriz.shape
    suprimidas = np.zeros_like(matriz, dtype=bool)

    # 1. Supresion primaria: celdas con 0 < valor < minimo_grupo
    for i in range(filas_n):
        for j in range(cols_n):
            if 0 < matriz[i, j] < minimo_grupo:
                suprimidas[i, j] = True

    # 2. Supresion complementaria por fila: evitar deduccion restando del total de fila
    for i in range(filas_n):
        if np.sum(suprimidas[i, :]) == 1:
            positivos = [(j, matriz[i, j]) for j in range(cols_n) if not suprimidas[i, j] and matriz[i, j] > 0]
            if positivos:
                positivos.sort(key=lambda x: x[1])
                suprimidas[i, positivos[0][0]] = True

    # 3. Supresion complementaria por columna: evitar deduccion restando del total de columna
    for j in range(cols_n):
        if np.sum(suprimidas[:, j]) == 1:
            positivos = [(i, matriz[i, j]) for i in range(filas_n) if not suprimidas[i, j] and matriz[i, j] > 0]
            if positivos:
                positivos.sort(key=lambda x: x[1])
                suprimidas[positivos[0][0], j] = True

    # Colormap accesible secuencial suave (YlGnBu)
    cmap = plt.cm.YlGnBu
    im = eje.imshow(matriz, cmap=cmap, aspect="auto")

    # Etiquetas de ejes
    eje.set_xticks(np.arange(len(CATEGORIAS_GAD7)))
    eje.set_yticks(np.arange(len(CATEGORIAS_PHQ9)))

    etiquetas_gad = ["Mínima", "Leve", "Moderada", "Severa"]
    etiquetas_phq = ["Mínima", "Leve", "Moderada", "Mod. sev.", "Severa"]

    eje.set_xticklabels(etiquetas_gad, fontsize=9, color="#0f172a")
    eje.set_yticklabels(etiquetas_phq, fontsize=9, color="#0f172a")

    eje.set_xlabel("Severidad Ansiedad (GAD-7)", fontsize=9.5, fontweight="bold", color="#334155", labelpad=8)
    eje.set_ylabel("Severidad Depresión (PHQ-9)", fontsize=9.5, fontweight="bold", color="#334155", labelpad=8)
    eje.set_title("Matriz de comorbilidad: PHQ-9 vs GAD-7", fontsize=10.5, fontweight="bold", color="#0f172a", loc="left", pad=10)

    # Anotar valores en cada celda respetando regla de privacidad y supresion
    umbral_contraste = matriz.max() / 2 if matriz.max() > 0 else 1
    for i in range(len(CATEGORIAS_PHQ9)):
        for j in range(len(CATEGORIAS_GAD7)):
            val = int(matriz[i, j])
            if suprimidas[i, j]:
                texto = f"<{minimo_grupo}"
            elif val == 0:
                texto = "0"
            else:
                texto = str(val)

            color_texto = "#ffffff" if val > umbral_contraste else "#0f172a"
            eje.text(
                j,
                i,
                texto,
                ha="center",
                va="center",
                color=color_texto,
                fontsize=9.5,
                fontweight="bold",
            )

    cbar = fig.colorbar(im, ax=eje, fraction=0.046, pad=0.04)
    cbar.ax.tick_params(labelsize=8)
    cbar.outline.set_visible(False)

    return _figura_a_base64(fig)
def _graficar_actividad_semanal_pacientes(eventos: List[Dict[str, Any]], minimo_grupo: int) -> str:
    """Genera grafico de evolucion temporal agrupando registros y evaluaciones de pacientes en bloques de al menos N."""
    fig, eje = plt.subplots(figsize=(7.0, 3.4), constrained_layout=True)

    eventos_pac = [
        e for e in eventos
        if e.get("tipo") in ("REGISTRO_PACIENTE", "PACIENTE_REGISTRADO", "EVALUACION_COMPLETADA")
    ]
    if not eventos_pac or len(eventos_pac) < minimo_grupo:
        eje.text(0.5, 0.5, "Sin datos disponibles o casos insuficientes (< N)", ha="center", va="center", color="#64748b", fontsize=9.5)
        eje.axis("off")
        return _figura_a_base64(fig)

    df = pd.DataFrame(eventos_pac)
    df["fecha_dt"] = pd.to_datetime(df["fecha"])
    df["es_registro"] = df["tipo"].isin(["REGISTRO_PACIENTE", "PACIENTE_REGISTRADO"]).astype(int)
    df["es_evaluacion"] = (df["tipo"] == "EVALUACION_COMPLETADA").astype(int)

    df = df.set_index("fecha_dt").sort_index()
    semanal = df.resample("W-MON", closed="left", label="left").agg({
        "es_registro": "sum",
        "es_evaluacion": "sum",
    })
    semanal["total"] = semanal["es_registro"] + semanal["es_evaluacion"]

    bloques = []
    conteo_acum = 0
    reg_acum = 0
    eval_acum = 0
    fecha_inicio_bloque = None
    fecha_fin_bloque = None

    for fecha, fila in semanal.iterrows():
        total_sem = int(fila["total"])
        if total_sem == 0 and fecha_inicio_bloque is None:
            continue
        if fecha_inicio_bloque is None:
            fecha_inicio_bloque = fecha
        fecha_fin_bloque = fecha
        conteo_acum += total_sem
        reg_acum += int(fila["es_registro"])
        eval_acum += int(fila["es_evaluacion"])

        if conteo_acum >= minimo_grupo:
            bloques.append({
                "inicio": fecha_inicio_bloque,
                "fin": fecha_fin_bloque,
                "registros": reg_acum,
                "evaluaciones": eval_acum,
                "total": conteo_acum,
            })
            conteo_acum = 0
            reg_acum = 0
            eval_acum = 0
            fecha_inicio_bloque = None
            fecha_fin_bloque = None

    if conteo_acum > 0:
        if bloques:
            bloques[-1]["fin"] = fecha_fin_bloque
            bloques[-1]["registros"] += reg_acum
            bloques[-1]["evaluaciones"] += eval_acum
            bloques[-1]["total"] += conteo_acum
        else:
            bloques.append({
                "inicio": fecha_inicio_bloque,
                "fin": fecha_fin_bloque,
                "registros": reg_acum,
                "evaluaciones": eval_acum,
                "total": conteo_acum,
            })

    if not bloques:
        eje.text(0.5, 0.5, "Sin datos disponibles", ha="center", va="center", color="#64748b")
        eje.axis("off")
        return _figura_a_base64(fig)

    etiquetas_x = []
    y_reg = []
    y_eval = []
    for b in bloques:
        i_str = b["inicio"].strftime("%d/%m")
        f_str = b["fin"].strftime("%d/%m")
        etiqueta = i_str if i_str == f_str else f"{i_str}-{f_str}"
        etiquetas_x.append(etiqueta)
        y_reg.append(b["registros"])
        y_eval.append(b["evaluaciones"])

    x = np.arange(len(etiquetas_x))
    ancho = 0.35
    eje.bar(x - ancho / 2, y_reg, width=ancho, label="Registros de pacientes", color="#059669", edgecolor="#065f46", linewidth=0.6)
    eje.bar(x + ancho / 2, y_eval, width=ancho, label="Evaluaciones completadas", color="#2563eb", edgecolor="#1e40af", linewidth=0.6)

    max_val = max(max(y_reg, default=0), max(y_eval, default=0))
    eje.set_ylim(0, max_val * 1.35 + 1)

    for i in range(len(x)):
        val_r = y_reg[i]
        val_e = y_eval[i]
        if val_r > 0:
            eje.text(x[i] - ancho / 2, val_r + 0.1, _formatear_cifra_privacidad(val_r, minimo_grupo), ha="center", va="bottom", fontsize=7.5, fontweight="bold", color="#065f46")
        if val_e > 0:
            eje.text(x[i] + ancho / 2, val_e + 0.1, _formatear_cifra_privacidad(val_e, minimo_grupo), ha="center", va="bottom", fontsize=7.5, fontweight="bold", color="#1e40af")

    eje.set_xticks(x)
    if len(etiquetas_x) > 8:
        paso = max(1, len(etiquetas_x) // 6)
        eje.set_xticklabels([etiquetas_x[i] if i % paso == 0 else "" for i in range(len(etiquetas_x))], fontsize=8, color="#334155", rotation=25)
    else:
        eje.set_xticklabels(etiquetas_x, fontsize=8.5, color="#334155")

    eje.set_xlabel("Periodo semanal (inicio - fin)", fontsize=9, color="#334155")
    eje.set_ylabel("Casos por bloque (≥ N)", fontsize=9, color="#334155")
    eje.set_title("Actividad semanal de pacientes (bloques ≥ N casos)", fontsize=10.5, fontweight="bold", color="#0f172a", loc="left", pad=10)
    eje.legend(fontsize=8, loc="upper right", frameon=False)
    eje.grid(axis="y", linestyle="--", alpha=0.5, color="#cbd5e1")
    for spine in ["top", "right"]:
        eje.spines[spine].set_visible(False)
    eje.spines["bottom"].set_color("#94a3b8")
    eje.spines["left"].set_color("#94a3b8")
    return _figura_a_base64(fig)


def _graficar_estado_citas(conteos: Dict[str, int], minimo_grupo: int) -> str:
    """Genera grafico de barras para el estado de las citas respetando el modelo (clinico propone, paciente confirma/rechaza)."""
    fig, eje = plt.subplots(figsize=(6.5, 3.4), constrained_layout=True)

    claves = ["CITA_PROPUESTA", "CITA_CONFIRMADA", "CITA_RECHAZADA", "CITA_CUMPLIDA", "CITA_CANCELADA", "CITA_POSPUESTA"]
    etiquetas = [
        "Propuestas (por clínico)",
        "Confirmadas (por paciente)",
        "Rechazadas (por paciente)",
        "Cumplidas",
        "Canceladas",
        "Pospuestas",
    ]
    colores = ["#3b82f6", "#10b981", "#f59e0b", "#8b5cf6", "#ef4444", "#64748b"]
    valores = [conteos.get(k, 0) for k in claves]

    if sum(valores) == 0:
        eje.text(0.5, 0.5, "Sin citas registradas", ha="center", va="center", color="#64748b", fontsize=9.5)
        eje.axis("off")
        return _figura_a_base64(fig)

    pos = np.arange(len(claves))
    barras = eje.barh(pos, valores, height=0.55, color=colores, edgecolor="#334155", linewidth=0.7)

    max_x = max(valores) if valores else 10
    eje.set_xlim(0, max_x * 1.35 + 1)

    for i, barra in enumerate(barras):
        val = valores[i]
        txt = _formatear_cifra_privacidad(val, minimo_grupo)
        eje.text(
            val + (max_x * 0.02) + 0.1,
            barra.get_y() + barra.get_height() / 2,
            txt,
            va="center",
            ha="left",
            fontsize=9.5,
            fontweight="bold",
            color="#1e293b",
        )

    eje.set_yticks(pos)
    eje.set_yticklabels(etiquetas, fontsize=9, color="#0f172a", fontweight="medium")
    eje.invert_yaxis()
    eje.set_xlabel("Número de citas", fontsize=9, color="#334155")
    eje.set_title("Estado de las Citas de Acompañamiento", fontsize=10.5, fontweight="bold", color="#0f172a", loc="left", pad=10)
    eje.grid(axis="x", linestyle="--", alpha=0.5, color="#cbd5e1")
    for spine in ["top", "right", "left"]:
        eje.spines[spine].set_visible(False)
    eje.spines["bottom"].set_color("#94a3b8")
    return _figura_a_base64(fig)


def _graficar_gestion_clinicos(conteos: Dict[str, int]) -> str:
    """Genera grafico de barras para la gestion del personal clinico (conteos exactos sin umbral de pacientes)."""
    fig, eje = plt.subplots(figsize=(6.5, 3.4), constrained_layout=True)

    claves = ["CLINICO_CREADO", "CLINICO_ACTIVADO", "CLINICO_DESACTIVADO", "CLINICO_ELIMINADO"]
    etiquetas = [
        "Altas de profesionales",
        "Cuentas activadas",
        "Cuentas desactivadas",
        "Cuentas eliminadas",
    ]
    colores = ["#0284c7", "#16a34a", "#d97706", "#dc2626"]
    valores = [conteos.get(k, 0) for k in claves]

    pos = np.arange(len(claves))
    barras = eje.barh(pos, valores, height=0.55, color=colores, edgecolor="#334155", linewidth=0.7)

    max_x = max(valores) if valores else 5
    eje.set_xlim(0, max_x * 1.35 + 1)

    for i, barra in enumerate(barras):
        val = valores[i]
        eje.text(
            val + (max_x * 0.02) + 0.1,
            barra.get_y() + barra.get_height() / 2,
            str(val),
            va="center",
            ha="left",
            fontsize=9.5,
            fontweight="bold",
            color="#1e293b",
        )

    eje.set_yticks(pos)
    eje.set_yticklabels(etiquetas, fontsize=9, color="#0f172a", fontweight="medium")
    eje.invert_yaxis()
    eje.set_xlabel("Número de eventos (gestión del personal)", fontsize=9, color="#334155")
    eje.set_title("Gestión de Profesionales Clínicos", fontsize=10.5, fontweight="bold", color="#0f172a", loc="left", pad=10)
    eje.grid(axis="x", linestyle="--", alpha=0.5, color="#cbd5e1")
    for spine in ["top", "right", "left"]:
        eje.spines[spine].set_visible(False)
    eje.spines["bottom"].set_color("#94a3b8")
    return _figura_a_base64(fig)


def _graficar_comparativa_pacientes(
    total_reg: int, total_eval: int, total_citas: int, minimo_grupo: int
) -> str:
    """Genera grafico comparativo de personas distintas en las 3 etapas clave del flujo."""
    fig, eje = plt.subplots(figsize=(6.5, 3.4), constrained_layout=True)

    etiquetas = [
        "Pacientes registrados",
        "Con evaluación completada",
        "Con cita confirmada",
    ]
    valores = [total_reg, total_eval, total_citas]
    colores = ["#0d9488", "#2563eb", "#7c3aed"]

    if sum(valores) == 0:
        eje.text(0.5, 0.5, "Sin pacientes registrados", ha="center", va="center", color="#64748b", fontsize=9.5)
        eje.axis("off")
        return _figura_a_base64(fig)

    pos = np.arange(len(etiquetas))
    barras = eje.bar(pos, valores, width=0.5, color=colores, edgecolor="#334155", linewidth=0.7)

    max_y = max(valores) if valores else 10
    eje.set_ylim(0, max_y * 1.35 + 1)

    for i, barra in enumerate(barras):
        val = valores[i]
        txt = _formatear_cifra_privacidad(val, minimo_grupo)
        eje.text(
            barra.get_x() + barra.get_width() / 2,
            val + (max_y * 0.02) + 0.1,
            txt,
            ha="center",
            va="bottom",
            fontsize=9.5,
            fontweight="bold",
            color="#1e293b",
        )

    eje.set_xticks(pos)
    eje.set_xticklabels(etiquetas, fontsize=9, color="#0f172a", fontweight="medium")
    eje.set_ylabel("Pacientes únicos (DISTINCT)", fontsize=9, color="#334155")
    eje.set_title("Pacientes en el Flujo de Atención (umbral ≥ N)", fontsize=10.5, fontweight="bold", color="#0f172a", loc="left", pad=10)
    eje.grid(axis="y", linestyle="--", alpha=0.5, color="#cbd5e1")
    for spine in ["top", "right"]:
        eje.spines[spine].set_visible(False)
    eje.spines["bottom"].set_color("#94a3b8")
    eje.spines["left"].set_color("#94a3b8")
    return _figura_a_base64(fig)


def _procesar_actividad(
    eventos: List[Dict[str, Any]],
    totales_personas: Dict[str, int],
    minimo_grupo: int,
) -> Dict[str, Any]:
    """Calcula indicadores agregados y genera los 4 graficos de la seccion de actividad del sistema."""
    conteo_eventos: Dict[str, int] = {}
    for ev in eventos:
        t = ev.get("tipo", "")
        conteo_eventos[t] = conteo_eventos.get(t, 0) + 1

    citas_propuestas = conteo_eventos.get("CITA_PROPUESTA", 0)
    citas_confirmadas = conteo_eventos.get("CITA_CONFIRMADA", 0)
    citas_rechazadas = conteo_eventos.get("CITA_RECHAZADA", 0)
    citas_canceladas = conteo_eventos.get("CITA_CANCELADA", 0)
    citas_cumplidas = conteo_eventos.get("CITA_CUMPLIDA", 0)
    citas_pospuestas = conteo_eventos.get("CITA_POSPUESTA", 0)

    clinicos_altas = conteo_eventos.get("CLINICO_CREADO", 0)
    clinicos_activados = conteo_eventos.get("CLINICO_ACTIVADO", 0)
    clinicos_desactivados = conteo_eventos.get("CLINICO_DESACTIVADO", 0)
    clinicos_eliminados = conteo_eventos.get("CLINICO_ELIMINADO", 0)

    consentimientos_aceptados = conteo_eventos.get("CONSENTIMIENTO_ACEPTADO", 0)
    consentimientos_revocados = conteo_eventos.get("CONSENTIMIENTO_REVOCADO", 0)
    autorizaciones_otorgadas = conteo_eventos.get("AUTORIZACION_OTORGADA", 0)
    autorizaciones_revocadas = conteo_eventos.get("AUTORIZACION_REVOCADA", 0)
    cuentas_eliminadas = conteo_eventos.get("CUENTA_ELIMINADA", 0)

    total_pac_reg = int(totales_personas.get("total_pacientes_registrados", 0))
    total_pac_eval = int(totales_personas.get("total_pacientes_con_evaluacion", 0))
    total_pac_cita = int(totales_personas.get("total_pacientes_con_cita_confirmada", 0))

    indicadores = {
        "total_pacientes_registrados": _formatear_cifra_privacidad(total_pac_reg, minimo_grupo),
        "total_pacientes_con_evaluacion": _formatear_cifra_privacidad(total_pac_eval, minimo_grupo),
        "total_pacientes_con_cita_confirmada": _formatear_cifra_privacidad(total_pac_cita, minimo_grupo),
        "citas_propuestas": _formatear_cifra_privacidad(citas_propuestas, minimo_grupo),
        "citas_confirmadas": _formatear_cifra_privacidad(citas_confirmadas, minimo_grupo),
        "citas_rechazadas": _formatear_cifra_privacidad(citas_rechazadas, minimo_grupo),
        "citas_canceladas": _formatear_cifra_privacidad(citas_canceladas, minimo_grupo),
        "citas_cumplidas": _formatear_cifra_privacidad(citas_cumplidas, minimo_grupo),
        "citas_pospuestas": _formatear_cifra_privacidad(citas_pospuestas, minimo_grupo),
        "clinicos_altas": clinicos_altas,
        "clinicos_activados": clinicos_activados,
        "clinicos_desactivados": clinicos_desactivados,
        "clinicos_eliminados": clinicos_eliminados,
        "consentimientos_aceptados": _formatear_cifra_privacidad(consentimientos_aceptados, minimo_grupo),
        "consentimientos_revocados": _formatear_cifra_privacidad(consentimientos_revocados, minimo_grupo),
        "autorizaciones_otorgadas": _formatear_cifra_privacidad(autorizaciones_otorgadas, minimo_grupo),
        "autorizaciones_revocadas": _formatear_cifra_privacidad(autorizaciones_revocadas, minimo_grupo),
        "cuentas_eliminadas": _formatear_cifra_privacidad(cuentas_eliminadas, minimo_grupo),
    }

    conteos_citas = {
        "CITA_PROPUESTA": citas_propuestas,
        "CITA_CONFIRMADA": citas_confirmadas,
        "CITA_RECHAZADA": citas_rechazadas,
        "CITA_CUMPLIDA": citas_cumplidas,
        "CITA_CANCELADA": citas_canceladas,
        "CITA_POSPUESTA": citas_pospuestas,
    }

    conteos_clinicos = {
        "CLINICO_CREADO": clinicos_altas,
        "CLINICO_ACTIVADO": clinicos_activados,
        "CLINICO_DESACTIVADO": clinicos_desactivados,
        "CLINICO_ELIMINADO": clinicos_eliminados,
    }

    graficos = {
        "actividad_semanal_pacientes": _graficar_actividad_semanal_pacientes(eventos, minimo_grupo),
        "estado_citas": _graficar_estado_citas(conteos_citas, minimo_grupo),
        "gestion_clinicos": _graficar_gestion_clinicos(conteos_clinicos),
        "comparativa_pacientes": _graficar_comparativa_pacientes(total_pac_reg, total_pac_eval, total_pac_cita, minimo_grupo),
    }

    return {
        "indicadores": indicadores,
        "graficos": graficos,
    }


def construir_tablero(
    registros: List[Dict[str, Any]],
    minimo_grupo: int = 5,
    es_demostracion: bool = False,
    eventos_actividad: Optional[List[Dict[str, Any]]] = None,
    totales_personas: Optional[Dict[str, int]] = None,
) -> Dict[str, Any]:
    """Calcula metricas agregadas y genera los 8 graficos del tablero administrativo.

    Parametros:
        registros: Lista de evaluaciones completadas anonimizadas.
        minimo_grupo: Umbral minimo de evaluaciones para agregacion de privacidad.
        es_demostracion: Indica si los datos provienen del generador sintetico demo.
        eventos_actividad: Lista anonima de eventos de actividad {fecha, tipo}.
        totales_personas: Conteo DISTINCT de personas registradas, evaluadas y con cita.

    Retorna:
        Diccionario con indicadores y graficos en base64 de salud mental y de actividad.
    """
    total = len(registros)
    actividad_resultado = _procesar_actividad(eventos_actividad or [], totales_personas or {}, minimo_grupo)

    if total < minimo_grupo:
        return {
            "datos_insuficientes": True,
            "es_demostracion": es_demostracion,
            "mensaje": (
                f"Datos insuficientes para generar estadísticas psicométricas agregadas. "
                f"Se requiere un mínimo de {minimo_grupo} evaluaciones completadas "
                f"(actualmente registradas: {total})."
            ),
            "total_evaluaciones": total,
            "minimo_requerido": minimo_grupo,
            "indicadores": None,
            "graficos": None,
            "actividad": actividad_resultado,
        }

    # Carga en DataFrame de pandas
    df = pd.DataFrame(registros)

    # Calculos estadisticos con numpy
    phq9_valores = df["puntaje_phq9"].to_numpy(dtype=float)
    gad7_valores = df["puntaje_gad7"].to_numpy(dtype=float)

    media_phq9 = float(np.mean(phq9_valores))
    mediana_phq9 = float(np.median(phq9_valores))
    desv_phq9 = float(np.std(phq9_valores, ddof=1 if total > 1 else 0))

    media_gad7 = float(np.mean(gad7_valores))
    mediana_gad7 = float(np.median(gad7_valores))
    desv_gad7 = float(np.std(gad7_valores, ddof=1 if total > 1 else 0))

    # Coeficiente de correlacion de Pearson entre PHQ-9 y GAD-7
    if desv_phq9 > 0 and desv_gad7 > 0:
        matriz_corr = np.corrcoef(phq9_valores, gad7_valores)
        correlacion = float(matriz_corr[0, 1])
        if np.isnan(correlacion):
            correlacion = None
        else:
            correlacion = round(correlacion, 3)
    else:
        correlacion = None

    # Calculo de riesgo de crisis y su complemento (sin crisis)
    total_crisis = int(df["riesgo_crisis"].sum())
    total_sin_crisis = total - total_crisis

    if total_crisis == 0:
        crisis_reportada: Union[int, str] = 0
    elif total_crisis < minimo_grupo:
        crisis_reportada = f"<{minimo_grupo}"
    else:
        crisis_reportada = total_crisis

    if total_sin_crisis == 0:
        sin_crisis_reportada: Union[int, str] = 0
    elif total_sin_crisis < minimo_grupo:
        sin_crisis_reportada = f"<{minimo_grupo}"
    else:
        sin_crisis_reportada = total_sin_crisis

    # Si cualquiera de los dos grupos tiene menos de N casos (y > 0), ocultar porcentaje para evitar deduccion por resta
    if (0 < total_crisis < minimo_grupo) or (0 < total_sin_crisis < minimo_grupo):
        porcentaje_crisis = None
    elif total_crisis == 0:
        porcentaje_crisis = 0.0
    else:
        porcentaje_crisis = round((total_crisis / total) * 100, 1)

    indicadores = {
        "total_evaluaciones": total,
        "media_phq9": round(media_phq9, 2),
        "mediana_phq9": round(mediana_phq9, 1),
        "desviacion_phq9": round(desv_phq9, 2),
        "media_gad7": round(media_gad7, 2),
        "mediana_gad7": round(mediana_gad7, 1),
        "desviacion_gad7": round(desv_gad7, 2),
        "correlacion_phq9_gad7": correlacion,
        "total_crisis": crisis_reportada,
        "total_sin_crisis": sin_crisis_reportada,
        "porcentaje_crisis": porcentaje_crisis,
        "minimo_grupo": minimo_grupo,
    }

    # Preparacion y generacion de graficos psicometricos con matplotlib
    df_phq9_cat = _preparar_categorias_agrupadas(df["severidad_depresion"], CATEGORIAS_PHQ9, minimo_grupo)
    df_gad7_cat = _preparar_categorias_agrupadas(df["severidad_ansiedad"], CATEGORIAS_GAD7, minimo_grupo)

    grafico_phq9 = _graficar_barras_severidad(
        df_phq9_cat,
        "Distribución por Severidad Depresión (PHQ-9)",
        ETIQUETAS_PHQ9,
        minimo_grupo,
    )

    grafico_gad7 = _graficar_barras_severidad(
        df_gad7_cat,
        "Distribución por Severidad Ansiedad (GAD-7)",
        ETIQUETAS_GAD7,
        minimo_grupo,
    )

    grafico_semanas = _graficar_evaluaciones_semanales(df, minimo_grupo)
    grafico_matriz = _graficar_matriz_frecuencias(df, minimo_grupo)

    graficos = {
        "severidad_phq9": grafico_phq9,
        "severidad_gad7": grafico_gad7,
        "evaluaciones_semana": grafico_semanas,
        "phq9_vs_gad7": grafico_matriz,
    }

    return {
        "datos_insuficientes": False,
        "es_demostracion": es_demostracion,
        "mensaje": None,
        "total_evaluaciones": total,
        "minimo_requerido": minimo_grupo,
        "indicadores": indicadores,
        "graficos": graficos,
        "actividad": actividad_resultado,
    }


def escapar_campo_csv(valor: Any) -> str:
    """Escapa celdas de texto para CSV contra inyeccion de formulas (=, +, -, @) y delimitadores."""
    if valor is None:
        return ""
    texto = str(valor)
    if texto and texto[0] in ("=", "+", "-", "@"):
        texto = f"'{texto}"
    if any(c in texto for c in (";", '"', "\n", "\r")):
        texto_escapado = texto.replace('"', '""')
        return f'"{texto_escapado}"'
    return texto


def generar_csv_tablero(datos: Dict[str, Any], dias: int = 30) -> str:
    """Genera el contenido de exportacion CSV agregado del tablero.

    Aplica codificacion UTF-8 con BOM, separador ';' y escape de formulas.
    Garantiza que no exista ningun dato ni identificador individual.
    """
    lineas = []
    # BOM UTF-8
    prefijo_bom = "\ufeff"

    es_demo = datos.get("es_demostracion", False)
    periodo_str = "Historico completo" if dias == 0 else f"Ultimos {dias} dias"

    lineas.append(f"{escapar_campo_csv('INFORME ESTADISTICO AGREGADO DEL TABLERO')};{escapar_campo_csv(periodo_str)}")
    if es_demo:
        lineas.append(f"{escapar_campo_csv('MARCA_DEMO')};{escapar_campo_csv('DATOS DE DEMOSTRACIÓN — Cifras sintéticas sin personas reales')}")

    lineas.append("")
    lineas.append(f"{escapar_campo_csv('SECCION')};{escapar_campo_csv('1. INDICADORES DE CRIBADO CLINICO (PHQ-9 Y GAD-7)')}")
    lineas.append(f"{escapar_campo_csv('Indicador')};{escapar_campo_csv('Valor')}")

    indicadores = datos.get("indicadores") or {}
    lineas.append(f"{escapar_campo_csv('Total evaluaciones completadas')};{escapar_campo_csv(indicadores.get('total_evaluaciones', 0))}")
    lineas.append(f"{escapar_campo_csv('Media PHQ-9 (Depresion)')};{escapar_campo_csv(indicadores.get('media_phq9', 'N/D'))}")
    lineas.append(f"{escapar_campo_csv('Mediana PHQ-9')};{escapar_campo_csv(indicadores.get('mediana_phq9', 'N/D'))}")
    lineas.append(f"{escapar_campo_csv('Desviacion estandar PHQ-9')};{escapar_campo_csv(indicadores.get('desviacion_phq9', 'N/D'))}")
    lineas.append(f"{escapar_campo_csv('Media GAD-7 (Ansiedad)')};{escapar_campo_csv(indicadores.get('media_gad7', 'N/D'))}")
    lineas.append(f"{escapar_campo_csv('Mediana GAD-7')};{escapar_campo_csv(indicadores.get('mediana_gad7', 'N/D'))}")
    lineas.append(f"{escapar_campo_csv('Desviacion estandar GAD-7')};{escapar_campo_csv(indicadores.get('desviacion_gad7', 'N/D'))}")
    lineas.append(f"{escapar_campo_csv('Correlacion Pearson PHQ-9 vs GAD-7')};{escapar_campo_csv(indicadores.get('correlacion_phq9_gad7', 'N/D'))}")
    lineas.append(f"{escapar_campo_csv('Casos con riesgo de crisis')};{escapar_campo_csv(indicadores.get('total_crisis', 0))}")
    lineas.append(f"{escapar_campo_csv('Casos sin riesgo de crisis')};{escapar_campo_csv(indicadores.get('total_sin_crisis', 0))}")
    lineas.append(f"{escapar_campo_csv('Porcentaje de casos con riesgo de crisis')};{escapar_campo_csv(indicadores.get('porcentaje_crisis', 'N/D'))}")

    lineas.append("")
    lineas.append(f"{escapar_campo_csv('SECCION')};{escapar_campo_csv('2. ACTIVIDAD DEL SISTEMA')}")
    lineas.append(f"{escapar_campo_csv('Indicador')};{escapar_campo_csv('Valor')}")

    actividad = datos.get("actividad") or {}
    act_ind = actividad.get("indicadores") or {}

    lineas.append(f"{escapar_campo_csv('Pacientes registrados (distintos)')};{escapar_campo_csv(act_ind.get('total_pacientes_registrados', 0))}")
    lineas.append(f"{escapar_campo_csv('Pacientes con al menos una evaluacion completada')};{escapar_campo_csv(act_ind.get('total_pacientes_con_evaluacion', 0))}")
    lineas.append(f"{escapar_campo_csv('Pacientes con al menos una cita confirmada')};{escapar_campo_csv(act_ind.get('total_pacientes_con_cita_confirmada', 0))}")
    lineas.append(f"{escapar_campo_csv('Citas propuestas (por clinico)')};{escapar_campo_csv(act_ind.get('citas_propuestas', 0))}")
    lineas.append(f"{escapar_campo_csv('Citas confirmadas (por paciente)')};{escapar_campo_csv(act_ind.get('citas_confirmadas', 0))}")
    lineas.append(f"{escapar_campo_csv('Citas rechazadas (por paciente)')};{escapar_campo_csv(act_ind.get('citas_rechazadas', 0))}")
    lineas.append(f"{escapar_campo_csv('Citas cumplidas')};{escapar_campo_csv(act_ind.get('citas_cumplidas', 0))}")
    lineas.append(f"{escapar_campo_csv('Citas canceladas')};{escapar_campo_csv(act_ind.get('citas_canceladas', 0))}")
    lineas.append(f"{escapar_campo_csv('Citas pospuestas')};{escapar_campo_csv(act_ind.get('citas_pospuestas', 0))}")
    lineas.append(f"{escapar_campo_csv('Altas de profesionales clinicos')};{escapar_campo_csv(act_ind.get('clinicos_altas', 0))}")
    lineas.append(f"{escapar_campo_csv('Cuentas clinicas activadas')};{escapar_campo_csv(act_ind.get('clinicos_activados', 0))}")
    lineas.append(f"{escapar_campo_csv('Cuentas clinicas desactivadas')};{escapar_campo_csv(act_ind.get('clinicos_desactivados', 0))}")
    lineas.append(f"{escapar_campo_csv('Cuentas clinicas eliminadas')};{escapar_campo_csv(act_ind.get('clinicos_eliminados', 0))}")
    lineas.append(f"{escapar_campo_csv('Consentimientos aceptados')};{escapar_campo_csv(act_ind.get('consentimientos_aceptados', 0))}")
    lineas.append(f"{escapar_campo_csv('Consentimientos revocados')};{escapar_campo_csv(act_ind.get('consentimientos_revocados', 0))}")
    lineas.append(f"{escapar_campo_csv('Autorizaciones de acceso otorgadas')};{escapar_campo_csv(act_ind.get('autorizaciones_otorgadas', 0))}")
    lineas.append(f"{escapar_campo_csv('Autorizaciones de acceso revocadas')};{escapar_campo_csv(act_ind.get('autorizaciones_revocadas', 0))}")
    lineas.append(f"{escapar_campo_csv('Cuentas de usuario eliminadas')};{escapar_campo_csv(act_ind.get('cuentas_eliminadas', 0))}")

    lineas.append("")
    minimo_grupo = datos.get("minimo_requerido", 5)
    lineas.append(f"{escapar_campo_csv('AVISO_PRIVACIDAD')};{escapar_campo_csv(f'Datos protegidos bajo umbral minimo de grupo N={minimo_grupo} y supresion complementaria. No contiene listas nominales.')}")

    cuerpo_csv = "\r\n".join(lineas)
    return prefijo_bom + cuerpo_csv


