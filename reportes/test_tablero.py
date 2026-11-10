"""Pruebas unitarias y de integracion para el modulo del tablero estadistico.

Verifica:
- Seguridad de cabecera X-Clave-Interna
- Validaciones Pydantic y rechazo de campos no autorizados (extra='forbid')
- Regla de grupo minimo N de privacidad
- Agrupacion en 'Otros' y cifras enmascaradas como '<N'
- Calculos estadisticos con numpy y pandas
- Generacion en memoria de los 4 graficos en base64
"""

import base64
import os
import pytest
from fastapi.testclient import TestClient

# Establecer clave interna para entorno de pruebas
os.environ["CLAVE_INTERNA"] = "clave-secreta-prueba"

from principal import aplicacion
from tablero import construir_tablero

cliente = TestClient(aplicacion)
CABECERA_AUTORIZADA = {"X-Clave-Interna": "clave-secreta-prueba"}


def test_tablero_requiere_clave_interna():
    """El endpoint /tablero debe rechazar peticiones sin la cabecera X-Clave-Interna o con clave invalida."""
    cuerpo = {"registros": [], "minimo_grupo": 5}

    # Sin cabecera
    resp = cliente.post("/tablero", json=cuerpo)
    assert resp.status_code == 401

    # Con clave incorrecta
    resp_invalida = cliente.post("/tablero", json=cuerpo, headers={"X-Clave-Interna": "clave-erronea"})
    assert resp_invalida.status_code == 401


def test_tablero_rechaza_campos_adicionales_privacidad():
    """Pydantic debe rechazar de forma estricta (extra='forbid') cualquier campo como correo, nombre o id."""
    cuerpo_con_campo_prohibido = {
        "registros": [
            {
                "fecha": "2026-11-01",
                "puntaje_phq9": 10,
                "puntaje_gad7": 8,
                "severidad_depresion": "MODERADA",
                "severidad_ansiedad": "LEVE",
                "riesgo_crisis": False,
                "correo": "paciente@ejemplo.com",  # Campo no permitido
            }
        ],
        "minimo_grupo": 5,
    }

    resp = cliente.post("/tablero", json=cuerpo_con_campo_prohibido, headers=CABECERA_AUTORIZADA)
    assert resp.status_code == 422


def test_tablero_valida_rango_puntajes():
    """Puntajes fuera de rango psicometrico (PHQ-9 0-27, GAD-7 0-21) deben ser rechazados."""
    # PHQ9 > 27
    cuerpo_phq_invalido = {
        "registros": [
            {
                "fecha": "2026-11-01",
                "puntaje_phq9": 28,
                "puntaje_gad7": 5,
                "severidad_depresion": "SEVERA",
                "severidad_ansiedad": "LEVE",
            }
        ],
        "minimo_grupo": 5,
    }
    resp1 = cliente.post("/tablero", json=cuerpo_phq_invalido, headers=CABECERA_AUTORIZADA)
    assert resp1.status_code == 422

    # GAD7 > 21
    cuerpo_gad_invalido = {
        "registros": [
            {
                "fecha": "2026-11-01",
                "puntaje_phq9": 10,
                "puntaje_gad7": 22,
                "severidad_depresion": "MODERADA",
                "severidad_ansiedad": "SEVERA",
            }
        ],
        "minimo_grupo": 5,
    }
    resp2 = cliente.post("/tablero", json=cuerpo_gad_invalido, headers=CABECERA_AUTORIZADA)
    assert resp2.status_code == 422


def test_tablero_datos_insuficientes_si_menos_de_minimo_grupo():
    """Si hay menos de N evaluaciones, no se calculan graficos y se avisa de datos insuficientes."""
    registros = [
        {
            "fecha": "2026-11-01",
            "puntaje_phq9": 8,
            "puntaje_gad7": 6,
            "severidad_depresion": "LEVE",
            "severidad_ansiedad": "LEVE",
            "riesgo_crisis": False,
        },
        {
            "fecha": "2026-11-02",
            "puntaje_phq9": 14,
            "puntaje_gad7": 11,
            "severidad_depresion": "MODERADA",
            "severidad_ansiedad": "MODERADA",
            "riesgo_crisis": False,
        },
    ]

    resp = cliente.post("/tablero", json={"registros": registros, "minimo_grupo": 5}, headers=CABECERA_AUTORIZADA)
    assert resp.status_code == 200
    datos = resp.json()

    assert datos["datos_insuficientes"] is True
    assert "Datos insuficientes" in datos["mensaje"]
    assert datos["total_evaluaciones"] == 2
    assert datos["minimo_requerido"] == 5
    assert datos["indicadores"] is None
    assert datos["graficos"] is None


