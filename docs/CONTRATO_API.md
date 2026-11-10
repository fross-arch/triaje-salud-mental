# Contrato de la API REST: Triaje de Salud Mental

Este documento establece el contrato formal y exhaustivo de la API REST del sistema de **Triaje de Salud Mental**, garantizando compatibilidad total entre el frontend (`React + Vite`) y el backend (`Spring Boot`), tanto para la versión actual como para la futura implementación en arquitectura por capas (`backend-capas/`).

---

## 1. Seguridad, Cabeceras y Cookies

### 1.1 Autenticación y Autorización
- **Tipo de Seguridad**: Sin estado (*stateless*), basada en **JWT (JSON Web Token)** firmado con HMAC-SHA256 (`HS256`).
- **Cabecera de Peticiones Autenticadas**:
  ```http
  Authorization: Bearer <token_acceso>
  ```
- **Contenido del Token de Acceso (Claims)**:
  - `sub`: ID del usuario (`Long`, ej. `"1"`).
  - `rol`: Rol del usuario (`PACIENTE`, `CLINICO`, `ADMINISTRADOR`).
  - `iss`: `triaje-salud-mental`
  - `iat`: Timestamp de emisión.
  - `exp`: Timestamp de expiración.
  - *Privacidad*: Nunca incluye nombre, correo ni datos de salud.

### 1.2 Duración de Tokens y Renovación
| Elemento | Duración | Mecanismo de Almacenamiento / Envío |
| :--- | :--- | :--- |
| **Token de Acceso** | 15 minutos (`15m` = 900 segundos) | Memoria en frontend (`clienteHttp.js`), enviado en header `Authorization: Bearer`. |
| **Token de Renovación** | 7 días (`7d` = 604.800 segundos) | Cookie HTTP-Only gestionada por el navegador; hash SHA-256 en tabla `tokens_renovacion`. |

### 1.3 Especificación de la Cookie de Renovación
- **Nombre**: `token_renovacion`
- **Ruta (`Path`)**: `/api/autenticacion` (aislada exclusivamente a los endpoints de sesión).
- **HttpOnly**: `true` (inaccesible desde JavaScript / XSS).
- **SameSite**: `Strict` (protección contra ataques CSRF).
- **Secure**: `false` en desarrollo local (`http://localhost`), configurable a `true` mediante variable de entorno `COOKIE_SEGURA=true` en producción HTTPS.
- **Ciclo de vida**:
  - Al iniciar sesión o renovar: se emite con `maxAge = 7 días`.
  - Al cerrar sesión o recibir token inválido: se elimina enviando cookie vacía con `maxAge = 0`.

---

## 2. Formato Uniforme de Respuestas de Error

Toda petición fallida devuelve un objeto JSON estandarizado correspondiente a la clase `RespuestaError`.

### 2.1 Esquema JSON (`RespuestaError`)
```json
{
  "fecha": "2026-10-28T21:15:48.123Z",
  "estado": 400,
  "error": "SOLICITUD_INVALIDA",
  "mensaje": "La solicitud contiene datos no válidos.",
  "ruta": "/api/autenticacion/inicio-sesion",
  "detalles": [
    {
      "campo": "correo",
      "mensaje": "El correo es obligatorio."
    }
  ]
}
```
*Nota: El campo `detalles` se omite si la lista está vacía (`@JsonInclude(NON_EMPTY)`).*

### 2.2 Matriz de Excepciones y Códigos HTTP
| Excepción / Situación | Código HTTP | Valor `error` | Mensaje por Defecto / Origen |
| :--- | :---: | :--- | :--- |
| `MethodArgumentNotValidException` | `400 Bad Request` | `SOLICITUD_INVALIDA` | "La solicitud contiene datos no válidos." + lista `detalles` (campo y mensaje). |
| `HttpMessageNotReadableException` | `400 Bad Request` | `CUERPO_INVALIDO` | "El cuerpo de la solicitud no es válido." |
| `ExcepcionDatoInvalido` | `400 Bad Request` | `DATO_INVALIDO` | Mensaje dinámico de la excepción. |
| `ExcepcionEvaluacionIncompleta` | `400 Bad Request` | `EVALUACION_INCOMPLETA` | Mensaje de preguntas faltantes. |
| `ExcepcionCredencialesInvalidas` | `401 Unauthorized` | `CREDENCIALES_INVALIDAS` | "Credenciales inválidas." |
| `ExcepcionTokenRenovacionInvalido` | `401 Unauthorized` | `SESION_INVALIDA` | Mensaje de sesión expirada/revocada (borra cookie). |
| Sin autenticación / JWT vencido (`AuthenticationEntryPoint`) | `401 Unauthorized` | `NO_AUTENTICADO` | "Debes iniciar sesión para acceder a este recurso." |
| Rol no autorizado (`AccessDeniedHandler`) | `403 Forbidden` | `ACCESO_DENEGADO` | "No tienes permiso para acceder a este recurso." |
| `ExcepcionAccesoDenegado` | `403 Forbidden` | `ACCESO_DENEGADO` | Mensaje dinámico de la excepción. |
| `ExcepcionUsuarioNoEncontrado` | `404 Not Found` | `USUARIO_NO_ENCONTRADO` | "El usuario no existe." |
| `ExcepcionEvaluacionNoEncontrada` | `404 Not Found` | `EVALUACION_NO_ENCONTRADA` | "La evaluación no existe o no tienes acceso a ella." |
| `ExcepcionCitaNoEncontrada` | `404 Not Found` | `CITA_NO_ENCONTRADA` | "La cita solicitada no existe." |
| `ExcepcionCorreoYaRegistrado` | `409 Conflict` | `CORREO_YA_REGISTRADO` | "El correo ya se encuentra registrado." |
| `ExcepcionDominio` | `409 Conflict` | `REGLA_DE_NEGOCIO` | Mensaje descriptivo de la regla infringida. |
| `DataIntegrityViolationException` | `409 Conflict` | `CONFLICTO_DE_DATOS` | "La operación entra en conflicto con datos existentes." |
| `ExcepcionDemasiadosIntentos` | `429 Too Many Requests`| `DEMASIADOS_INTENTOS` | "Demasiados intentos de inicio de sesión. Espera un momento e inténtalo de nuevo." (Header `Retry-After`). |
| Recurso / Método no soportado (`NoResourceFoundException`, etc.) | `404 / 405 / 415` | `NOT_FOUND` / etc. | "El recurso solicitado no existe." / mensaje acorde. |
| `ExcepcionServicioReportesNoDisponible` | `503 Service Unavailable`| `SERVICIO_NO_DISPONIBLE` | "El servicio de generación de reportes no está disponible en este momento. Por favor, intente más tarde." |
| `Exception` (Error no controlado) | `500 Internal Server Error`| `ERROR_INTERNO` | "Ocurrió un error interno. Inténtalo más tarde." |

---

## 3. Matriz de Controladores y Endpoints

### 3.1 ControladorAutenticacion (`/api/autenticacion`)

