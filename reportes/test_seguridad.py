"""Pruebas de integracion y seguridad para el microservicio de reportes FastAPI.

Reglas del proyecto:
- Convencion pytest (inicia con test_).
- Todo el codigo, identificadores y comentarios en espanol, sin tildes ni enies.
- Verificacion de seguridad: rechazo 401 por cabecera ausente o incorrecta.
- Verificacion de invariante: rechazo cuando riesgo_crisis y resultado_minimo son ambos verdaderos.
"""

import os
from fastapi.testclient import TestClient
import pytest

from principal import aplicacion


CLAVE_PRUEBA = "secreto_super_seguro_para_pruebas_12345"


@pytest.fixture
def cliente_http(monkeypatch):
    """Fixture que configura la variable de entorno CLAVE_INTERNA y entrega el cliente de prueba."""
    monkeypatch.setenv("CLAVE_INTERNA", CLAVE_PRUEBA)
    return TestClient(aplicacion)


def obtener_cuerpo_solicitud_valido():
    """Genera una carga util valida para las pruebas de generacion de reportes."""
    return {
        "nombre_paciente": "Laura Restrepo",
        "fecha_evaluacion": "2026-10-13",
        "puntaje_phq9": 7,
        "puntaje_gad7": 5,
        "severidad_depresion": "LEVE",
        "severidad_ansiedad": "LEVE",
        "riesgo_crisis": False,
        "resultado_minimo": False,
        "lineas_ayuda": [
            {
                "nombre": "Linea 106",
                "telefono": "106",
                "horario": "24/7",
                "descripcion": "Orientacion psicosocial",
            }
        ],
        "respuestas_resumen": [
            {
                "instrumento": "PHQ-9",
                "item": 1,
                "texto": "Poco interes o placer en hacer cosas",
                "frecuencia": "Varios dias",
            }
        ],
    }


def test_rechazo_con_401_cuando_falta_cabecera_clave_interna(cliente_http):
    """Verifica que el servicio responda HTTP 401 si no se envia la cabecera X-Clave-Interna."""
    cuerpo = obtener_cuerpo_solicitud_valido()
    respuesta = cliente_http.post("/api/reportes/generar", json=cuerpo)

    assert respuesta.status_code == 401
    assert respuesta.json().get("detail") == "No autorizado"
    assert CLAVE_PRUEBA not in respuesta.text
    assert "CLAVE_INTERNA" not in respuesta.text


def test_rechazo_con_401_cuando_clave_interna_es_invalida(cliente_http):
    """Verifica que el servicio responda HTTP 401 si la cabecera X-Clave-Interna es incorrecta."""
    cuerpo = obtener_cuerpo_solicitud_valido()
    encabezados = {"X-Clave-Interna": "clave_falsa_o_incorrecta"}
    respuesta = cliente_http.post("/api/reportes/generar", json=cuerpo, headers=encabezados)

    assert respuesta.status_code == 401
    assert respuesta.json().get("detail") == "No autorizado"
    assert CLAVE_PRUEBA not in respuesta.text
    assert "CLAVE_INTERNA" not in respuesta.text


def test_rechazo_con_401_si_servidor_no_tiene_clave_configurada(monkeypatch):
    """Verifica que si la variable de entorno CLAVE_INTERNA no existe, se rechace con 401 por precaucion."""
    monkeypatch.delenv("CLAVE_INTERNA", raising=False)
    cliente = TestClient(aplicacion)

    cuerpo = obtener_cuerpo_solicitud_valido()
    encabezados = {"X-Clave-Interna": "cualquier_clave"}
    respuesta = cliente.post("/api/reportes/generar", json=cuerpo, headers=encabezados)

    assert respuesta.status_code == 401
    assert respuesta.json().get("detail") == "No autorizado"
    assert "CLAVE_INTERNA" not in respuesta.text