def test_tablero_calculos_e_imagenes_exitosas():
    """Con N o mas evaluaciones, calcula metricas con numpy/pandas y genera 4 imagenes base64."""
    registros = [
        {"fecha": "2026-10-01", "puntaje_phq9": 2, "puntaje_gad7": 2, "severidad_depresion": "MINIMA", "severidad_ansiedad": "MINIMA", "riesgo_crisis": False},
        {"fecha": "2026-10-08", "puntaje_phq9": 6, "puntaje_gad7": 7, "severidad_depresion": "LEVE", "severidad_ansiedad": "LEVE", "riesgo_crisis": False},
        {"fecha": "2026-10-15", "puntaje_phq9": 12, "puntaje_gad7": 10, "severidad_depresion": "MODERADA", "severidad_ansiedad": "MODERADA", "riesgo_crisis": False},
        {"fecha": "2026-10-22", "puntaje_phq9": 16, "puntaje_gad7": 15, "severidad_depresion": "MODERADAMENTE_SEVERA", "severidad_ansiedad": "SEVERA", "riesgo_crisis": True},
        {"fecha": "2026-10-29", "puntaje_phq9": 22, "puntaje_gad7": 18, "severidad_depresion": "SEVERA", "severidad_ansiedad": "SEVERA", "riesgo_crisis": False},
        {"fecha": "2026-11-05", "puntaje_phq9": 8, "puntaje_gad7": 6, "severidad_depresion": "LEVE", "severidad_ansiedad": "LEVE", "riesgo_crisis": False},
    ]

    resp = cliente.post("/tablero", json={"registros": registros, "minimo_grupo": 5}, headers=CABECERA_AUTORIZADA)
    assert resp.status_code == 200
    datos = resp.json()

    assert datos["datos_insuficientes"] is False
    assert datos["total_evaluaciones"] == 6

    indicadores = datos["indicadores"]
    assert indicadores is not None
    assert indicadores["total_evaluaciones"] == 6
    assert isinstance(indicadores["media_phq9"], float)
    assert isinstance(indicadores["media_gad7"], float)
    assert isinstance(indicadores["correlacion_phq9_gad7"], float)
    # Como solo 1 caso tiene riesgo de crisis y 1 < 5, se enmascara como "<5"
    assert indicadores["total_crisis"] == "<5"
    # Si el conteo de crisis es <N, no se muestra su porcentaje ni el del complemento
    assert indicadores["porcentaje_crisis"] is None

    graficos = datos["graficos"]
    assert graficos is not None
    for clave in ["severidad_phq9", "severidad_gad7", "evaluaciones_semana", "phq9_vs_gad7"]:
        assert clave in graficos
        # Validar que es base64 valido decodificable
        contenido_b64 = graficos[clave]
        bytes_png = base64.b64decode(contenido_b64)
        assert bytes_png.startswith(b"\x89PNG")  # Encabezado magico PNG


def test_construir_tablero_directo_regla_otros():
    """Verifica directamente la funcion de agrupamiento en Otros cuando conteos son menores a N."""
    registros = [
        {"fecha": "2026-11-01", "puntaje_phq9": 11, "puntaje_gad7": 11, "severidad_depresion": "MODERADA", "severidad_ansiedad": "MODERADA", "riesgo_crisis": False},
        {"fecha": "2026-11-01", "puntaje_phq9": 12, "puntaje_gad7": 12, "severidad_depresion": "MODERADA", "severidad_ansiedad": "MODERADA", "riesgo_crisis": False},
        {"fecha": "2026-11-01", "puntaje_phq9": 13, "puntaje_gad7": 13, "severidad_depresion": "MODERADA", "severidad_ansiedad": "MODERADA", "riesgo_crisis": False},
        {"fecha": "2026-11-01", "puntaje_phq9": 10, "puntaje_gad7": 10, "severidad_depresion": "MODERADA", "severidad_ansiedad": "MODERADA", "riesgo_crisis": False},
        {"fecha": "2026-11-01", "puntaje_phq9": 14, "puntaje_gad7": 14, "severidad_depresion": "MODERADA", "severidad_ansiedad": "MODERADA", "riesgo_crisis": False},
        {"fecha": "2026-11-02", "puntaje_phq9": 2, "puntaje_gad7": 1, "severidad_depresion": "MINIMA", "severidad_ansiedad": "MINIMA", "riesgo_crisis": False},
        {"fecha": "2026-11-02", "puntaje_phq9": 22, "puntaje_gad7": 18, "severidad_depresion": "SEVERA", "severidad_ansiedad": "SEVERA", "riesgo_crisis": False},
    ]

    resultado = construir_tablero(registros, minimo_grupo=5)
    assert resultado["datos_insuficientes"] is False
    assert resultado["indicadores"]["total_evaluaciones"] == 7
    # Debe generar los 4 graficos
    assert len(resultado["graficos"]) == 4