| Método | Ruta | Rol Requerido | JSON de Entrada | JSON de Salida | Código Estado | Posibles Errores |
| :--- | :--- | :--- | :--- | :--- | :---: | :--- |
| `POST` | `/api/autenticacion/registro` | Permitido a todos (`permitAll`) | `SolicitudRegistro`:<br>• `nombreCompleto` (string, max 120)<br>• `correo` (email, max 120)<br>• `contrasena` (string, 10-72 car.) | `RespuestaUsuario`:<br>• `id` (long)<br>• `nombreCompleto` (string)<br>• `correo` (string)<br>• `rol` (string: "PACIENTE")<br>• `especialidad` (null) | `201 Created` | `400 SOLICITUD_INVALIDA`<br>`409 CORREO_YA_REGISTRADO`<br>`429 DEMASIADOS_INTENTOS` |
| `POST` | `/api/autenticacion/inicio-sesion` | Permitido a todos (`permitAll`) | `SolicitudInicioSesion`:<br>• `correo` (email, max 120)<br>• `contrasena` (string, max 72) | `RespuestaToken`:<br>• `tokenAcceso` (string JWT)<br>• `tipo` ("Bearer")<br>• `expiraEnSegundos` (long: 900)<br>*(Header Set-Cookie: token_renovacion)* | `200 OK` | `400 SOLICITUD_INVALIDA`<br>`401 CREDENCIALES_INVALIDAS`<br>`429 DEMASIADOS_INTENTOS` |
| `POST` | `/api/autenticacion/renovar` | Permitido a todos (`permitAll`) | *Vacío (lee cookie `token_renovacion`)* | `RespuestaToken`:<br>• `tokenAcceso` (string JWT)<br>• `tipo` ("Bearer")<br>• `expiraEnSegundos` (long: 900)<br>*(Header Set-Cookie: token_renovacion nuevo)* | `200 OK` | `401 SESION_INVALIDA`<br>`429 DEMASIADOS_INTENTOS` |
| `POST` | `/api/autenticacion/cierre-sesion` | Permitido a todos (`permitAll`) | *Vacío (lee cookie `token_renovacion`)* | *Vacío*<br>*(Header Set-Cookie: token_renovacion borrado)* | `204 No Content` | `500 ERROR_INTERNO` |

---

### 3.2 ControladorUsuarios (`/api/usuarios`)

| Método | Ruta | Rol Requerido | JSON de Entrada | JSON de Salida | Código Estado | Posibles Errores |
| :--- | :--- | :--- | :--- | :--- | :---: | :--- |
| `GET` | `/api/usuarios/yo` | Autenticado (`PACIENTE`, `CLINICO`, `ADMINISTRADOR`) | *Ninguno* | `RespuestaUsuario`:<br>• `id` (long)<br>• `nombreCompleto` (string)<br>• `correo` (string)<br>• `rol` (string)<br>• `especialidad` (string o null) | `200 OK` | `401 NO_AUTENTICADO`<br>`404 USUARIO_NO_ENCONTRADO` |
| `PUT` | `/api/usuarios/yo` | Autenticado (`PACIENTE`, `CLINICO`, `ADMINISTRADOR`) | `SolicitudActualizarPerfil`:<br>• `nombreCompleto` (string, max 120)<br>• `correo` (email, max 120)<br>• `especialidad` (string opcional, max 100)<br>• `contrasenaActual` (string opcional)<br>• `nuevaContrasena` (string opcional, min 10) | `RespuestaUsuario`:<br>• `id` (long)<br>• `nombreCompleto` (string)<br>• `correo` (string)<br>• `rol` (string)<br>• `especialidad` (string o null) | `200 OK` | `400 SOLICITUD_INVALIDA`<br>`400 DATO_INVALIDO`<br>`401 NO_AUTENTICADO`<br>`409 CORREO_YA_REGISTRADO` |
| `DELETE` | `/api/usuarios/yo` | Autenticado (`PACIENTE`, `CLINICO`, `ADMINISTRADOR`) | *Ninguno* | *Vacío*<br>*(Header Set-Cookie: token_renovacion borrado)* | `204 No Content` | `401 NO_AUTENTICADO`<br>`404 USUARIO_NO_ENCONTRADO` |

---

### 3.3 ControladorConsentimientos (`/api/consentimientos`)

| Método | Ruta | Rol Requerido | JSON de Entrada | JSON de Salida | Código Estado | Posibles Errores |
| :--- | :--- | :--- | :--- | :--- | :---: | :--- |
| `POST` | `/api/consentimientos` | `PACIENTE` | `SolicitudConsentimiento`:<br>• `confirmaMayoriaEdad` (boolean, debe ser `true`) | `RespuestaConsentimiento`:<br>• `id` (long)<br>• `versionConsentimiento` ("1.0")<br>• `versionPolitica` ("1.0")<br>• `confirmaMayoriaEdad` (true)<br>• `fechaAceptacion` (timestamp)<br>• `fechaRevocacion` (null)<br>• `vigente` (true) | `201 Created` | `400 SOLICITUD_INVALIDA`<br>`400 DATO_INVALIDO`<br>`401 NO_AUTENTICADO`<br>`403 ACCESO_DENEGADO` |
| `GET` | `/api/consentimientos/vigente` | `PACIENTE` | *Ninguno* | `RespuestaConsentimientoVigente`:<br>• `vigente` (boolean)<br>• `fechaAceptacion` (timestamp o null)<br>• `versionConsentimiento` (string o null) | `200 OK` | `401 NO_AUTENTICADO`<br>`403 ACCESO_DENEGADO` |
| `POST` | `/api/consentimientos/revocar` | `PACIENTE` | *Ninguno* (cuerpo `{}`) | *Vacío* | `200 OK` | `401 NO_AUTENTICADO`<br>`403 ACCESO_DENEGADO`<br>`409 REGLA_DE_NEGOCIO` *(si hay evaluación EN_CURSO)* |

---

### 3.4 ControladorTriaje (`/api/triaje/evaluaciones`)

