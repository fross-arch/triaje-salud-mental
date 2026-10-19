
import logging
import os
import secrets
import time
from enum import Enum
from typing import Any, Dict, List, Optional, Union

from fastapi import Depends, FastAPI, Header, HTTPException, Request, Response, status
from pydantic import BaseModel, Field, model_validator

from generador_pdf import construir_reporte_pdf

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