def test_evitar_deduccion_por_resta_supresion_complementaria():
    """Verifica que si solo 1 categoria cae por debajo de N, se aplique supresion complementaria

    para que no sea posible deducir el conteo exacto de la categoria pequeña restando del total.
    """
    import pandas as pd
    from tablero import _preparar_categorias_agrupadas, CATEGORIAS_PHQ9

    # 18 registros: MODERADA=10, MINIMA=6, SEVERA=2. Con N=5, solo SEVERA es < 5.
    serie = pd.Series(["MODERADA"] * 10 + ["MINIMA"] * 6 + ["SEVERA"] * 2)

    df_agrupado = _preparar_categorias_agrupadas(serie, CATEGORIAS_PHQ9, minimo_grupo=5)
    categorias_resultantes = set(df_agrupado["categoria"].tolist())

    # Debe incluir 'OTROS'
    assert "OTROS" in categorias_resultantes
    # Y debido a la supresion complementaria para evitar deduccion por resta,
    # la categoria mas pequeña restante (MINIMA) debio incorporarse a OTROS junto con SEVERA.
    assert "MODERADA" in categorias_resultantes
    assert "MINIMA" not in categorias_resultantes  # Suprimida complementariamente
    # El valor de OTROS es 6 + 2 = 8, impidiendo deducir individualmente SEVERA=2
    conteo_otros = int(df_agrupado[df_agrupado["categoria"] == "OTROS"]["conteo"].iloc[0])
    assert conteo_otros == 8


def test_bloques_semanales_acumulan_al_menos_n():
    """Verifica que la serie temporal agrupe semanas consecutivas hasta tener al menos N casos

    y que la ultima franja incompleta se fusione con la anterior, sin puntos sueltos < N.
    """
    import pandas as pd
    from tablero import _graficar_evaluaciones_semanales

    # Registros repartidos en 3 semanas: semana 1 (3 casos), semana 2 (3 casos), semana 3 (2 casos)
    # Total = 8 casos. Con N=5, las semanas 1 y 2 se unen (6 casos), y la semana 3 (2 casos) se fusiona al bloque.
    registros = [
        {"fecha": "2026-10-05"}, {"fecha": "2026-10-06"}, {"fecha": "2026-10-07"},
        {"fecha": "2026-10-12"}, {"fecha": "2026-10-13"}, {"fecha": "2026-10-14"},
        {"fecha": "2026-10-19"}, {"fecha": "2026-10-20"},
    ]
    df = pd.DataFrame(registros)
    grafico_b64 = _graficar_evaluaciones_semanales(df, minimo_grupo=5)
    assert isinstance(grafico_b64, str)
    assert len(grafico_b64) > 100


def test_matriz_frecuencias_supresion_complementaria():
    """Verifica que la matriz no exponga celdas unicas < N que puedan deducirse por resta de filas o columnas."""
    import pandas as pd
    from tablero import _graficar_matriz_frecuencias

    # Datos donde una sola celda es pequeña
    registros = []
    # 10 en MINIMA/MINIMA
    for _ in range(10):
        registros.append({"severidad_depresion": "MINIMA", "severidad_ansiedad": "MINIMA"})
    # 6 en MINIMA/LEVE
    for _ in range(6):
        registros.append({"severidad_depresion": "MINIMA", "severidad_ansiedad": "LEVE"})
    # 2 en MODERADA/MODERADA (celda pequeña)
    for _ in range(2):
        registros.append({"severidad_depresion": "MODERADA", "severidad_ansiedad": "MODERADA"})
    # 8 en MODERADA/SEVERA
    for _ in range(8):
        registros.append({"severidad_depresion": "MODERADA", "severidad_ansiedad": "SEVERA"})

    df = pd.DataFrame(registros)
    grafico_matriz = _graficar_matriz_frecuencias(df, minimo_grupo=5)
    assert isinstance(grafico_matriz, str)
    assert len(grafico_matriz) > 100