| Método | Ruta | Rol Requerido | JSON de Entrada | JSON de Salida | Código Estado | Posibles Errores |
| :--- | :--- | :--- | :--- | :--- | :---: | :--- |
| `POST` | `/api/triaje/evaluaciones` | `PACIENTE` | *Ninguno* (cuerpo `{}`) | `RespuestaEvaluacionResumen`:<br>• `id` (UUID string)<br>• `estado` ("EN_CURSO")<br>• `puntajePhq9` (null)<br>• `puntajeGad7` (null)<br>• `severidadDepresion` (null)<br>• `severidadAnsiedad` (null)<br>• `riesgoCrisis` (false)<br>• `resultadoMinimo` (null)<br>• `fechaCreacion` (timestamp)<br>• `fechaFinalizacion` (null) | `201 Created` | `401 NO_AUTENTICADO`<br>`403 ACCESO_DENEGADO` *(sin consentimiento)*<br>`409 REGLA_DE_NEGOCIO` *(cita activa pendiente o evaluación compartida pendiente)* |
| `PUT` | `/api/triaje/evaluaciones/{id}/respuestas` | `PACIENTE` | `SolicitudRespuesta`:<br>• `instrumento` ("PHQ9" o "GAD7")<br>• `indicePregunta` (int: 1 a 9)<br>• `valorPuntaje` (int: 0 a 3) | `RespuestaRegistroItem`:<br>• `evaluacionId` (UUID string)<br>• `instrumento` (string)<br>• `indicePregunta` (int)<br>• `riesgoCrisis` (boolean) | `200 OK` | `400 SOLICITUD_INVALIDA`<br>`401 NO_AUTENTICADO`<br>`404 EVALUACION_NO_ENCONTRADA`<br>`409 REGLA_DE_NEGOCIO` *(evaluación ya cerrada/abandonada)* |
| `POST` | `/api/triaje/evaluaciones/{id}/finalizar` | `PACIENTE` | *Ninguno* (cuerpo `{}`) | `RespuestaEvaluacionResumen`:<br>• `id` (UUID string)<br>• `estado` ("COMPLETADA")<br>• `puntajePhq9` (int 0-27)<br>• `puntajeGad7` (int 0-21)<br>• `severidadDepresion` (enum string)<br>• `severidadAnsiedad` (enum string)<br>• `riesgoCrisis` (boolean)<br>• `resultadoMinimo` (boolean)<br>• `fechaCreacion` (timestamp)<br>• `fechaFinalizacion` (timestamp) | `200 OK` | `400 EVALUACION_INCOMPLETA`<br>`401 NO_AUTENTICADO`<br>`404 EVALUACION_NO_ENCONTRADA`<br>`409 REGLA_DE_NEGOCIO` |
| `GET` | `/api/triaje/evaluaciones` | `PACIENTE` | *Ninguno* | `List<RespuestaEvaluacionResumen>` | `200 OK` | `401 NO_AUTENTICADO` |
| `GET` | `/api/triaje/evaluaciones/{id}` | `PACIENTE` | *Ninguno* | `RespuestaEvaluacionDetalle`:<br>• `evaluacion` (`RespuestaEvaluacionResumen`)<br>• `respuestas`: array de `RespuestaItemDetalle` (`instrumento`, `indicePregunta`, `valorPuntaje`) | `200 OK` | `401 NO_AUTENTICADO`<br>`404 EVALUACION_NO_ENCONTRADA` |
| `GET` | `/api/triaje/evaluaciones/{id}/pdf` | `PACIENTE` | *Ninguno* | Binario (`byte[]`, `application/pdf`) | `200 OK` | `401 NO_AUTENTICADO`<br>`404 EVALUACION_NO_ENCONTRADA`<br>`503 SERVICIO_NO_DISPONIBLE` |
| `POST` | `/api/triaje/evaluaciones/{id}/autorizaciones` | `PACIENTE` | `SolicitudOtorgarAutorizacion`:<br>• `clinicoId` (long)<br>• `confirmaAutorizacion` (boolean, true) | `RespuestaAutorizacion`:<br>• `id` (UUID string)<br>• `evaluacionId` (UUID string)<br>• `clinicoId` (long)<br>• `versionTextoAutorizacion` ("1.0")<br>• `fechaOtorgamiento` (timestamp)<br>• `fechaRevocacion` (null)<br>• `vigente` (true)<br>• `estado` ("AUTORIZADA") | `201 Created` | `400 SOLICITUD_INVALIDA`<br>`400 DATO_INVALIDO`<br>`400 EVALUACION_INCOMPLETA`<br>`401 NO_AUTENTICADO`<br>`404 USUARIO_NO_ENCONTRADO`<br>`404 EVALUACION_NO_ENCONTRADA`<br>`409 REGLA_DE_NEGOCIO` |
| `GET` | `/api/triaje/evaluaciones/{id}/autorizaciones` | `PACIENTE` | *Ninguno* | `List<RespuestaAutorizacion>` | `200 OK` | `401 NO_AUTENTICADO`<br>`404 EVALUACION_NO_ENCONTRADA` |
| `POST` | `/api/triaje/evaluaciones/{id}/autorizaciones/{clinicoId}/revocar` | `PACIENTE` | *Ninguno* (cuerpo `{}`) | `RespuestaAutorizacion`:<br>• `id` (UUID string)<br>• `evaluacionId` (UUID string)<br>• `clinicoId` (long)<br>• `versionTextoAutorizacion` (string)<br>• `fechaOtorgamiento` (timestamp)<br>• `fechaRevocacion` (timestamp)<br>• `vigente` (false)<br>• `estado` ("REVOCADA") | `200 OK` | `401 NO_AUTENTICADO`<br>`404 EVALUACION_NO_ENCONTRADA` |

---

### 3.5 ControladorClinicos (`/api/clinicos`)

| Método | Ruta | Rol Requerido | JSON de Entrada | JSON de Salida | Código Estado | Posibles Errores |
| :--- | :--- | :--- | :--- | :--- | :---: | :--- |
| `GET` | `/api/clinicos` | `PACIENTE` | *Ninguno* | `List<RespuestaClinicoActivo>`:<br>[<br>  • `id` (long)<br>  • `nombreCompleto` (string)<br>  • `especialidad` (string o null)<br>] | `200 OK` | `401 NO_AUTENTICADO`<br>`403 ACCESO_DENEGADO` |

---

### 3.6 ControladorCitas (`/api/citas` - Portal Paciente)

| Método | Ruta | Rol Requerido | JSON de Entrada | JSON de Salida | Código Estado | Posibles Errores |
| :--- | :--- | :--- | :--- | :--- | :---: | :--- |
| `GET` | `/api/citas` | `PACIENTE` | *Ninguno* | `List<RespuestaCitaPaciente>`:<br>[<br>  • `id` (UUID string)<br>  • `fechaHora` (timestamp)<br>  • `modalidad` ("PRESENCIAL"\|"VIRTUAL")<br>  • `mensaje` (string o null)<br>  • `estado` (enum: "PROPUESTA", "CONFIRMADA", "RECHAZADA", "CANCELADA", "CUMPLIDA")<br>  • `observaciones` (string o null)<br>  • `nombreClinico` (string)<br>] | `200 OK` | `401 NO_AUTENTICADO`<br>`403 ACCESO_DENEGADO` |
| `POST` | `/api/citas/{id}/confirmar` | `PACIENTE` | *Ninguno* (cuerpo `{}`) | `RespuestaCitaPaciente` (con `estado` = "CONFIRMADA") | `200 OK` | `401 NO_AUTENTICADO`<br>`404 CITA_NO_ENCONTRADA`<br>`409 REGLA_DE_NEGOCIO` |
| `POST` | `/api/citas/{id}/rechazar` | `PACIENTE` | *Ninguno* (cuerpo `{}`) | `RespuestaCitaPaciente` (con `estado` = "RECHAZADA") | `200 OK` | `401 NO_AUTENTICADO`<br>`404 CITA_NO_ENCONTRADA`<br>`409 REGLA_DE_NEGOCIO` |
| `POST` | `/api/citas/{id}/cancelar` | `PACIENTE` | *Ninguno* (cuerpo `{}`) | `RespuestaCitaPaciente` (con `estado` = "CANCELADA") | `200 OK` | `401 NO_AUTENTICADO`<br>`404 CITA_NO_ENCONTRADA`<br>`409 REGLA_DE_NEGOCIO` |

---

### 3.7 ControladorClinicoCitas (`/api/clinico/citas` - Portal Clínico)

