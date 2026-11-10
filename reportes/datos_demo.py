"""Generador de registros sinteticos anonimos para el modo demostracion del tablero.

Genera 120 registros sinteticos repartidos en las ultimas 12 semanas
utilizando una semilla fija para garantizar reproducibilidad exacta.
Ningun dato corresponde a personas o evaluaciones reales.
"""

from datetime import date, timedelta
import random
from typing import Any, Dict, List


def _clasificar_phq9(puntaje: int) -> str:
    if puntaje <= 4:
        return "MINIMA"
    if puntaje <= 9:
        return "LEVE"
    if puntaje <= 14:
        return "MODERADA"
    if puntaje <= 19:
        return "MODERADAMENTE_SEVERA"
    return "SEVERA"


def _clasificar_gad7(puntaje: int) -> str:
    if puntaje <= 4:
        return "MINIMA"
    if puntaje <= 9:
        return "LEVE"
    if puntaje <= 14:
        return "MODERADA"
    return "SEVERA"


def generar_registros_demo(cantidad: int = 120, semilla: int = 42) -> List[Dict[str, Any]]:
    """Genera una lista de registros sinteticos con distribucion psicometrica realista.

    Parametros:
        cantidad: Total de registros sinteticos a generar (por defecto 120).
        semilla: Semilla aleatoria para reproducibilidad (por defecto 42).

    Retorna:
        Lista de diccionarios con formato anonimo compatible con SolicitudTablero.
    """
    rng = random.Random(semilla)
    fecha_referencia = date.today()

    registros = []
    for _ in range(cantidad):
        # Repartir en las ultimas 12 semanas (84 dias)
        dias_atras = rng.randint(0, 83)
        fecha_eval = fecha_referencia - timedelta(days=dias_atras)

        # Generar puntajes correlacionados de forma realista
        base = rng.gauss(mu=9.0, sigma=4.5)
        puntaje_phq9 = max(0, min(27, int(round(base + rng.gauss(mu=0, sigma=2.0)))))
        puntaje_gad7 = max(0, min(21, int(round(base * 0.75 + rng.gauss(mu=0, sigma=2.0)))))

        severidad_phq9 = _clasificar_phq9(puntaje_phq9)
        severidad_gad7 = _clasificar_gad7(puntaje_gad7)

        # Aproximadamente 10-12% de casos con riesgo de crisis en severidades altas
        if severidad_phq9 in ("MODERADAMENTE_SEVERA", "SEVERA") and rng.random() < 0.45:
            riesgo_crisis = True
        elif rng.random() < 0.03:
            riesgo_crisis = True
        else:
            riesgo_crisis = False

        registros.append({
            "fecha": fecha_eval.strftime("%Y-%m-%d"),
            "puntaje_phq9": puntaje_phq9,
            "puntaje_gad7": puntaje_gad7,
            "severidad_depresion": severidad_phq9,
            "severidad_ansiedad": severidad_gad7,
            "riesgo_crisis": riesgo_crisis,
        })

    # Ordenar cronológicamente
    registros.sort(key=lambda r: r["fecha"])
    return registros


def generar_actividad_demo(semilla: int = 42) -> tuple[List[Dict[str, Any]], Dict[str, int]]:
    """Genera eventos de actividad sinteticos coherentes para el modo demostracion.

    Parametros:
        semilla: Semilla fija para reproducibilidad exacta (por defecto 42).

    Retorna:
        Tupla con (lista de eventos {fecha, tipo}, diccionario con totales distinct de personas).
    """
    rng = random.Random(semilla)
    fecha_referencia = date.today()

    eventos = []

    def _fecha_aleatoria(dias_max: int = 83) -> str:
        dias_atras = rng.randint(0, dias_max)
        return (fecha_referencia - timedelta(days=dias_atras)).strftime("%Y-%m-%d")

    # 1. Altas y consentimientos de pacientes (150 pacientes)
    for _ in range(150):
        f = _fecha_aleatoria()
        eventos.append({"fecha": f, "tipo": "REGISTRO_PACIENTE"})
        eventos.append({"fecha": f, "tipo": "CONSENTIMIENTO_ACEPTADO"})

    for _ in range(4):
        eventos.append({"fecha": _fecha_aleatoria(), "tipo": "CONSENTIMIENTO_REVOCADO"})

    for _ in range(3):
        eventos.append({"fecha": _fecha_aleatoria(), "tipo": "CUENTA_ELIMINADA"})

    # 2. Evaluaciones completadas (120 eventos coherentes con registros)
    for _ in range(120):
        eventos.append({"fecha": _fecha_aleatoria(), "tipo": "EVALUACION_COMPLETADA"})

    # 3. Autorizaciones para compartir con clinicos
    for _ in range(65):
        eventos.append({"fecha": _fecha_aleatoria(), "tipo": "AUTORIZACION_OTORGADA"})
    for _ in range(6):
        eventos.append({"fecha": _fecha_aleatoria(), "tipo": "AUTORIZACION_REVOCADA"})

    # 4. Flujo de citas (el clinico propone, el paciente confirma o rechaza)
    for _ in range(48):
        eventos.append({"fecha": _fecha_aleatoria(), "tipo": "CITA_PROPUESTA"})
    for _ in range(38):
        eventos.append({"fecha": _fecha_aleatoria(), "tipo": "CITA_CONFIRMADA"})
    for _ in range(6):
        eventos.append({"fecha": _fecha_aleatoria(), "tipo": "CITA_RECHAZADA"})
    for _ in range(30):
        eventos.append({"fecha": _fecha_aleatoria(), "tipo": "CITA_CUMPLIDA"})
    for _ in range(5):
        eventos.append({"fecha": _fecha_aleatoria(), "tipo": "CITA_CANCELADA"})
    for _ in range(3):
        eventos.append({"fecha": _fecha_aleatoria(), "tipo": "CITA_POSPUESTA"})

    # 5. Gestion de profesionales clinicos
    for _ in range(8):
        eventos.append({"fecha": _fecha_aleatoria(), "tipo": "CLINICO_CREADO"})
    for _ in range(7):
        eventos.append({"fecha": _fecha_aleatoria(), "tipo": "CLINICO_ACTIVADO"})
    for _ in range(2):
        eventos.append({"fecha": _fecha_aleatoria(), "tipo": "CLINICO_DESACTIVADO"})
    for _ in range(1):
        eventos.append({"fecha": _fecha_aleatoria(), "tipo": "CLINICO_ELIMINADO"})

    eventos.sort(key=lambda e: e["fecha"])

    totales_personas = {
        "total_pacientes_registrados": 150,
        "total_pacientes_con_evaluacion": 95,
        "total_pacientes_con_cita_confirmada": 35,
    }

    return eventos, totales_personas

