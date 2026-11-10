
import logging
import os
import secrets
import time
from enum import Enum
from typing import Any, Dict, List, Optional, Union

from fastapi import Depends, FastAPI, Header, HTTPException, Request, Response, status
from pydantic import BaseModel, ConfigDict, Field, model_validator

from generador_pdf import construir_reporte_pdf
from generador_pdf_tablero import construir_pdf_tablero
from tablero import construir_tablero
from datos_demo import generar_registros_demo, generar_actividad_demo

# Cargar variables de entorno locales de .env si existe
for _ruta_env in [os.path.join(os.path.dirname(__file__), ".env"), os.path.join(os.path.dirname(__file__), "..", "backend-capas", ".env")]:
    if os.path.exists(_ruta_env):
        with open(_ruta_env, "r", encoding="utf-8") as _f:
            for _linea in _f:
                _linea = _linea.strip()
                if _linea and not _linea.startswith("#") and "=" in _linea:
                    _k, _v = _linea.split("=", 1)
                    os.environ.setdefault(_k.strip(), _v.strip())

# Configuracion de registro anonimizado sin datos sensibles
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
)
registrador = logging.getLogger("servicio_reportes")

aplicacion = FastAPI(
    title="Microservicio de Reportes de Cribado Clinico",
    description="Servicio interno para generacion en memoria de reportes PDF orientativos en salud mental.",
    version="1.0.0",
)


@aplicacion.middleware("http")
async def registrar_solicitud(solicitud: Request, llamar_siguiente):
    """Middleware de registro limitado unicamente a metodo, ruta, codigo HTTP y tiempo de ejecucion en milisegundos."""
    tiempo_inicio = time.perf_counter()
    try:
        respuesta = await llamar_siguiente(solicitud)
        tiempo_ejecucion_ms = (time.perf_counter() - tiempo_inicio) * 1000
        registrador.info(
            "%s %s %d %.2f ms",
            solicitud.method,
            solicitud.url.path,
            respuesta.status_code,
            tiempo_ejecucion_ms,
        )
        return respuesta
    except Exception:
        tiempo_ejecucion_ms = (time.perf_counter() - tiempo_inicio) * 1000
        registrador.info(
            "%s %s 500 %.2f ms",
            solicitud.method,
            solicitud.url.path,
            tiempo_ejecucion_ms,
        )
        raise


class SeveridadDepresionEnum(str, Enum):
    """Niveles psicometricos estandarizados de PHQ-9 (orientativos, no diagnosticos)."""

    MINIMA = "MINIMA"
    LEVE = "LEVE"
    MODERADA = "MODERADA"
    MODERADAMENTE_SEVERA = "MODERADAMENTE_SEVERA"
    SEVERA = "SEVERA"


class SeveridadAnsiedadEnum(str, Enum):
    """Niveles psicometricos estandarizados de GAD-7 (orientativos, no diagnosticos)."""

    MINIMA = "MINIMA"
    LEVE = "LEVE"
    MODERADA = "MODERADA"
    SEVERA = "SEVERA"


class ModeloLineaAyuda(BaseModel):
    """Estructura de datos para un recurso o linea de contacto institucional en crisis."""

    nombre: str
    telefono: str
    horario: Optional[str] = "24/7"
    descripcion: Optional[str] = ""


class ModeloItemRespuesta(BaseModel):
    """Estructura fenomenologica para un item respondido en los cuestionarios."""

    instrumento: Optional[str] = ""
    item: Optional[int] = None
    texto: Optional[str] = None
    texto_sintoma: Optional[str] = None
    frecuencia: Optional[str] = None
    frecuencia_reportada: Optional[str] = None