| Método | Ruta | Rol Requerido | JSON de Entrada | JSON de Salida | Código Estado | Posibles Errores |
| :--- | :--- | :--- | :--- | :--- | :---: | :--- |
| `POST` | `/api/clinico/citas` | `CLINICO` | `SolicitudProponerCita`:<br>• `evaluacionId` (UUID string obligatorio)<br>• `fechaHora` (timestamp futuro obligatorio)<br>• `modalidad` ("PRESENCIAL"\|"VIRTUAL")<br>• `mensaje` (string opcional, max 300) | `RespuestaCitaClinico`:<br>• `id` (UUID string)<br>• `fechaHora` (timestamp)<br>• `modalidad` (string)<br>• `mensaje` (string o null)<br>• `estado` ("PROPUESTA")<br>• `observaciones` (null)<br>• `nombrePaciente` (string) | `201 Created` | `400 SOLICITUD_INVALIDA`<br>`400 DATO_INVALIDO`<br>`401 NO_AUTENTICADO`<br>`403 ACCESO_DENEGADO`<br>`404 EVALUACION_NO_ENCONTRADA` *(sin autoriz. vigente)*<br>`409 REGLA_DE_NEGOCIO` *(conflicto de horario o cita activa)* |
| `GET` | `/api/clinico/citas` | `CLINICO` | *Ninguno* | `List<RespuestaCitaClinico>` | `200 OK` | `401 NO_AUTENTICADO`<br>`403 ACCESO_DENEGADO` |
| `POST` | `/api/clinico/citas/{id}/cumplir` | `CLINICO` | `SolicitudCumplirCita`:<br>• `observaciones` (string obligatorio, 1-1000 car.) | `RespuestaCitaClinico` (con `estado` = "CUMPLIDA", `observaciones`) | `200 OK` | `400 SOLICITUD_INVALIDA`<br>`400 DATO_INVALIDO`<br>`401 NO_AUTENTICADO`<br>`404 CITA_NO_ENCONTRADA`<br>`409 REGLA_DE_NEGOCIO` |
| `POST` | `/api/clinico/citas/{id}/cancelar` | `CLINICO` | `SolicitudCancelarCitaClinico` (opcional):<br>• `observaciones` o `motivo` (string, max 1000, obligatorio para clínico) | `RespuestaCitaClinico` (con `estado` = "CANCELADA", `observaciones`) | `200 OK` | `400 DATO_INVALIDO`<br>`401 NO_AUTENTICADO`<br>`404 CITA_NO_ENCONTRADA`<br>`409 REGLA_DE_NEGOCIO` |
| `PUT` | `/api/clinico/citas/{id}/posponer` | `CLINICO` | `SolicitudPosponerCita`:<br>• `nuevaFechaHora` (timestamp futuro obligatorio)<br>• `mensaje` (string opcional, max 300)<br>• `observaciones` (string opcional, max 1000) | `RespuestaCitaClinico` (con nueva `fechaHora` y `observaciones`) | `200 OK` | `400 SOLICITUD_INVALIDA`<br>`400 DATO_INVALIDO`<br>`401 NO_AUTENTICADO`<br>`404 CITA_NO_ENCONTRADA`<br>`409 REGLA_DE_NEGOCIO` *(si no está en PROPUESTA o CONFIRMADA)* |

---

### 3.8 ControladorClinicoEvaluaciones (`/api/clinico/evaluaciones`)

| Método | Ruta | Rol Requerido | JSON de Entrada | JSON de Salida | Código Estado | Posibles Errores |
| :--- | :--- | :--- | :--- | :--- | :---: | :--- |
| `GET` | `/api/clinico/evaluaciones` | `CLINICO` | *Ninguno* | `List<RespuestaEvaluacionClinicoResumen>`:<br>[<br>  • `evaluacionId` (UUID string)<br>  • `nombrePaciente` (string)<br>  • `fechaFinalizacion` (timestamp)<br>  • `severidadDepresion` (enum string)<br>  • `severidadAnsiedad` (enum string)<br>  • `riesgoCrisis` (boolean)<br>  • `resultadoMinimo` (boolean)<br>] | `200 OK` | `401 NO_AUTENTICADO`<br>`403 ACCESO_DENEGADO` |
| `GET` | `/api/clinico/evaluaciones/{id}` | `CLINICO` | *Ninguno* | `RespuestaEvaluacionClinicoDetalle`:<br>• `evaluacionId`, `nombrePaciente`, `fechaFinalizacion`<br>• `puntajePhq9`, `puntajeGad7`<br>• `severidadDepresion`, `severidadAnsiedad`<br>• `riesgoCrisis`, `resultadoMinimo`<br>• `respuestas`: array de items (`instrumento`, `indicePregunta`, `enunciadoPregunta`, `valorPuntaje`, `textoFrecuencia`) | `200 OK` | `401 NO_AUTENTICADO`<br>`403 ACCESO_DENEGADO`<br>`404 EVALUACION_NO_ENCONTRADA` *(sin autoriz. vigente)* |
| `GET` | `/api/clinico/evaluaciones/{id}/pdf` | `CLINICO` | *Ninguno* | Binario (`byte[]`, `application/pdf`) | `200 OK` | `401 NO_AUTENTICADO`<br>`403 ACCESO_DENEGADO`<br>`404 EVALUACION_NO_ENCONTRADA`<br>`503 SERVICIO_NO_DISPONIBLE` |

---

### 3.9 ControladorAdministracion (`/api/administracion/clinicos`)