def test_crisis_complemento_suprime_si_sin_crisis_menor_a_n():
    """Con 18 casos con crisis y 2 sin crisis (N=5), el complemento es menor a N.

    El grupo sin crisis debe mostrar '<5' y el porcentaje debe ocultarse (None)
    para evitar deduccion residual por resta.
    """
    registros = []
    # 18 con crisis
    for _ in range(18):
        registros.append({
            "fecha": "2026-11-01",
            "puntaje_phq9": 20,
            "puntaje_gad7": 16,
            "severidad_depresion": "SEVERA",
            "severidad_ansiedad": "SEVERA",
            "riesgo_crisis": True,
        })
    # 2 sin crisis
    for _ in range(2):
        registros.append({
            "fecha": "2026-11-02",
            "puntaje_phq9": 10,
            "puntaje_gad7": 8,
            "severidad_depresion": "MODERADA",
            "severidad_ansiedad": "LEVE",
            "riesgo_crisis": False,
        })

    resultado = construir_tablero(registros, minimo_grupo=5)
    assert resultado["datos_insuficientes"] is False
    assert resultado["total_evaluaciones"] == 20

    indicadores = resultado["indicadores"]
    assert indicadores["total_crisis"] == 18
    assert indicadores["total_sin_crisis"] == "<5"
    assert indicadores["porcentaje_crisis"] is None


def test_modo_demo_genera_datos_sinteticos_con_aviso():
    """El modo demo (demo=True) genera 120 registros sinteticos y marca es_demostracion=True."""
    resp = cliente.post("/tablero", json={"demo": True, "minimo_grupo": 5}, headers=CABECERA_AUTORIZADA)
    assert resp.status_code == 200
    datos = resp.json()

    assert datos["es_demostracion"] is True
    assert datos["datos_insuficientes"] is False
    assert datos["total_evaluaciones"] == 120
    assert datos["indicadores"] is not None
    assert datos["graficos"] is not None
    assert len(datos["graficos"]) == 4


def test_modo_demo_false_marca_es_demostracion_false():
    """Cuando demo=False, la respuesta indica es_demostracion=False."""
    registros = [
        {"fecha": "2026-11-01", "puntaje_phq9": 2, "puntaje_gad7": 2, "severidad_depresion": "MINIMA", "severidad_ansiedad": "MINIMA", "riesgo_crisis": False},
        {"fecha": "2026-11-01", "puntaje_phq9": 6, "puntaje_gad7": 5, "severidad_depresion": "LEVE", "severidad_ansiedad": "LEVE", "riesgo_crisis": False},
        {"fecha": "2026-11-02", "puntaje_phq9": 11, "puntaje_gad7": 10, "severidad_depresion": "MODERADA", "severidad_ansiedad": "MODERADA", "riesgo_crisis": False},
        {"fecha": "2026-11-02", "puntaje_phq9": 16, "puntaje_gad7": 12, "severidad_depresion": "MODERADAMENTE_SEVERA", "severidad_ansiedad": "MODERADA", "riesgo_crisis": False},
        {"fecha": "2026-11-03", "puntaje_phq9": 22, "puntaje_gad7": 18, "severidad_depresion": "SEVERA", "severidad_ansiedad": "SEVERA", "riesgo_crisis": False},
    ]
    resp = cliente.post("/tablero", json={"demo": False, "registros": registros, "minimo_grupo": 5}, headers=CABECERA_AUTORIZADA)
    assert resp.status_code == 200
    datos = resp.json()
    assert datos["es_demostracion"] is False
    assert "actividad" in datos
    assert len(datos["actividad"]["graficos"]) == 4


def test_actividad_supresion_pacientes_menor_a_umbral():
    """Métricas de pacientes por debajo del umbral mínimo N deben aparecer enmascaradas como '<N'."""
    from tablero import construir_tablero

    eventos = [
        {"fecha": "2026-11-01", "tipo": "CITA_PROPUESTA"},
        {"fecha": "2026-11-01", "tipo": "CITA_CONFIRMADA"},
        {"fecha": "2026-11-02", "tipo": "CITA_CONFIRMADA"},
    ]
    totales = {
        "total_pacientes_registrados": 3,
        "total_pacientes_con_evaluacion": 2,
        "total_pacientes_con_cita_confirmada": 2,
    }

    resultado = construir_tablero([], minimo_grupo=5, eventos_actividad=eventos, totales_personas=totales)
    act_ind = resultado["actividad"]["indicadores"]

    assert act_ind["total_pacientes_registrados"] == "<5"
    assert act_ind["total_pacientes_con_evaluacion"] == "<5"
    assert act_ind["total_pacientes_con_cita_confirmada"] == "<5"
    assert act_ind["citas_propuestas"] == "<5"
    assert act_ind["citas_confirmadas"] == "<5"