class SolicitudReporte(BaseModel):
    """Modelo de entrada para la solicitud de generacion del reporte de cribado."""

    nombre_paciente: str
    fecha_evaluacion: str
    puntaje_phq9: int = Field(ge=0, le=27, description="Puntaje acumulado PHQ-9")
    puntaje_gad7: int = Field(ge=0, le=21, description="Puntaje acumulado GAD-7")
    severidad_depresion: SeveridadDepresionEnum
    severidad_ansiedad: SeveridadAnsiedadEnum
    riesgo_crisis: bool = False
    resultado_minimo: bool = False
    lineas_ayuda: List[ModeloLineaAyuda] = Field(default_factory=list)
    respuestas_resumen: List[Union[ModeloItemRespuesta, Dict[str, Any], str]] = Field(default_factory=list)

    @model_validator(mode="after")
    def validar_invariante_crisis_y_minimo(self) -> "SolicitudReporte":
        """Invariante de negocio: resultado_minimo nunca puede ser verdadero si riesgo_crisis lo es."""
        if self.riesgo_crisis and self.resultado_minimo:
            raise ValueError(
                "Inconsistencia en cribado: el resultado no puede catalogarse como minimo cuando existe riesgo de crisis."
            )
        return self


def verificar_clave_interna(
    x_clave_interna: Optional[str] = Header(None, alias="X-Clave-Interna"),
) -> str:
    """Valida la cabecera de seguridad interna contra la variable de entorno CLAVE_INTERNA."""
    clave_esperada = os.getenv("CLAVE_INTERNA", "")

    if not clave_esperada:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="No autorizado",
        )

    if not x_clave_interna:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="No autorizado",
        )

    if not secrets.compare_digest(x_clave_interna, clave_esperada):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="No autorizado",
        )

    return x_clave_interna


@aplicacion.get("/salud", tags=["Monitoreo"])
def verificar_salud():
    """Endpoint basico para verificacion de disponibilidad del microservicio."""
    return {"estado": "disponible", "servicio": "reportes"}


@aplicacion.post(
    "/api/reportes/generar",
    response_class=Response,
    summary="Generar reporte PDF de cribado",
    tags=["Reportes"],
)
def generar_reporte_pdf(
    solicitud: SolicitudReporte,
    clave_verificada: str = Depends(verificar_clave_interna),
) -> Response:
    """Genera en memoria un documento PDF clinico de exactamente 2 paginas.

    No persiste datos en disco ni almacena informacion confidencial en base de datos ni logs.
    """
    # Conversion a diccionario para el generador (mode="json" entrega los enums como texto: "SEVERA")
    datos_dict = solicitud.model_dump(mode="json")

    # Construccion del documento en memoria
    bytes_pdf = construir_reporte_pdf(datos_dict)

    return Response(
        content=bytes_pdf,
        media_type="application/pdf",
        headers={
            "Content-Disposition": "attachment; filename=reporte-cribado.pdf",
            "Content-Type": "application/pdf",
        },
    )


class RegistroEvaluacionAnonima(BaseModel):
    """Registro individual anonimizado de evaluacion para agregaciones del tablero."""

    model_config = ConfigDict(extra="forbid")

    fecha: str = Field(description="Fecha de la evaluación en formato YYYY-MM-DD (sin hora)")
    puntaje_phq9: int = Field(ge=0, le=27, description="Puntaje acumulado PHQ-9")
    puntaje_gad7: int = Field(ge=0, le=21, description="Puntaje acumulado GAD-7")
    severidad_depresion: SeveridadDepresionEnum
    severidad_ansiedad: SeveridadAnsiedadEnum
    riesgo_crisis: bool = False


class EventoActividadAnonima(BaseModel):
    """Evento anónimo de actividad del sistema para agregaciones del tablero."""

    model_config = ConfigDict(extra="forbid")

    fecha: str = Field(description="Fecha del evento en formato YYYY-MM-DD (sin hora)")
    tipo: str = Field(description="Tipo de evento de auditoría o actividad")