| Método | Ruta | Rol Requerido | JSON de Entrada | JSON de Salida | Código Estado | Posibles Errores |
| :--- | :--- | :--- | :--- | :--- | :---: | :--- |
| `GET` | `/api/administracion/clinicos` | `ADMINISTRADOR` | *Ninguno* | `List<RespuestaClinico>`:<br>[<br>  • `id` (long)<br>  • `nombreCompleto` (string)<br>  • `correo` (string)<br>  • `rol` ("CLINICO")<br>  • `activo` (boolean)<br>  • `especialidad` (string o null)<br>] | `200 OK` | `401 NO_AUTENTICADO`<br>`403 ACCESO_DENEGADO` |
| `POST` | `/api/administracion/clinicos` | `ADMINISTRADOR` | `SolicitudCrearClinico`:<br>• `nombreCompleto` (string, max 120)<br>• `correo` (email, max 120)<br>• `contrasenaInicial` (string, 10-72 car.)<br>• `especialidad` (string opcional, max 100) | `RespuestaClinico` | `201 Created` | `400 SOLICITUD_INVALIDA`<br>`401 NO_AUTENTICADO`<br>`403 ACCESO_DENEGADO`<br>`409 CORREO_YA_REGISTRADO` |
| `PUT` | `/api/administracion/clinicos/{id}` | `ADMINISTRADOR` | `SolicitudActualizarClinico`:<br>• `nombreCompleto` (string, max 120)<br>• `correo` (email, max 120)<br>• `especialidad` (string opcional, max 100)<br>• `nuevaContrasena` (string opcional, min 10) | `RespuestaClinico` | `200 OK` | `400 SOLICITUD_INVALIDA`<br>`401 NO_AUTENTICADO`<br>`403 ACCESO_DENEGADO`<br>`404 USUARIO_NO_ENCONTRADO`<br>`409 CORREO_YA_REGISTRADO` |
| `DELETE` | `/api/administracion/clinicos/{id}` | `ADMINISTRADOR` | *Ninguno* | *Vacío* | `204 No Content` | `401 NO_AUTENTICADO`<br>`403 ACCESO_DENEGADO`<br>`404 USUARIO_NO_ENCONTRADO` |
| `POST` | `/api/administracion/clinicos/{id}/activar` | `ADMINISTRADOR` | *Ninguno* (cuerpo `{}`) | `RespuestaClinico` (con `activo` = true) | `200 OK` | `401 NO_AUTENTICADO`<br>`403 ACCESO_DENEGADO`<br>`404 USUARIO_NO_ENCONTRADO` |
| `POST` | `/api/administracion/clinicos/{id}/desactivar` | `ADMINISTRADOR` | *Ninguno* (cuerpo `{}`) | `RespuestaClinico` (con `activo` = false) | `200 OK` | `401 NO_AUTENTICADO`<br>`403 ACCESO_DENEGADO`<br>`404 USUARIO_NO_ENCONTRADO` |
| `GET` | `/api/administracion/tablero` | `ADMINISTRADOR` | *Query param `dias`* (int: 0, 7, 30, 90; defecto 30) | `RespuestaTablero`:<br>• `datos_insuficientes` (boolean)<br>• `mensaje` (string o null)<br>• `total_evaluaciones` (int)<br>• `minimo_requerido` (int)<br>• `es_demostracion` (boolean)<br>• `indicadores` (`total_evaluaciones`, `media_phq9`, `mediana_phq9`, `desviacion_phq9`, `media_gad7`, `mediana_gad7`, `desviacion_gad7`, `correlacion_phq9_gad7`, `total_crisis`, `porcentaje_crisis`, `minimo_grupo`)<br>• `graficos` (`severidad_phq9`, `severidad_gad7`, `evaluaciones_semana`, `phq9_vs_gad7` en PNG Base64)<br>• `actividad` (`indicadores`: `total_pacientes_registrados`, `total_pacientes_con_evaluacion`, `total_pacientes_con_cita_confirmada`, `citas_propuestas`, `citas_confirmadas`, `citas_rechazadas`, `citas_cumplidas`, `citas_canceladas`, `citas_pospuestas`, `clinicos_altas`, `clinicos_activados`, `clinicos_desactivados`, `clinicos_eliminados`, `consentimientos_aceptados`, `consentimientos_revocados`, `autorizaciones_otorgadas`, `autorizaciones_revocadas`, `cuentas_eliminadas`; `graficos`: `actividad_semanal_pacientes`, `estado_citas`, `gestion_clinicos`, `comparativa_pacientes` en PNG Base64) | `200 OK` | `400 DATO_INVALIDO`<br>`401 NO_AUTENTICADO`<br>`403 ACCESO_DENEGADO`<br>`503 SERVICIO_NO_DISPONIBLE` |
| `GET` | `/api/administracion/tablero/exportar` | `ADMINISTRADOR` | *Query params*:<br>• `dias` (int: 0, 7, 30, 90; defecto 30)<br>• `formato` (string: "csv" \| "pdf", requerido) | Archivo descargable binario o de texto plano:<br>• CSV: `text/csv;charset=utf-8` con BOM `\uFEFF`, `;` y sanitización contra inyección de fórmulas (sin datos individuales).<br>• PDF: `application/pdf` generado con ReportLab en Python con tablas consolidadas, 8 gráficos y nota de confidencialidad.<br>*Audita acción `TABLERO_EXPORTADO`*. | `200 OK` | `400 DATO_INVALIDO`<br>`401 NO_AUTENTICADO`<br>`403 ACCESO_DENEGADO`<br>`503 SERVICIO_NO_DISPONIBLE` |

---

### 3.10 ControladorAdministracionPacientes (`/api/administracion/pacientes`)

| Método | Ruta | Rol Requerido | JSON de Entrada | JSON de Salida | Código Estado | Posibles Errores |
| :--- | :--- | :--- | :--- | :--- | :---: | :--- |
| `GET` | `/api/administracion/pacientes` | `ADMINISTRADOR` | *Ninguno* | `List<RespuestaPacienteAdministracion>`:<br>[<br>  • `id` (long)<br>  • `nombre` (string)<br>  • `correo` (string)<br>  • `fechaRegistro` (timestamp)<br>  • `activo` (boolean)<br>] | `200 OK` | `401 NO_AUTENTICADO`<br>`403 ACCESO_DENEGADO` |
| `PUT` | `/api/administracion/pacientes/{id}` | `ADMINISTRADOR` | `SolicitudActualizarPaciente`:<br>• `nombreCompleto` (string, max 120)<br>• `correo` (email, max 120)<br>• `nuevaContrasena` (string opcional, min 10) | `RespuestaPacienteAdministracion` | `200 OK` | `400 SOLICITUD_INVALIDA`<br>`401 NO_AUTENTICADO`<br>`403 ACCESO_DENEGADO`<br>`404 USUARIO_NO_ENCONTRADO`<br>`409 CORREO_YA_REGISTRADO` |

---

### 3.11 ControladorRecursosAyuda (`/api/recursos-ayuda`)

| Método | Ruta | Rol Requerido | JSON de Entrada | JSON de Salida | Código Estado | Posibles Errores |
| :--- | :--- | :--- | :--- | :--- | :---: | :--- |
| `GET` | `/api/recursos-ayuda` | Permitido a todos (`permitAll`) | *Ninguno* | `List<RespuestaRecursoAyuda>`:<br>[<br>  • `nombre` (string)<br>  • `telefono` (string)<br>  • `horario` (string)<br>  • `descripcion` (string)<br>  • `ambito` (string)<br>] | `200 OK` | *Ninguno* (estático en memoria) |

---

## 4. Comparación de Rutas: Backend vs Frontend

Se ha realizado una validación exhaustiva cruzando todos los servicios en `frontend/src/servicios/` con los controladores de Spring Boot.

### 4.1 Resultados de la Conciliación
- **Rutas invocadas por el Front que NO existen en el Backend**: **0** (Ninguna).
- **Rutas expuestas en el Backend que NO se usen en el Front**: **0** (Ninguna).
- **Correspondencia**: **100% idéntica**. 29 endpoints en frontend coinciden en método HTTP, ruta relativa y parámetros con los 29 métodos de los controladores del backend (incluyendo `servicioTablero.js` para `GET /api/administracion/tablero` y `GET /api/administracion/tablero/exportar`).

---

## 5. Esquema de Base de Datos y Entidades

Base de datos relacional MariaDB / MySQL (`triaje_salud_mental_db`).
A continuación se detallan las 8 tablas resultantes de las migraciones V1 a V6:

### 5.1 Tabla `usuarios`
| Columna | Tipo de Dato | Nulo | Restricciones / Valor por Defecto | Descripción |
| :--- | :--- | :---: | :--- | :--- |
| `id` | `BIGINT` | NO | `AUTO_INCREMENT`, `PRIMARY KEY` | Identificador único numérico |
| `nombre_completo`| `VARCHAR(120)` | NO | | Nombre y apellido del usuario |
| `correo` | `VARCHAR(120)` | NO | `UNIQUE` | Correo electrónico normalizado (lowercase) |
| `contrasena_hash`| `VARCHAR(255)` | NO | | Hash BCrypt (fuerza 12) de la contraseña |
| `rol` | `ENUM` | NO | `'PACIENTE'`, `'CLINICO'`, `'ADMINISTRADOR'`. DEFAULT `'PACIENTE'` | Rol de acceso en el sistema |
| `activo` | `BOOLEAN` | NO | DEFAULT `TRUE` | Estado de la cuenta (activa / desactivada) |
| `especialidad` | `VARCHAR(100)` | SÍ | DEFAULT `NULL` | Especialidad clínica (solo aplica a rol CLINICO) |
| `fecha_creacion` | `TIMESTAMP` | NO | DEFAULT `CURRENT_TIMESTAMP` | Momento de registro de la cuenta |

