package com.Cesde.triaje.Controlador;

import com.Cesde.triaje.Dto.RespuestaError;
import com.Cesde.triaje.Excepcion.ExcepcionAccesoDenegado;
import com.Cesde.triaje.Excepcion.ExcepcionCitaNoEncontrada;
import com.Cesde.triaje.Excepcion.ExcepcionCorreoYaRegistrado;
import com.Cesde.triaje.Excepcion.ExcepcionCredencialesInvalidas;
import com.Cesde.triaje.Excepcion.ExcepcionDatoInvalido;
import com.Cesde.triaje.Excepcion.ExcepcionDemasiadosIntentos;
import com.Cesde.triaje.Excepcion.ExcepcionDominio;
import com.Cesde.triaje.Excepcion.ExcepcionEvaluacionIncompleta;
import com.Cesde.triaje.Excepcion.ExcepcionEvaluacionNoEncontrada;
import com.Cesde.triaje.Excepcion.ExcepcionServicioReportesNoDisponible;
import com.Cesde.triaje.Excepcion.ExcepcionTokenRenovacionInvalido;
import com.Cesde.triaje.Excepcion.ExcepcionUsuarioNoEncontrado;
import com.Cesde.triaje.Seguridad.GestorCookieRenovacion;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class ManejadorGlobalExcepciones {

    private static final Logger REGISTRO = LoggerFactory.getLogger(ManejadorGlobalExcepciones.class);

    private final GestorCookieRenovacion gestorCookie;

    public ManejadorGlobalExcepciones(GestorCookieRenovacion gestorCookie) {
        this.gestorCookie = gestorCookie;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespuestaError> manejarValidacion(MethodArgumentNotValidException e, HttpServletRequest solicitud) {
        List<RespuestaError.DetalleError> detalles = e.getBindingResult().getFieldErrors().stream()
                .map(error -> new RespuestaError.DetalleError(error.getField(), error.getDefaultMessage()))
                .toList();
        return responder(HttpStatus.BAD_REQUEST, "SOLICITUD_INVALIDA",
                "La solicitud contiene datos no válidos.", solicitud, detalles);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RespuestaError> manejarCuerpoIlegible(HttpServletRequest solicitud) {
        return responder(HttpStatus.BAD_REQUEST, "CUERPO_INVALIDO",
                "El cuerpo de la solicitud no es válido.", solicitud, List.of());
    }

    @ExceptionHandler(ExcepcionDatoInvalido.class)
    public ResponseEntity<RespuestaError> manejarDatoInvalido(ExcepcionDatoInvalido e, HttpServletRequest solicitud) {
        return responder(HttpStatus.BAD_REQUEST, "DATO_INVALIDO", e.getMessage(), solicitud, List.of());
    }

    @ExceptionHandler(ExcepcionCorreoYaRegistrado.class)
    public ResponseEntity<RespuestaError> manejarCorreoDuplicado(ExcepcionCorreoYaRegistrado e, HttpServletRequest solicitud) {
        return responder(HttpStatus.CONFLICT, "CORREO_YA_REGISTRADO", e.getMessage(), solicitud, List.of());
    }

    @ExceptionHandler(ExcepcionCredencialesInvalidas.class)
    public ResponseEntity<RespuestaError> manejarCredenciales(ExcepcionCredencialesInvalidas e, HttpServletRequest solicitud) {
        return responder(HttpStatus.UNAUTHORIZED, "CREDENCIALES_INVALIDAS", e.getMessage(), solicitud, List.of());
    }

    @ExceptionHandler(ExcepcionTokenRenovacionInvalido.class)
    public ResponseEntity<RespuestaError> manejarTokenRenovacion(ExcepcionTokenRenovacionInvalido e, HttpServletRequest solicitud) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.SET_COOKIE, gestorCookie.eliminar().toString())
                .body(new RespuestaError(
                        Instant.now(),
                        HttpStatus.UNAUTHORIZED.value(),
                        "SESION_INVALIDA",
                        e.getMessage(),
                        solicitud.getRequestURI(),
                        List.of()
                ));
    }

    @ExceptionHandler(ExcepcionUsuarioNoEncontrado.class)
    public ResponseEntity<RespuestaError> manejarUsuarioNoEncontrado(ExcepcionUsuarioNoEncontrado e, HttpServletRequest solicitud) {
        return responder(HttpStatus.NOT_FOUND, "USUARIO_NO_ENCONTRADO", e.getMessage(), solicitud, List.of());
    }

    @ExceptionHandler(ExcepcionEvaluacionNoEncontrada.class)
    public ResponseEntity<RespuestaError> manejarEvaluacionNoEncontrada(ExcepcionEvaluacionNoEncontrada e, HttpServletRequest solicitud) {
        return responder(HttpStatus.NOT_FOUND, "EVALUACION_NO_ENCONTRADA", e.getMessage(), solicitud, List.of());
    }

    @ExceptionHandler(ExcepcionEvaluacionIncompleta.class)
    public ResponseEntity<RespuestaError> manejarEvaluacionIncompleta(ExcepcionEvaluacionIncompleta e, HttpServletRequest solicitud) {
        return responder(HttpStatus.BAD_REQUEST, "EVALUACION_INCOMPLETA", e.getMessage(), solicitud, List.of());
    }

    @ExceptionHandler(ExcepcionServicioReportesNoDisponible.class)
    public ResponseEntity<RespuestaError> manejarServicioReportesNoDisponible(ExcepcionServicioReportesNoDisponible e, HttpServletRequest solicitud) {
        return responder(HttpStatus.SERVICE_UNAVAILABLE, "SERVICIO_NO_DISPONIBLE",
                "El servicio de generación de reportes no está disponible en este momento. Por favor, intente más tarde.",
                solicitud, List.of());
    }

    @ExceptionHandler(ExcepcionAccesoDenegado.class)
    public ResponseEntity<RespuestaError> manejarAccesoDenegado(ExcepcionAccesoDenegado e, HttpServletRequest solicitud) {
        return responder(HttpStatus.FORBIDDEN, "ACCESO_DENEGADO", e.getMessage(), solicitud, List.of());
    }

    @ExceptionHandler(ExcepcionCitaNoEncontrada.class)
    public ResponseEntity<RespuestaError> manejarCitaNoEncontrada(ExcepcionCitaNoEncontrada e, HttpServletRequest solicitud) {
        return responder(HttpStatus.NOT_FOUND, "CITA_NO_ENCONTRADA", e.getMessage(), solicitud, List.of());
    }

    @ExceptionHandler(ExcepcionDominio.class)
    public ResponseEntity<RespuestaError> manejarDominio(ExcepcionDominio e, HttpServletRequest solicitud) {
        return responder(HttpStatus.CONFLICT, "REGLA_DE_NEGOCIO", e.getMessage(), solicitud, List.of());
    }

    @ExceptionHandler(ExcepcionDemasiadosIntentos.class)
    public ResponseEntity<RespuestaError> manejarDemasiadosIntentos(ExcepcionDemasiadosIntentos e, HttpServletRequest solicitud) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(e.getSegundosEspera()))
                .body(new RespuestaError(
                        Instant.now(),
                        HttpStatus.TOO_MANY_REQUESTS.value(),
                        "DEMASIADOS_INTENTOS",
                        "Demasiados intentos de inicio de sesión. Espera un momento e inténtalo de nuevo.",
                        solicitud.getRequestURI(),
                        List.of()
                ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<RespuestaError> manejarIntegridad(HttpServletRequest solicitud) {
        REGISTRO.warn("Violación de integridad de datos en {}", solicitud.getRequestURI());
        return responder(HttpStatus.CONFLICT, "CONFLICTO_DE_DATOS",
                "La operación entra en conflicto con datos existentes.", solicitud, List.of());
    }

    @ExceptionHandler({
            NoResourceFoundException.class,
            HttpRequestMethodNotSupportedException.class,
            HttpMediaTypeNotSupportedException.class,
            HttpMediaTypeNotAcceptableException.class,
            MissingServletRequestParameterException.class,
            ErrorResponseException.class
    })
    public ResponseEntity<RespuestaError> manejarErrorMvc(Exception e, HttpServletRequest solicitud) {
        HttpStatusCode codigo = ((ErrorResponse) e).getStatusCode();
        HttpStatus estado = HttpStatus.resolve(codigo.value());
        if (estado == null) {
            estado = HttpStatus.BAD_REQUEST;
        }
        String mensaje = switch (estado) {
            case NOT_FOUND -> "El recurso solicitado no existe.";
            case METHOD_NOT_ALLOWED -> "El método HTTP no está permitido para este recurso.";
            case UNSUPPORTED_MEDIA_TYPE -> "El tipo de contenido no es compatible.";
            case NOT_ACCEPTABLE -> "No se puede generar una respuesta en el formato solicitado.";
            default -> "La solicitud no pudo procesarse.";
        };
        return responder(estado, estado.name(), mensaje, solicitud, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespuestaError> manejarInesperado(Exception e, HttpServletRequest solicitud) {
        REGISTRO.error("Error interno no controlado ({}) en {}", e.getClass().getName(), solicitud.getRequestURI());
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO",
                "Ocurrió un error interno. Inténtalo más tarde.", solicitud, List.of());
    }

    private ResponseEntity<RespuestaError> responder(HttpStatus estado, String codigo, String mensaje,
                                                     HttpServletRequest solicitud,
                                                     List<RespuestaError.DetalleError> detalles) {
        return ResponseEntity.status(estado)
                .body(new RespuestaError(Instant.now(), estado.value(), codigo, mensaje, solicitud.getRequestURI(), detalles));
    }
}