class SolicitudTablero(BaseModel):
    """Solicitud de agregacion y generacion del tablero estadistico administrativo."""

    model_config = ConfigDict(extra="forbid")

    registros: List[RegistroEvaluacionAnonima] = Field(
        default_factory=list,
        max_length=50000,
        description="Listado anonimizado de evaluaciones (maximo 50.000)",
    )
    eventos_actividad: List[EventoActividadAnonima] = Field(
        default_factory=list,
        max_length=200000,
        description="Listado anónimo de eventos de actividad (máximo 200.000)",
    )
    total_pacientes_registrados: Optional[int] = Field(
        default=0,
        ge=0,
        description="Conteo DISTINCT por SQL de pacientes registrados",
    )
    total_pacientes_con_evaluacion: Optional[int] = Field(
        default=0,
        ge=0,
        description="Conteo DISTINCT por SQL de pacientes con al menos una evaluación completada",
    )
    total_pacientes_con_cita_confirmada: Optional[int] = Field(
        default=0,
        ge=0,
        description="Conteo DISTINCT por SQL de pacientes con al menos una cita confirmada",
    )
    minimo_grupo: int = Field(
        default=5,
        ge=1,
        description="Tamano de umbral minimo de grupo (supresion de celdas pequeñas / grupo minimo N)",
    )
    demo: bool = Field(
        default=False,
        description="Activa el modo demostracion con datos sinteticos anonimos",
    )


@aplicacion.post(
    "/tablero",
    summary="Generar estadísticas y gráficos para tablero administrativo",
    tags=["Tablero"],
)
def generar_tablero(
    solicitud: SolicitudTablero,
    clave_verificada: str = Depends(verificar_clave_interna),
) -> Dict[str, Any]:
    """Calcula indicadores estadisticos agregados y genera los 8 graficos en base64.

    Rechaza estrictamente campos individuales o no permitidos (extra='forbid')
    y aplica la regla de privacidad de grupo minimo N.
    En modo demo (demo=True), genera registros y eventos sinteticos sin consultar datos reales.
    """
    if solicitud.demo:
        registros_demo = generar_registros_demo()
        eventos_demo, totales_demo = generar_actividad_demo()
        return construir_tablero(
            registros_demo,
            solicitud.minimo_grupo,
            es_demostracion=True,
            eventos_actividad=eventos_demo,
            totales_personas=totales_demo,
        )

    registros_dict = [r.model_dump(mode="json") for r in solicitud.registros]
    eventos_dict = [e.model_dump(mode="json") for e in solicitud.eventos_actividad]
    totales_dict = {
        "total_pacientes_registrados": solicitud.total_pacientes_registrados or 0,
        "total_pacientes_con_evaluacion": solicitud.total_pacientes_con_evaluacion or 0,
        "total_pacientes_con_cita_confirmada": solicitud.total_pacientes_con_cita_confirmada or 0,
    }
    return construir_tablero(
        registros_dict,
        solicitud.minimo_grupo,
        es_demostracion=False,
        eventos_actividad=eventos_dict,
        totales_personas=totales_dict,
    )


class SolicitudExportarPdfTablero(BaseModel):
    """Modelo de entrada para la generación en PDF del informe ejecutivo del tablero."""

    model_config = ConfigDict(extra="forbid")

    datos: Dict[str, Any] = Field(description="Estructura consolidada del tablero devuelta por /tablero")
    dias: int = Field(default=30, description="Periodo de días analizado (0, 7, 30, 90)")
    fecha_reporte: str = Field(default="", description="Fecha legible de generación del informe")


@aplicacion.post(
    "/tablero/exportar-pdf",
    response_class=Response,
    summary="Generar informe PDF del tablero estadístico administrativo",
    tags=["Tablero"],
)
def exportar_pdf_tablero(
    solicitud: SolicitudExportarPdfTablero,
    clave_verificada: str = Depends(verificar_clave_interna),
) -> Response:
    """Genera en memoria el documento PDF con tablas agregadas y gráficos del tablero."""
    bytes_pdf = construir_pdf_tablero(
        solicitud.datos,
        dias=solicitud.dias,
        fecha_reporte=solicitud.fecha_reporte,
    )

    es_demo = solicitud.datos.get("es_demostracion", False)
    prefijo = "DEMO_" if es_demo else ""

    return Response(
        content=bytes_pdf,
        media_type="application/pdf",
        headers={
            "Content-Disposition": f"attachment; filename={prefijo}tablero.pdf",
            "Content-Type": "application/pdf",
        },
    )