### 5.2 Tabla `consentimientos`
| Columna | Tipo de Dato | Nulo | Restricciones / Valor por Defecto | Descripción |
| :--- | :--- | :---: | :--- | :--- |
| `id` | `BIGINT` | NO | `AUTO_INCREMENT`, `PRIMARY KEY` | Identificador del registro de consentimiento |
| `usuario_id` | `BIGINT` | NO | `FOREIGN KEY (usuarios.id) ON DELETE CASCADE` | Paciente que otorga el consentimiento |
| `version_consentimiento` | `VARCHAR(20)` | NO | | Versión del texto del consentimiento (ej: "1.0") |
| `version_politica` | `VARCHAR(20)` | NO | | Versión de la política de datos (ej: "1.0") |
| `confirma_mayoria_edad` | `BOOLEAN` | NO | `CHECK (confirma_mayoria_edad = TRUE)` | Confirmación legal de tener al menos 18 años |
| `fecha_aceptacion` | `TIMESTAMP` | NO | DEFAULT `CURRENT_TIMESTAMP` | Fecha y hora en que se aceptó |
| `fecha_revocacion` | `TIMESTAMP` | SÍ | DEFAULT `NULL` | Fecha en que fue revocado (`NULL` = vigente) |

### 5.3 Tabla `evaluaciones`
| Columna | Tipo de Dato | Nulo | Restricciones / Valor por Defecto | Descripción |
| :--- | :--- | :---: | :--- | :--- |
| `id` | `CHAR(36)` | NO | `PRIMARY KEY` | Identificador UUID v4 de la evaluación |
| `usuario_id` | `BIGINT` | NO | `FOREIGN KEY (usuarios.id) ON DELETE CASCADE` | Paciente evaluado |
| `estado` | `ENUM` | NO | `'EN_CURSO'`, `'COMPLETADA'`, `'ABANDONADA'`. DEFAULT `'EN_CURSO'` | Estado del ciclo de vida del cribado |
| `puntaje_phq9` | `TINYINT` | SÍ | `CHECK (puntaje_phq9 BETWEEN 0 AND 27)` | Puntaje total sumado de PHQ-9 |
| `puntaje_gad7` | `TINYINT` | SÍ | `CHECK (puntaje_gad7 BETWEEN 0 AND 21)` | Puntaje total sumado de GAD-7 |
| `severidad_depresion`| `ENUM` | SÍ | `'MINIMA'`,`'LEVE'`,`'MODERADA'`,`'MODERADAMENTE_SEVERA'`,`'SEVERA'` | Clasificación estandarizada PHQ-9 |
| `severidad_ansiedad` | `ENUM` | SÍ | `'MINIMA'`,`'LEVE'`,`'MODERADA'`,`'SEVERA'` | Clasificación estandarizada GAD-7 |
| `riesgo_crisis` | `BOOLEAN` | NO | DEFAULT `FALSE` | Indicador pestillo si PHQ-9 ítem 9 >= 1 |
| `fecha_creacion` | `TIMESTAMP` | NO | DEFAULT `CURRENT_TIMESTAMP` | Momento de apertura de la evaluación |
| `fecha_finalizacion`| `TIMESTAMP` | SÍ | DEFAULT `NULL` | Momento de cierre y cómputo de puntajes |

### 5.4 Tabla `respuestas_evaluacion`
| Columna | Tipo de Dato | Nulo | Restricciones / Valor por Defecto | Descripción |
| :--- | :--- | :---: | :--- | :--- |
| `id` | `BIGINT` | NO | `AUTO_INCREMENT`, `PRIMARY KEY` | Identificador de la respuesta individual |
| `evaluacion_id` | `CHAR(36)` | NO | `FOREIGN KEY (evaluaciones.id) ON DELETE CASCADE` | Evaluación a la que pertenece |
| `instrumento` | `ENUM` | NO | `'PHQ9'`, `'GAD7'` | Instrumento de cribado |
| `indice_pregunta`| `TINYINT` | NO | `CHECK (indice_pregunta BETWEEN 1 AND 9)` | Número de pregunta (PHQ-9: 1-9, GAD-7: 1-7) |
| `valor_puntaje` | `TINYINT` | NO | `CHECK (valor_puntaje BETWEEN 0 AND 3)` | Respuesta seleccionada (0=Ningún día, 3=Casi todos) |
| *Restricción Única* | | | `UNIQUE (evaluacion_id, instrumento, indice_pregunta)` | Una única respuesta por pregunta por sesión |

### 5.5 Tabla `registro_auditoria`
| Columna | Tipo de Dato | Nulo | Restricciones / Valor por Defecto | Descripción |
| :--- | :--- | :---: | :--- | :--- |
| `id` | `BIGINT` | NO | `AUTO_INCREMENT`, `PRIMARY KEY` | Identificador del evento auditado |
| `usuario_id` | `BIGINT` | SÍ | `FOREIGN KEY (usuarios.id) ON DELETE SET NULL` | Usuario que ejecutó la acción (anónimo si se borró cuenta) |
| `accion` | `VARCHAR(50)` | NO | | Acción: `INICIO_SESION`, `CONSENTIMIENTO_ACEPTADO`, `CONSENTIMIENTO_REVOCADO`, `EVALUACION_COMPLETADA`, `AUTORIZACION_OTORGADA`, `AUTORIZACION_REVOCADA`, `CITA_PROPUESTA`, `CITA_CONFIRMADA`, `CITA_RECHAZADA`, `CITA_CANCELADA`, `CITA_CUMPLIDA`, `CITA_POSPUESTA`, `PDF_DESCARGADO`, `CUENTA_ELIMINADA`, `CLINICO_ELIMINADO`, `PERFIL_ACTUALIZADO` |
| `entidad_id` | `VARCHAR(36)` | SÍ | | ID del recurso afectado (sin PII) |
| `fecha_creacion` | `TIMESTAMP` | NO | DEFAULT `CURRENT_TIMESTAMP` | Momento de la ocurrencia del evento |

### 5.6 Tabla `tokens_renovacion`
| Columna | Tipo de Dato | Nulo | Restricciones / Valor por Defecto | Descripción |
| :--- | :--- | :---: | :--- | :--- |
| `id` | `BIGINT` | NO | `AUTO_INCREMENT`, `PRIMARY KEY` | Identificador de la sesión de renovación |
| `usuario_id` | `BIGINT` | NO | `FOREIGN KEY (usuarios.id) ON DELETE CASCADE` | Usuario dueño de la sesión |
| `hash_token` | `VARCHAR(64)` | NO | `UNIQUE` | Hash SHA-256 del token opaco (nunca en texto plano) |
| `fecha_creacion` | `TIMESTAMP` | NO | DEFAULT `CURRENT_TIMESTAMP` | Momento de emisión |
| `fecha_expiracion`| `TIMESTAMP` | NO | | Momento en que expira (creación + 7 días) |
| `fecha_revocacion`| `TIMESTAMP` | SÍ | DEFAULT `NULL` | Momento de revocación (`NULL` = vigente) |