def test_logs_no_contienen_nombre_paciente_ni_valores_reactivos(cliente_http, caplog):
    """Verifica que en una generacion exitosa de PDF los logs no registren datos sensibles ni valores de reactivos."""
    import logging

    caplog.set_level(logging.INFO)

    cuerpo = {
        "nombre_paciente": "Valentina Gomez Morales",
        "fecha_evaluacion": "2026-10-14",
        "puntaje_phq9": 14,
        "puntaje_gad7": 11,
        "severidad_depresion": "MODERADA",
        "severidad_ansiedad": "MODERADA",
        "riesgo_crisis": False,
        "resultado_minimo": False,
        "lineas_ayuda": [
            {
                "nombre": "Linea Esperanza 106",
                "telefono": "106",
                "horario": "24/7",
                "descripcion": "Orientacion y soporte confidencial",
            }
        ],
        "respuestas_resumen": [
            {
                "instrumento": "PHQ-9",
                "item": 1,
                "texto": "Poco interes o placer en realizar actividades",
                "frecuencia": "Mas de la mitad de los dias",
            }
        ],
    }

    encabezados = {"X-Clave-Interna": CLAVE_PRUEBA}
    respuesta = cliente_http.post("/api/reportes/generar", json=cuerpo, headers=encabezados)

    assert respuesta.status_code == 200
    assert respuesta.headers["content-type"] == "application/pdf"

    texto_logs = caplog.text

    # No debe figurar el nombre del paciente ni sus componentes
    assert cuerpo["nombre_paciente"] not in texto_logs
    assert "Valentina" not in texto_logs
    assert "Gomez" not in texto_logs
    assert "Morales" not in texto_logs

    # No deben figurar los nombres de campos del payload
    assert "nombre_paciente" not in texto_logs
    assert "lineas_ayuda" not in texto_logs
    assert "respuestas_resumen" not in texto_logs

    # No deben figurar recursos ni textos de preguntas/sintomas
    assert "Linea Esperanza 106" not in texto_logs
    assert "Poco interes o placer" not in texto_logs

    # Los registros del microservicio deben existir y limitarse unicamente a metodo, ruta, codigo HTTP y tiempo en ms
    registros_servicio = [r for r in caplog.records if r.name == "servicio_reportes"]
    assert len(registros_servicio) > 0

    for registro in registros_servicio:
        mensaje = registro.getMessage()
        partes = mensaje.split()
        assert len(partes) == 5, f"Formato de log inesperado: {mensaje}"
        assert partes[0] == "POST"
        assert partes[1] == "/api/reportes/generar"
        assert partes[2] == "200"
        assert float(partes[3]) >= 0.0
        assert partes[4] == "ms"
        # Ningun dato del paciente ni del payload en el mensaje registrado
        assert cuerpo["nombre_paciente"] not in mensaje
        assert "Valentina" not in mensaje
        assert "14" not in partes[:3]
        assert "11" not in partes[:3]


def test_generacion_exitosa_con_clave_interna_valida(cliente_http):
    """Verifica que con clave valida el servicio retorne 200, Content-Type PDF y bytes no vacios."""
    cuerpo = obtener_cuerpo_solicitud_valido()
    encabezados = {"X-Clave-Interna": CLAVE_PRUEBA}
    respuesta = cliente_http.post("/api/reportes/generar", json=cuerpo, headers=encabezados)

    assert respuesta.status_code == 200
    assert respuesta.headers["content-type"] == "application/pdf"
    assert "attachment; filename=reporte-cribado.pdf" in respuesta.headers.get("content-disposition", "")
    assert len(respuesta.content) > 0
    assert respuesta.content.startswith(b"%PDF-")


def test_rechazo_cuando_riesgo_crisis_y_resultado_minimo_son_ambos_verdaderos(cliente_http):
    """Verifica que el modelo rechace (422) la inconsistencia clinica de resultado minimo con riesgo de crisis."""
    cuerpo = obtener_cuerpo_solicitud_valido()
    cuerpo["riesgo_crisis"] = True
    cuerpo["resultado_minimo"] = True

    encabezados = {"X-Clave-Interna": CLAVE_PRUEBA}
    respuesta = cliente_http.post("/api/reportes/generar", json=cuerpo, headers=encabezados)

    assert respuesta.status_code == 422
    detalles_error = str(respuesta.json())
    assert "riesgo de crisis" in detalles_error.lower()


def test_verificacion_endpoint_salud_disponible(cliente_http):
    """Verifica la respuesta del endpoint publico de monitoreo de salud."""
    respuesta = cliente_http.get("/salud")
    assert respuesta.status_code == 200
    assert respuesta.json() == {"estado": "disponible", "servicio": "reportes"}


def test_pdf_muestra_severidad_legible_y_orientacion_acorde(cliente_http):
    """Verifica que el PDF no filtre nombres internos de enum y que la orientacion corresponda a la severidad."""
    import io
    import pypdf

    cuerpo = obtener_cuerpo_solicitud_valido()
    cuerpo.update(
        {
            "puntaje_phq9": 27,
            "puntaje_gad7": 21,
            "severidad_depresion": "SEVERA",
            "severidad_ansiedad": "SEVERA",
            "riesgo_crisis": True,
        }
    )
    encabezados = {"X-Clave-Interna": CLAVE_PRUEBA}
    respuesta = cliente_http.post("/api/reportes/generar", json=cuerpo, headers=encabezados)
    assert respuesta.status_code == 200

    lector = pypdf.PdfReader(io.BytesIO(respuesta.content))
    texto = " ".join(pagina.extract_text() for pagina in lector.pages)

    assert "Enum" not in texto, "No deben aparecer nombres internos de enum en el reporte."
    assert "Severa" in texto, "La severidad debe mostrarse en lenguaje legible."
    assert "rango minimo" not in texto.lower(), "Un resultado severo no puede mostrar la orientacion de nivel minimo."
    assert "altamente prioritario" in texto.lower(), "Debe mostrarse la orientacion correspondiente a severidad severa."