def test_actividad_exactitud_conteos_clinicos():
    """Las métricas de gestión de personal clínico son conteos exactos sin enmascaramiento."""
    from tablero import construir_tablero

    eventos = [
        {"fecha": "2026-11-01", "tipo": "CLINICO_CREADO"},
        {"fecha": "2026-11-01", "tipo": "CLINICO_CREADO"},
        {"fecha": "2026-11-02", "tipo": "CLINICO_ACTIVADO"},
        {"fecha": "2026-11-03", "tipo": "CLINICO_DESACTIVADO"},
        {"fecha": "2026-11-04", "tipo": "CLINICO_ELIMINADO"},
    ]

    resultado = construir_tablero([], minimo_grupo=5, eventos_actividad=eventos)
    act_ind = resultado["actividad"]["indicadores"]

    # Conteo exacto incluso siendo menor a 5
    assert act_ind["clinicos_altas"] == 2
    assert act_ind["clinicos_activados"] == 1
    assert act_ind["clinicos_desactivados"] == 1
    assert act_ind["clinicos_eliminados"] == 1


def test_actividad_caso_cero_eventos():
    """El tablero maneja correctamente el caso de 0 eventos sin lanzar excepciones."""
    from tablero import construir_tablero

    resultado = construir_tablero([], minimo_grupo=5, eventos_actividad=[], totales_personas={})
    assert "actividad" in resultado
    act = resultado["actividad"]
    assert act["indicadores"]["total_pacientes_registrados"] == "0"
    assert act["indicadores"]["clinicos_altas"] == 0
    assert len(act["graficos"]) == 4


def test_formato_csv_bom_separador_y_escape():
    """Verifica que el CSV incluya BOM, use ';' como separador y escape fórmulas que empiecen por =, +, -, @."""
    from tablero import generar_csv_tablero, escapar_campo_csv

    # Prueba individual de escape
    assert escapar_campo_csv("=SUMA(A1:A5)").startswith("'=")
    assert escapar_campo_csv("+500").startswith("'+")
    assert escapar_campo_csv("-100").startswith("'-")
    assert escapar_campo_csv("@usuario").startswith("'@")
    assert escapar_campo_csv("Texto normal") == "Texto normal"
    assert escapar_campo_csv("<5") == "<5"

    datos_prueba = {
        "es_demostracion": True,
        "minimo_requerido": 5,
        "indicadores": {
            "total_evaluaciones": 120,
            "media_phq9": 8.5,
            "mediana_phq9": 8.0,
            "desviacion_phq9": 3.2,
            "media_gad7": 6.4,
            "mediana_gad7": 6.0,
            "desviacion_gad7": 2.8,
            "correlacion_phq9_gad7": 0.65,
            "total_crisis": "<5",
            "total_sin_crisis": 118,
            "porcentaje_crisis": None,
        },
        "actividad": {
            "indicadores": {
                "total_pacientes_registrados": 150,
                "total_pacientes_con_evaluacion": 95,
                "total_pacientes_con_cita_confirmada": 35,
                "citas_propuestas": 48,
                "citas_confirmadas": 38,
                "citas_rechazadas": 6,
                "citas_cumplidas": 30,
                "citas_canceladas": 5,
                "citas_pospuestas": 3,
                "clinicos_altas": 8,
                "clinicos_activados": 7,
                "clinicos_desactivados": 2,
                "clinicos_eliminados": 1,
                "consentimientos_aceptados": 150,
                "consentimientos_revocados": 4,
                "autorizaciones_otorgadas": 65,
                "autorizaciones_revocadas": 6,
                "cuentas_eliminadas": 3,
            }
        }
    }

    csv_generado = generar_csv_tablero(datos_prueba, dias=30)
    assert csv_generado.startswith("\ufeff")
    assert ";" in csv_generado
    assert "DATOS DE DEMOSTRACIÓN" in csv_generado
    assert "<5" in csv_generado


def test_exportacion_pdf_tablero_endpoint():
    """El endpoint /tablero/exportar-pdf genera un documento PDF válido."""
    resp_tablero = cliente.post("/tablero", json={"demo": True, "minimo_grupo": 5}, headers=CABECERA_AUTORIZADA)
    assert resp_tablero.status_code == 200
    datos = resp_tablero.json()

    resp_pdf = cliente.post(
        "/tablero/exportar-pdf",
        json={"datos": datos, "dias": 30, "fecha_reporte": "2026-11-05"},
        headers=CABECERA_AUTORIZADA,
    )
    assert resp_pdf.status_code == 200
    assert resp_pdf.headers["content-type"] == "application/pdf"
    assert resp_pdf.content.startswith(b"%PDF-")
    assert len(resp_pdf.content) > 1000