### 5.7 Tabla `autorizaciones_acceso`
| Columna | Tipo de Dato | Nulo | Restricciones / Valor por Defecto | Descripción |
| :--- | :--- | :---: | :--- | :--- |
| `id` | `CHAR(36)` | NO | `PRIMARY KEY` | Identificador UUID v4 de la autorización |
| `evaluacion_id` | `CHAR(36)` | NO | `FOREIGN KEY (evaluaciones.id) ON DELETE CASCADE` | Evaluación compartida |
| `paciente_id` | `BIGINT` | NO | `FOREIGN KEY (usuarios.id) ON DELETE CASCADE` | Paciente que autoriza |
| `clinico_id` | `BIGINT` | NO | `FOREIGN KEY (usuarios.id) ON DELETE CASCADE` | Profesional de la salud autorizado |
| `version_texto_autorizacion` | `VARCHAR(20)` | NO | DEFAULT `"1.0"` | Versión legal del consentimiento de compartir datos |
| `fecha_otorgamiento` | `TIMESTAMP` | NO | DEFAULT `CURRENT_TIMESTAMP` | Momento de concesión |
| `fecha_revocacion` | `TIMESTAMP` | SÍ | DEFAULT `NULL` | Momento en que se revocó (`NULL` = vigente) |
| *Restricción Única* | | | `UNIQUE (evaluacion_id, clinico_id)` | Reactiva la misma fila si se vuelve a autorizar |

### 5.8 Tabla `citas`
| Columna | Tipo de Dato | Nulo | Restricciones / Valor por Defecto | Descripción |
| :--- | :--- | :---: | :--- | :--- |
| `id` | `CHAR(36)` | NO | `PRIMARY KEY` | Identificador UUID v4 de la cita |
| `autorizacion_id`| `CHAR(36)` | NO | `FOREIGN KEY (autorizaciones_acceso.id) ON DELETE CASCADE` | Autorización clínica que habilita la cita |
| `paciente_id` | `BIGINT` | NO | `FOREIGN KEY (usuarios.id) ON DELETE CASCADE` | Paciente citado |
| `clinico_id` | `BIGINT` | NO | `FOREIGN KEY (usuarios.id) ON DELETE CASCADE` | Clínico que atiende |
| `fecha_hora` | `TIMESTAMP` | NO | | Fecha y hora acordada para la cita |
| `modalidad` | `ENUM` | NO | `'PRESENCIAL'`, `'VIRTUAL'` | Formato de la consulta |
| `mensaje` | `VARCHAR(300)` | SÍ | | Nota administrativa sin datos clínicos |
| `estado` | `ENUM` | NO | `'PROPUESTA'`, `'CONFIRMADA'`, `'RECHAZADA'`, `'CANCELADA'`, `'CUMPLIDA'`. DEFAULT `'PROPUESTA'` | Estado del ciclo de vida de la cita |
| `observaciones` | `VARCHAR(1000)`| SÍ | | Observaciones clínicas (al cumplir/posponer/cancelar) |
| `fecha_creacion` | `TIMESTAMP` | NO | DEFAULT `CURRENT_TIMESTAMP` | Momento de proposición de la cita |
| `fecha_actualizacion` | `TIMESTAMP`| NO | DEFAULT `CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` | Momento de última actualización |
| *Índice* | | | `INDEX idx_citas_clinico_fecha (clinico_id, fecha_hora)` | Optimización de consulta de solapamiento |

---

## 6. Reglas de Negocio del Dominio

### 6.1 Regla de Riesgo de Crisis (`ReglaCrisis`)
- **Disparador**: Respuesta al ítem 9 del cuestionario PHQ-9 (*"Pensamientos de que estaría mejor muerto/a o de hacerse daño de alguna manera"*).
- **Criterio**: Si el valor es **$\ge 1$** (1 = *Varios días*, 2 = *Más de la mitad de los días*, 3 = *Casi todos los días*), se activa `riesgoCrisis = true`. Si es 0 (*Ningún día*), es `false`.
- **Detección temprana y persistencia (*Latch*)**:
  - Al registrar la respuesta vía `PUT /api/triaje/evaluaciones/{id}/respuestas`, si el ítem 9 es $\ge 1$, el campo `riesgo_crisis` en la base de datos se guarda inmediatamente en `true`.
  - El frontend detecta la bandera `riesgoCrisis: true` en la respuesta y muestra de forma inmediata los recursos de emergencia sin esperar a finalizar el cuestionario.
  - Al invocar `FinalizarEvaluacion`, si el riesgo ya fue marcado, el estado se mantiene de forma irrevocable para esa sesión.

### 6.2 Regla de Resultado Mínimo (`ReglaResultadoMinimo`)
- **Propósito**: Determinar si los síntomas reportados son de severidad mínima en ambos trastornos y sin signos de alarma.
- **Condición Estricta**: Devuelve `true` **ÚNICAMENTE** si se cumplen simultáneamente las 3 condiciones:
  1. `severidadDepresion == SeveridadDepresion.MINIMA` (Puntaje PHQ-9 entre 0 y 4).
  2. `severidadAnsiedad == SeveridadAnsiedad.MINIMA` (Puntaje GAD-7 entre 0 y 4).
  3. `riesgoCrisis == false`.
- **Exclusión de Alarma**: Si `riesgoCrisis == true`, **NUNCA** es resultado mínimo, incluso si la suma total de puntos fuere muy baja (ej: puntaje PHQ-9 = 1 por el ítem 9, puntaje GAD-7 = 0 $\rightarrow$ `resultadoMinimo = false`).

### 6.3 Reglas de Cuestionarios y Puntuación
- **PHQ-9 (Depresión)**:
  - 9 ítems, cada uno con puntaje de 0 a 3. Total: 0 a 27.
  - 0 a 4: `MINIMA`
  - 5 a 9: `LEVE`
  - 10 a 14: `MODERADA`
  - 15 a 19: `MODERADAMENTE_SEVERA`
  - 20 a 27: `SEVERA`
- **GAD-7 (Ansiedad)**:
  - 7 ítems, cada uno con puntaje de 0 a 3. Total: 0 a 21.
  - 0 a 4: `MINIMA`
  - 5 a 9: `LEVE`
  - 10 a 14: `MODERADA`
  - 15 a 21: `SEVERA`

### 6.4 Reglas de Evaluaciones
- **Consentimiento Previo**: Para ejecutar `POST /api/triaje/evaluaciones`, el paciente debe tener un consentimiento informado vigente (`buscarVigentePorUsuarioId`). De lo contrario lanza `ExcepcionAccesoDenegado` (HTTP 403).
- **Control de Evaluaciones Múltiples**:
  - No puede iniciar una nueva evaluación si tiene una cita activa pendiente (`PROPUESTA` o `CONFIRMADA`). Lanza `ExcepcionDominio` (HTTP 409).
  - No puede iniciar una nueva evaluación si tiene una evaluación compartida en espera de atención clínica (autorización vigente sin citas en estado `CUMPLIDA`). Lanza `ExcepcionDominio` (HTTP 409).
- **Completitud Obligatoria**: No se puede finalizar una evaluación sin haber respondido exactamente las 9 preguntas de PHQ-9 y las 7 preguntas de GAD-7. Lanza `ExcepcionEvaluacionIncompleta` (HTTP 400).
- **Abandono Automático**: Tarea programada en cron identifica evaluaciones en estado `EN_CURSO` con más de 24 horas de antigüedad y las transiciona a `ABANDONADA`, preservando intacto el indicador `riesgo_crisis`.

### 6.5 Reglas de Autorizaciones de Acceso
- **Requisitos para Otorgar**:
  - La evaluación debe estar en estado `COMPLETADA`. Lanza `ExcepcionEvaluacionIncompleta` si está `EN_CURSO`.
  - El paciente debe tener consentimiento informado vigente.
  - El usuario clínico seleccionado debe existir, tener rol `CLINICO` y estar activo (`activo == true`).
- **Reactivación**: Si existía un registro previo para `(evaluacion_id, clinico_id)`, se reactiva actualizando `fecha_otorgamiento` a la hora actual y fijando `fecha_revocacion = null`.
- **Efectos de la Revocación de Autorización**:
  - Fija `fecha_revocacion` con el timestamp actual.
  - Cancela automáticamente todas las citas en estado `PROPUESTA` asociadas a esa autorización.
  - Las citas en estado `CONFIRMADA` se mantienen.
- **Efectos de la Revocación de Consentimiento**:
  - Si el paciente tiene una evaluación en estado `EN_CURSO`, se prohíbe la revocación hasta finalizarla o cancelarla (`ExcepcionDominio` HTTP 409).
  - Al revocar el consentimiento: se revocan en cascada todas las autorizaciones vigentes del paciente y se cancelan todas sus citas en estado `PROPUESTA`.

### 6.6 Reglas y Ciclo de Vida de Citas (`MaquinaEstadosCita`)
- **Proposición (`PROPUESTA`)**:
  - Solo puede ser propuesta por un usuario con rol `CLINICO`.
  - La fecha y hora debe ser estrictamente futura.
  - Requiere autorización de acceso vigente entre el paciente y el clínico para la evaluación indicada.
  - Conflicto de horario: El clínico no puede tener otra cita propuesta o confirmada en la misma fecha y segundo exacto. Lanza `ExcepcionDominio` (HTTP 409).
  - Cita única activa: No puede existir otra cita en estado `PROPUESTA` o `CONFIRMADA` entre ese mismo paciente y clínico. Lanza `ExcepcionDominio` (HTTP 409).
- **Confirmación / Rechazo**:
  - Acciones exclusivas del rol `PACIENTE`.
  - `PROPUESTA` $\rightarrow$ `CONFIRMADA`
  - `PROPUESTA` $\rightarrow$ `RECHAZADA` (Estado terminal).
- **Cancelación**:
  - Permitida a `PACIENTE`, `CLINICO` o el sistema (cron).
  - Permitida desde `PROPUESTA` o `CONFIRMADA` $\rightarrow$ `CANCELADA` (Estado terminal).
  - Si el clínico cancela, es obligatorio indicar el motivo u observaciones clínicas (`ExcepcionDatoInvalido` HTTP 400).
- **Cumplimiento**:
  - Acción exclusiva del `CLINICO`.
  - Permitida desde `PROPUESTA` o `CONFIRMADA` $\rightarrow$ `CUMPLIDA` (Estado terminal).
  - Es obligatorio ingresar las observaciones clínicas (1 a 1000 caracteres).
- **Posponer Cita**:
  - Acción del `CLINICO`.
  - Permitido únicamente en citas con estado `PROPUESTA` o `CONFIRMADA`.
  - Requiere nueva fecha y hora estrictamente futura. Mantiene el estado actual de la cita.
- **Expiración Automática**: Tarea cron periódica cancela de forma automática toda cita en estado `PROPUESTA` cuya `fechaHora` ya haya pasado.

---

## 7. Checkpoint de Verificación con el Frontend

Comparación de 3 rutas seleccionadas aleatoriamente entre este contrato y el código real de `frontend/src/servicios/`:

### Checkpoint 1: Registro de Respuesta Técnica
- **Ruta y Método**: `PUT /api/triaje/evaluaciones/{id}/respuestas`
- **Frontend (`servicioTriaje.js`)**:
  ```javascript
  async registrarRespuesta(evaluacionId, instrumento, indicePregunta, valorPuntaje) {
    return await clienteHttp.actualizar(`/api/triaje/evaluaciones/${evaluacionId}/respuestas`, {
      instrumento,
      indicePregunta,
      valorPuntaje
    });
  }
  ```
- **Backend (`ControladorTriaje.java`)**:
  ```java
  @PutMapping("/{id}/respuestas")
  public ResponseEntity<RespuestaRegistroItem> registrarRespuestaItem(
      @PathVariable String id,
      @Valid @RequestBody SolicitudRespuesta solicitud,
      @AuthenticationPrincipal UsuarioAutenticado usuario
  )
  ```
- **Resultado**: Coincidencia exacta de ruta, verbo PUT, payload JSON (`instrumento`, `indicePregunta`, `valorPuntaje`) y respuesta `RespuestaRegistroItem`.

### Checkpoint 2: Posponer Cita por el Clínico
- **Ruta y Método**: `PUT /api/clinico/citas/{id}/posponer`
- **Frontend (`servicioCitas.js`)**:
  ```javascript
  async posponerCita(id, datos) {
    return await clienteHttp.actualizar(`/api/clinico/citas/${id}/posponer`, {
      nuevaFechaHora: datos.nuevaFechaHora,
      mensaje: datos.mensaje || null,
      observaciones: datos.observaciones || null
    });
  }
  ```
- **Backend (`ControladorClinicoCitas.java`)**:
  ```java
  @PutMapping("/{id}/posponer")
  public ResponseEntity<RespuestaCitaClinico> posponer(
      @PathVariable String id,
      @Valid @RequestBody SolicitudPosponerCita solicitud,
      @AuthenticationPrincipal UsuarioAutenticado clinico
  )
  ```
- **Resultado**: Coincidencia exacta de ruta, verbo PUT, payload JSON (`nuevaFechaHora`, `mensaje`, `observaciones`) y respuesta `RespuestaCitaClinico`.

### Checkpoint 3: Inicio de Sesión
- **Ruta y Método**: `POST /api/autenticacion/inicio-sesion`
- **Frontend (`servicioAutenticacion.js`)**:
  ```javascript
  async iniciarSesion(correo, contrasena) {
    const respuesta = await clienteHttp.enviar('/api/autenticacion/inicio-sesion', {
      correo,
      contrasena
    });
    if (respuesta && respuesta.tokenAcceso) {
      establecerToken(respuesta.tokenAcceso);
    }
    return respuesta;
  }
  ```
- **Backend (`ControladorAutenticacion.java`)**:
  ```java
  @PostMapping("/inicio-sesion")
  public ResponseEntity<RespuestaToken> iniciarSesion(
      @Valid @RequestBody SolicitudInicioSesion solicitud,
      HttpServletRequest peticion
  )
  ```
- **Resultado**: Coincidencia exacta de ruta, verbo POST, payload JSON (`correo`, `contrasena`), respuesta `RespuestaToken` (`tokenAcceso`, `tipo`, `expiraEnSegundos`) y emisión de cookie `token_renovacion`.
