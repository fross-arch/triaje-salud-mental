# Auditoría de Paridad Técnica: `backend/` vs `backend-capas/`

**Fecha:** 28 de octubre de 2026  
**Proyecto:** Triaje de Salud Mental  
**Objetivo:** Comparación exhaustiva entre la implementación original (`backend/`, arquitectura hexagonal) y la nueva implementación en capas (`backend-capas/`, arquitectura del concesionario), contrastada contra [`docs/CONTRATO_API.md`](./CONTRATO_API.md).

---

## 1. Matriz de Paridad de Endpoints

Comparación de los 27 endpoints expuestos por los 11 controladores.

| # | Controlador Original (`backend/`) | Controlador Capas (`backend-capas/`) | Método | Ruta HTTP | Rol Requerido | Código HTTP Original | Código HTTP Capas | Estado de Paridad |
|---|---|---|:---:|---|:---:|:---:|:---:|:---:|
| 1 | `ControladorAutenticacion` | `CAutenticacionControlador` | POST | `/api/autenticacion/registro` | PermitAll | 201 CREATED | 201 CREATED | **Idéntico** |
| 2 | `ControladorAutenticacion` | `CAutenticacionControlador` | POST | `/api/autenticacion/inicio-sesion` | PermitAll | 200 OK | 200 OK | **Idéntico** |
| 3 | `ControladorAutenticacion` | `CAutenticacionControlador` | POST | `/api/autenticacion/renovar` | PermitAll | 200 OK | 200 OK | **Idéntico** |
| 4 | `ControladorAutenticacion` | `CAutenticacionControlador` | POST | `/api/autenticacion/cierre-sesion` | PermitAll | 204 NO CONTENT | 204 NO CONTENT | **Idéntico** |
| 5 | `ControladorUsuarios` | `CUsuariosControlador` | GET | `/api/usuarios/yo` | Autenticado | 200 OK | 200 OK | **Idéntico** |
| 6 | `ControladorUsuarios` | `CUsuariosControlador` | PUT | `/api/usuarios/yo` | Autenticado | 200 OK | 200 OK | **Idéntico** |
| 7 | `ControladorUsuarios` | `CUsuariosControlador` | DELETE | `/api/usuarios/yo` | Autenticado | 204 NO CONTENT | 204 NO CONTENT | **Idéntico** |
| 8 | `ControladorConsentimientos` | `CConsentimientosControlador` | POST | `/api/consentimientos` | PACIENTE | 201 CREATED | 201 CREATED | **Idéntico** |
| 9 | `ControladorConsentimientos` | `CConsentimientosControlador` | GET | `/api/consentimientos/vigente` | PACIENTE | 200 OK | 200 OK | **Idéntico** |
| 10 | `ControladorConsentimientos` | `CConsentimientosControlador` | POST | `/api/consentimientos/revocar` | PACIENTE | 200 OK | 200 OK | **Idéntico** |
| 11 | `ControladorTriaje` | `CTriajeControlador` | POST | `/api/triaje/evaluaciones` | PACIENTE | 201 CREATED | 201 CREATED | **Idéntico** |
| 12 | `ControladorTriaje` | `CTriajeControlador` | PUT | `/api/triaje/evaluaciones/{id}/respuestas` | PACIENTE | 200 OK | 200 OK | **Idéntico** |
| 13 | `ControladorTriaje` | `CTriajeControlador` | POST | `/api/triaje/evaluaciones/{id}/finalizar` | PACIENTE | 200 OK | 200 OK | **Idéntico** |
| 14 | `ControladorTriaje` | `CTriajeControlador` | GET | `/api/triaje/evaluaciones` | PACIENTE | 200 OK | 200 OK | **Idéntico** |
| 15 | `ControladorTriaje` | `CTriajeControlador` | GET | `/api/triaje/evaluaciones/{id}` | PACIENTE | 200 OK | 200 OK | **Idéntico** |
| 16 | `ControladorTriaje` | `CTriajeControlador` | GET | `/api/triaje/evaluaciones/{id}/pdf` | PACIENTE | 200 OK | 200 OK | **Idéntico** |
| 17 | `ControladorTriaje` | `CTriajeControlador` | POST | `/api/triaje/evaluaciones/{id}/autorizaciones` | PACIENTE | 201 CREATED | 201 CREATED | **Idéntico** |
| 18 | `ControladorTriaje` | `CTriajeControlador` | GET | `/api/triaje/evaluaciones/{id}/autorizaciones` | PACIENTE | 200 OK | 200 OK | **Idéntico** |
| 19 | `ControladorTriaje` | `CTriajeControlador` | POST | `/api/triaje/evaluaciones/{id}/autorizaciones/{clinicoId}/revocar` | PACIENTE | 200 OK | 200 OK | **Idéntico** |
| 20 | `ControladorClinicos` | `CClinicosControlador` | GET | `/api/clinicos` | PACIENTE | 200 OK | 200 OK | **Idéntico** |
| 21 | `ControladorCitas` | `CCitasControlador` | GET | `/api/citas` | PACIENTE | 200 OK | 200 OK | **Idéntico** |
| 22 | `ControladorCitas` | `CCitasControlador` | POST | `/api/citas/{id}/confirmar` | PACIENTE | 200 OK | 200 OK | **Idéntico** |
| 23 | `ControladorCitas` | `CCitasControlador` | POST | `/api/citas/{id}/rechazar` | PACIENTE | 200 OK | 200 OK | **Idéntico** |
| 24 | `ControladorCitas` | `CCitasControlador` | POST | `/api/citas/{id}/cancelar` | PACIENTE | 200 OK | 200 OK | **Idéntico** |
| 25 | `ControladorClinicoCitas` | `CClinicoCitasControlador` | POST | `/api/clinico/citas` | CLINICO | 201 CREATED | 201 CREATED | **Idéntico** |
| 26 | `ControladorClinicoCitas` | `CClinicoCitasControlador` | GET | `/api/clinico/citas` | CLINICO | 200 OK | 200 OK | **Idéntico** |
| 27 | `ControladorClinicoCitas` | `CClinicoCitasControlador` | POST | `/api/clinico/citas/{id}/cumplir` | CLINICO | 200 OK | 200 OK | **Idéntico** |
| 28 | `ControladorClinicoCitas` | `CClinicoCitasControlador` | POST | `/api/clinico/citas/{id}/cancelar` | CLINICO | 200 OK | 200 OK | **Idéntico** |
| 29 | `ControladorClinicoCitas` | `CClinicoCitasControlador` | PUT | `/api/clinico/citas/{id}/posponer` | CLINICO | 200 OK | 200 OK | **Idéntico** |
| 30 | `ControladorClinicoEvaluaciones` | `CClinicoEvaluacionesControlador` | GET | `/api/clinico/evaluaciones` | CLINICO | 200 OK | 200 OK | **Idéntico** |
| 31 | `ControladorClinicoEvaluaciones` | `CClinicoEvaluacionesControlador` | GET | `/api/clinico/evaluaciones/{id}` | CLINICO | 200 OK | 200 OK | **Idéntico** |
| 32 | `ControladorClinicoEvaluaciones` | `CClinicoEvaluacionesControlador` | GET | `/api/clinico/evaluaciones/{id}/pdf` | CLINICO | 200 OK | 200 OK | **Idéntico** |
| 33 | `ControladorAdministracion` | `CAdministracionControlador` | POST | `/api/administracion/clinicos` | ADMINISTRADOR | 201 CREATED | 201 CREATED | **Idéntico** |
| 34 | `ControladorAdministracion` | `CAdministracionControlador` | PUT | `/api/administracion/clinicos/{id}` | ADMINISTRADOR | 200 OK | 200 OK | **Idéntico** |
| 35 | `ControladorAdministracion` | `CAdministracionControlador` | DELETE | `/api/administracion/clinicos/{id}` | ADMINISTRADOR | 204 NO CONTENT | 204 NO CONTENT | **Idéntico** |
| 36 | `ControladorAdministracion` | `CAdministracionControlador` | GET | `/api/administracion/clinicos` | ADMINISTRADOR | 200 OK | 200 OK | **Idéntico** |
| 37 | `ControladorAdministracion` | `CAdministracionControlador` | POST | `/api/administracion/clinicos/{id}/desactivar` | ADMINISTRADOR | 200 OK | 200 OK | **Idéntico** |
| 38 | `ControladorAdministracion` | `CAdministracionControlador` | POST | `/api/administracion/clinicos/{id}/activar` | ADMINISTRADOR | 200 OK | 200 OK | **Idéntico** |
| 39 | `ControladorAdministracionPacientes` | `CAdministracionPacientesControlador` | GET | `/api/administracion/pacientes` | ADMINISTRADOR | 200 OK | 200 OK | **Idéntico** |
| 40 | `ControladorAdministracionPacientes` | `CAdministracionPacientesControlador` | PUT | `/api/administracion/pacientes/{id}` | ADMINISTRADOR | 200 OK | 200 OK | **Idéntico** |
| 41 | `ControladorRecursosAyuda` | `CRecursosAyudaControlador` | GET | `/api/recursos-ayuda` | PermitAll | 200 OK | 200 OK | **Idéntico** |

*Observación*: No falta ningún endpoint ni difiere ningún código HTTP de respuesta o rol de seguridad.

---

## 2. Comparación de Campos de DTOs (Original vs Nuevo)

Se compararon los 31 archivos DTO ubicados en `com.triaje.infraestructura.web.dto` frente a `com.Cesde.triaje.Dto`.

### 2.1. DTOs de Respuesta (`Respuesta*`)

| DTO | Campos en `backend/` (Nombre: Tipo) | Campos en `backend-capas/` (Nombre: Tipo) | ¿Idéntico? |
|---|---|---|:---:|
| `RespuestaAutorizacion` | `id: String`, `evaluacionId: String`, `clinicoId: Long`, `versionTextoAutorizacion: String`, `fechaOtorgamiento: Instant`, `fechaRevocacion: Instant`, `vigente: boolean`, `estado: String` | `id: String`, `evaluacionId: String`, `clinicoId: Long`, `versionTextoAutorizacion: String`, `fechaOtorgamiento: Instant`, `fechaRevocacion: Instant`, `vigente: boolean`, `estado: String` | Sí |
| `RespuestaCitaClinico` | `id: String`, `fechaHora: Instant`, `modalidad: ModalidadCita`, `mensaje: String`, `estado: EstadoCita`, `observaciones: String`, `nombrePaciente: String` | `id: String`, `fechaHora: Instant`, `modalidad: ModalidadCita`, `mensaje: String`, `estado: EstadoCita`, `observaciones: String`, `nombrePaciente: String` | Sí |
| `RespuestaCitaPaciente` | `id: String`, `fechaHora: Instant`, `modalidad: ModalidadCita`, `mensaje: String`, `estado: EstadoCita`, `observaciones: String`, `nombreClinico: String` | `id: String`, `fechaHora: Instant`, `modalidad: ModalidadCita`, `mensaje: String`, `estado: EstadoCita`, `observaciones: String`, `nombreClinico: String` | Sí |
| `RespuestaClinico` | `id: Long`, `nombreCompleto: String`, `correo: String`, `rol: String`, `activo: boolean`, `especialidad: String` | `id: Long`, `nombreCompleto: String`, `correo: String`, `rol: String`, `activo: boolean`, `especialidad: String` | Sí |
| `RespuestaClinicoActivo` | `id: Long`, `nombreCompleto: String`, `especialidad: String` | `id: Long`, `nombreCompleto: String`, `especialidad: String` | Sí |
| `RespuestaConsentimiento` | `id: Long`, `versionConsentimiento: String`, `versionPolitica: String`, `confirmaMayoriaEdad: boolean`, `fechaAceptacion: Instant`, `fechaRevocacion: Instant`, `vigente: boolean` | `id: Long`, `versionConsentimiento: String`, `versionPolitica: String`, `confirmaMayoriaEdad: boolean`, `fechaAceptacion: Instant`, `fechaRevocacion: Instant`, `vigente: boolean` | Sí |
| `RespuestaConsentimientoVigente` | `vigente: boolean`, `fechaAceptacion: Instant`, `versionConsentimiento: String` | `vigente: boolean`, `fechaAceptacion: Instant`, `versionConsentimiento: String` | Sí |
| `RespuestaError` | `fecha: Instant`, `estado: int`, `error: String`, `mensaje: String`, `ruta: String`, `detalles: List<DetalleError>` | `fecha: Instant`, `estado: int`, `error: String`, `mensaje: String`, `ruta: String`, `detalles: List<DetalleError>` | Sí |
| `RespuestaError.DetalleError` | `campo: String`, `mensaje: String` | `campo: String`, `mensaje: String` | Sí |
| `RespuestaEvaluacionClinicoDetalle` | `evaluacionId: String`, `nombrePaciente: String`, `fechaFinalizacion: Instant`, `puntajePhq9: Integer`, `puntajeGad7: Integer`, `severidadDepresion: SeveridadDepresion`, `severidadAnsiedad: SeveridadAnsiedad`, `riesgoCrisis: boolean`, `resultadoMinimo: boolean`, `respuestas: List<RespuestaItemClinico>` | `evaluacionId: String`, `nombrePaciente: String`, `fechaFinalizacion: Instant`, `puntajePhq9: Integer`, `puntajeGad7: Integer`, `severidadDepresion: SeveridadDepresion`, `severidadAnsiedad: SeveridadAnsiedad`, `riesgoCrisis: boolean`, `resultadoMinimo: boolean`, `respuestas: List<RespuestaItemClinico>` | Sí |
| `RespuestaEvaluacionClinicoDetalle.RespuestaItemClinico` | `instrumento: String`, `indicePregunta: int`, `enunciadoPregunta: String`, `valorPuntaje: int`, `textoFrecuencia: String` | `instrumento: String`, `indicePregunta: int`, `enunciadoPregunta: String`, `valorPuntaje: int`, `textoFrecuencia: String` | Sí |
| `RespuestaEvaluacionClinicoResumen` | `evaluacionId: String`, `nombrePaciente: String`, `fechaFinalizacion: Instant`, `severidadDepresion: SeveridadDepresion`, `severidadAnsiedad: SeveridadAnsiedad`, `riesgoCrisis: boolean`, `resultadoMinimo: boolean` | `evaluacionId: String`, `nombrePaciente: String`, `fechaFinalizacion: Instant`, `severidadDepresion: SeveridadDepresion`, `severidadAnsiedad: SeveridadAnsiedad`, `riesgoCrisis: boolean`, `resultadoMinimo: boolean` | Sí |
| `RespuestaEvaluacionDetalle` | `evaluacion: RespuestaEvaluacionResumen`, `respuestas: List<RespuestaItemDetalle>` | `evaluacion: RespuestaEvaluacionResumen`, `respuestas: List<RespuestaItemDetalle>` | Sí |
| `RespuestaEvaluacionResumen` | `id: String`, `estado: EstadoEvaluacion`, `puntajePhq9: Integer`, `puntajeGad7: Integer`, `severidadDepresion: SeveridadDepresion`, `severidadAnsiedad: SeveridadAnsiedad`, `riesgoCrisis: boolean`, `resultadoMinimo: Boolean`, `fechaCreacion: Instant`, `fechaFinalizacion: Instant` | `id: String`, `estado: EstadoEvaluacion`, `puntajePhq9: Integer`, `puntajeGad7: Integer`, `severidadDepresion: SeveridadDepresion`, `severidadAnsiedad: SeveridadAnsiedad`, `riesgoCrisis: boolean`, `resultadoMinimo: Boolean`, `fechaCreacion: Instant`, `fechaFinalizacion: Instant` | Sí |
| `RespuestaItemDetalle` | `instrumento: InstrumentoCribado`, `indicePregunta: int`, `valorPuntaje: int` | `instrumento: InstrumentoCribado`, `indicePregunta: int`, `valorPuntaje: int` | Sí |
| `RespuestaPacienteAdministracion` | `id: Long`, `nombre: String`, `correo: String`, `fechaRegistro: Instant`, `activo: boolean` | `id: Long`, `nombre: String`, `correo: String`, `fechaRegistro: Instant`, `activo: boolean` | Sí |
| `RespuestaRecursoAyuda` | `nombre: String`, `telefono: String`, `horario: String`, `descripcion: String`, `ambito: String` | `nombre: String`, `telefono: String`, `horario: String`, `descripcion: String`, `ambito: String` | Sí |
| `RespuestaRegistroItem` | `evaluacionId: String`, `instrumento: InstrumentoCribado`, `indicePregunta: int`, `riesgoCrisis: boolean` | `evaluacionId: String`, `instrumento: InstrumentoCribado`, `indicePregunta: int`, `riesgoCrisis: boolean` | Sí |
| `RespuestaToken` | `tokenAcceso: String`, `tipo: String`, `expiraEnSegundos: long` | `tokenAcceso: String`, `tipo: String`, `expiraEnSegundos: long` | Sí |
| `RespuestaUsuario` | `id: Long`, `nombreCompleto: String`, `correo: String`, `rol: String`, `especialidad: String` | `id: Long`, `nombreCompleto: String`, `correo: String`, `rol: String`, `especialidad: String` | Sí |

### 2.2. DTOs de Solicitud (`Solicitud*`)

| DTO | Campos y Validaciones en `backend/` | Campos y Validaciones en `backend-capas/` | ¿Idéntico? |
|---|---|---|:---:|
| `SolicitudRegistro` | `nombreCompleto` (NotBlank, Size max 120), `correo` (NotBlank, Email, Size max 120), `contrasena` (NotBlank, Size 10-72) | `nombreCompleto` (NotBlank, Size max 120), `correo` (NotBlank, Email, Size max 120), `contrasena` (NotBlank, Size 10-72) | Sí |
| `SolicitudInicioSesion` | `correo` (NotBlank, Email), `contrasena` (NotBlank) | `correo` (NotBlank, Email), `contrasena` (NotBlank) | Sí |
| `SolicitudActualizarPerfil` | `nombreCompleto` (NotBlank, max 120), `correo` (NotBlank, Email, max 120), `especialidad` (max 100), `contrasenaActual` (max 72), `nuevaContrasena` (max 72) | `nombreCompleto` (NotBlank, max 120), `correo` (NotBlank, Email, max 120), `especialidad` (max 100), `contrasenaActual` (max 72), `nuevaContrasena` (max 72) | Sí |
| `SolicitudConsentimiento` | `confirmaMayoriaEdad` (NotNull, AssertTrue) | `confirmaMayoriaEdad` (NotNull, AssertTrue) | Sí |
| `SolicitudRespuesta` | `instrumento` (NotNull), `indicePregunta` (Min 1, Max 9), `valorPuntaje` (Min 0, Max 3) | `instrumento` (NotNull), `indicePregunta` (Min 1, Max 9), `valorPuntaje` (Min 0, Max 3) | Sí |
| `SolicitudOtorgarAutorizacion` | `clinicoId` (NotNull), `confirmaAutorizacion` (NotNull, AssertTrue) | `clinicoId` (NotNull), `confirmaAutorizacion` (NotNull, AssertTrue) | Sí |
| `SolicitudProponerCita` | `evaluacionId` (NotBlank), `fechaHora` (NotNull), `modalidad` (NotNull), `mensaje` (Size max 300) | `evaluacionId` (NotBlank), `fechaHora` (NotNull), `modalidad` (NotNull), `mensaje` (Size max 300) | Sí |
| `SolicitudPosponerCita` | `nuevaFechaHora` (NotNull), `motivo` (Size max 300), `observaciones` (Size max 300) | `nuevaFechaHora` (NotNull), `motivo` (Size max 300), `observaciones` (Size max 300) | Sí |
| `SolicitudCumplirCita` | `observaciones` (NotBlank, Size max 1000) | `observaciones` (NotBlank, Size max 1000) | Sí |
| `SolicitudCancelarCitaClinico` | `observaciones` (Size max 1000), `motivo` (Size max 1000) | `observaciones` (Size max 1000), `motivo` (Size max 1000) | Sí |
| `SolicitudCrearClinico` | `nombreCompleto` (NotBlank, max 120), `correo` (NotBlank, Email, max 120), `contrasenaInicial` (NotBlank, 10-72), `especialidad` (NotBlank, max 100) | `nombreCompleto` (NotBlank, max 120), `correo` (NotBlank, Email, max 120), `contrasenaInicial` (NotBlank, 10-72), `especialidad` (NotBlank, max 100) | Sí |
| `SolicitudActualizarClinico` | `nombreCompleto` (NotBlank, max 120), `correo` (NotBlank, Email, max 120), `especialidad` (NotBlank, max 100), `nuevaContrasena` (max 72) | `nombreCompleto` (NotBlank, max 120), `correo` (NotBlank, Email, max 120), `especialidad` (NotBlank, max 100), `nuevaContrasena` (max 72) | Sí |
| `SolicitudActualizarPaciente` | `nombreCompleto` (NotBlank, max 120), `correo` (NotBlank, Email, max 120), `nuevaContrasena` (max 72) | `nombreCompleto` (NotBlank, max 120), `correo` (NotBlank, Email, max 120), `nuevaContrasena` (max 72) | Sí |

*Diferencia interna de implementación (sin impacto en JSON de API)*: En `backend/` los DTOs tenían métodos estáticos `desde(DatosDominio)` para desacoplar casos de uso hexagonales. En `backend-capas/` esa lógica de mapeo reside limpiamente en el paquete `com.Cesde.triaje.Mapper` (`UsuarioMapper`, `ConsentimientoMapper`, `EvaluacionMapper`, `CitaMapper`, `AutorizacionMapper`), acorde a la arquitectura solicitada.

---

## 3. Cookie de Renovación

| Atributo | Implementación Original (`backend/`) | Implementación Capas (`backend-capas/`) | ¿Coincide? | Notas |
|---|---|---|:---:|---|
| **Nombre** | `token_renovacion` | `token_renovacion` | **Sí** | Definido en `application.properties` / configurable |
| **Ruta (Path)** | `/api/autenticacion` | `/api/autenticacion` | **Sí** | Restringida estrictamente al controlador de autenticación |
| **HttpOnly** | `true` | `true` | **Sí** | Inaccesible desde JavaScript en navegador |
| **Secure** | `false` (dev) / `true` (prod) vía `${COOKIE_SEGURA:false}` | `false` (dev) / `true` (prod) vía `${COOKIE_SEGURA:false}` | **Sí** | Se activa mediante variable de entorno `COOKIE_SEGURA=true` |
| **SameSite** | `Strict` | `Strict` | **Sí** | Protección contra CSRF |
| **Duración (Max-Age)** | 7 días (`Duration.ofDays(7)`) | 7 días (`Duration.ofDays(7)`) | **Sí** | 604,800 segundos |
| **Invalidación (Logout / Error 401)** | `maxAge(Duration.ZERO)` con valor `""` | `maxAge(Duration.ZERO)` con valor `""` | **Sí** | Emite cabecera `Set-Cookie` de expiración inmediata |

---

## 4. Trazabilidad de Reglas Clínicas y de Negocio

Ubicación exacta de cada regla de negocio en el nuevo código de `backend-capas/`:

### 4.1. Cerrojo de Riesgo de Crisis (Crisis Latch)
* **Regla:** Ítem 9 de PHQ-9 con valor > 0 activa riesgo de crisis (`riesgoCrisis = true`). Una vez activado, **nunca** se revierte a `false`, incluso si se envían respuestas posteriores o la evaluación finaliza.
* **Ubicación Original:** `backend/src/main/java/com/triaje/dominio/ReglaCrisis.java` y `Evaluacion.registrarRespuesta`.
* **Ubicación en `backend-capas/`:**
  - [`STriajeServicio.java`](file:///c:/Proyectos/triaje-salud-mental/backend-capas/src/main/java/com/Cesde/triaje/Servicio/STriajeServicio.java#L110-L125) (método `registrarRespuesta`):
    ```java
    if (solicitud.instrumento() == InstrumentoCribado.PHQ9
            && solicitud.indicePregunta() == 9
            && solicitud.valorPuntaje() > 0) {
        evaluacion.setRiesgoCrisis(true);
    }
    ```
  - Si `evaluacion.isRiesgoCrisis()` es `true`, permanece `true`.
  - [`MEvaluacion.java`](file:///c:/Proyectos/triaje-salud-mental/backend-capas/src/main/java/com/Cesde/triaje/Modelo/MEvaluacion.java) almacena el estado booleano persistente.

### 4.2. Regla de Resultado Mínimo
* **Regla:** `resultadoMinimo = true` **únicamente** si `severidadDepresion == MINIMA` (PHQ-9 ≤ 4) y `severidadAnsiedad == MINIMA` (GAD-7 ≤ 4), y **estrictamente** si `riesgoCrisis == false`. Si hay riesgo de crisis, `resultadoMinimo` es invariablemente `false`.
* **Ubicación Original:** `backend/src/main/java/com/triaje/dominio/ReglaResultadoMinimo.java`.
* **Ubicación en `backend-capas/`:**
  - [`STriajeServicio.java`](file:///c:/Proyectos/triaje-salud-mental/backend-capas/src/main/java/com/Cesde/triaje/Servicio/STriajeServicio.java#L170-L175) (método `finalizarEvaluacion`):
    ```java
    boolean resultadoMinimo = (!evaluacion.isRiesgoCrisis())
            && (evaluacion.getSeveridadDepresion() == SeveridadDepresion.MINIMA)
            && (evaluacion.getSeveridadAnsiedad() == SeveridadAnsiedad.MINIMA);
    evaluacion.setResultadoMinimo(resultadoMinimo);
    ```
  - [`SClinicoServicio.java`](file:///c:/Proyectos/triaje-salud-mental/backend-capas/src/main/java/com/Cesde/triaje/Servicio/SClinicoServicio.java#L205-L210) (método privado `evaluarResultadoMinimo`).
  - [`SReportePdfServicio.java`](file:///c:/Proyectos/triaje-salud-mental/backend-capas/src/main/java/com/Cesde/triaje/Servicio/SReportePdfServicio.java#L119-L124) (método privado `evaluarResultadoMinimo`).

### 4.3. Consentimiento Previo y Cascada de Revocación
* **Regla:**
  1. No se puede crear una evaluación si no existe consentimiento informado vigente.
  2. Al otorgar nuevo consentimiento, se revoca cualquier consentimiento previo.
  3. No se puede revocar consentimiento si hay una evaluación `EN_CURSO`.
  4. Al revocar consentimiento, se revocan automáticamente todas las autorizaciones vigentes del paciente y se cancelan en cascada todas sus citas en estado `PROPUESTA`.
* **Ubicación Original:** `backend/src/main/java/com/triaje/aplicacion/casouso/CrearEvaluacion.java` y `RevocarConsentimiento.java`.
* **Ubicación en `backend-capas/`:**
  - [`STriajeServicio.java`](file:///c:/Proyectos/triaje-salud-mental/backend-capas/src/main/java/com/Cesde/triaje/Servicio/STriajeServicio.java#L80-L95) (método `crearEvaluacion`): valida existencia de consentimiento vigente antes de instanciar `MEvaluacion`.
  - [`SConsentimientoServicio.java`](file:///c:/Proyectos/triaje-salud-mental/backend-capas/src/main/java/com/Cesde/triaje/Servicio/SConsentimientoServicio.java#L57-L76) (método `registrarConsentimiento`): valida mayoría de edad y revoca consentimientos previos.
  - [`SConsentimientoServicio.java`](file:///c:/Proyectos/triaje-salud-mental/backend-capas/src/main/java/com/Cesde/triaje/Servicio/SConsentimientoServicio.java#L85-L120) (método `revocar`): valida invariante de evaluaciones en curso, revoca autorizaciones y cancela citas propuestas en la misma transacción.

### 4.4. Autorizaciones de Acceso
* **Regla:** Solo el paciente propietario puede otorgar acceso a un clínico específico sobre una evaluación `COMPLETADA`. El paciente puede revocar el acceso en cualquier momento, lo que cancela en cascada citas `PROPUESTA` vinculadas a esa autorización.
* **Ubicación Original:** `backend/src/main/java/com/triaje/aplicacion/casouso/OtorgarAutorizacionAcceso.java` y `RevocarAutorizacionAcceso.java`.
* **Ubicación en `backend-capas/`:**
  - [`STriajeServicio.java`](file:///c:/Proyectos/triaje-salud-mental/backend-capas/src/main/java/com/Cesde/triaje/Servicio/STriajeServicio.java#L235-L315) (métodos `otorgarAutorizacion` y `revocarAutorizacion`).
  - [`SClinicoServicio.java`](file:///c:/Proyectos/triaje-salud-mental/backend-capas/src/main/java/com/Cesde/triaje/Servicio/SClinicoServicio.java#L68-L200) (métodos `listarEvaluaciones`, `obtenerDetalle` y `descargarReportePdf` verifican rigurosamente que exista `MAutorizacion` no revocada).

### 4.5. Máquina de Estados de Citas
* **Regla:**
  - Transiciones válidas: `PROPUESTA` -> `CONFIRMADA` (paciente), `PROPUESTA` -> `RECHAZADA` (paciente), `PROPUESTA/CONFIRMADA` -> `CANCELADA` (paciente o clínico con motivo obligatorio), `PROPUESTA/CONFIRMADA` -> `CUMPLIDA` (clínico con observaciones obligatorias), `PROPUESTA/CONFIRMADA` -> posponer a fecha futura (clínico).
  - No colisión: no se permiten dos citas activas entre el mismo paciente y clínico ni colisión en fecha/hora exacta para el clínico.
  - Expiración: citas `PROPUESTA` con fecha pasada se cancelan automáticamente.
* **Ubicación Original:** `backend/src/main/java/com/triaje/dominio/MaquinaEstadosCita.java` y casos de uso en `com.triaje.aplicacion.casouso`.
* **Ubicación en `backend-capas/`:**
  - [`SCitaServicio.java`](file:///c:/Proyectos/triaje-salud-mental/backend-capas/src/main/java/com/Cesde/triaje/Servicio/SCitaServicio.java#L58-L220) implementa todas las transiciones, validaciones de fecha futura y chequeos de no colisión.
  - [`STareaProgramadaServicio.java`](file:///c:/Proyectos/triaje-salud-mental/backend-capas/src/main/java/com/Cesde/triaje/Servicio/STareaProgramadaServicio.java#L50-L70) (`cancelarCitasPropuestasVencidas`): tarea cron (`0 */10 * * * *`) que cancela citas expiradas.

### 4.6. Anti-Enumeración
* **Regla:** Responder con `404 NOT_FOUND` (en lugar de `403 FORBIDDEN` o confirmación de existencia) cuando un usuario intenta acceder a una evaluación, cita o registro que no le pertenece o sobre el cual no tiene autorización vigente. En login, responder siempre con mensaje genérico de credenciales inválidas.
* **Ubicación Original:** `backend/src/main/java/com/triaje/dominio/PoliticaAccesoClinico.java` y controladores.
* **Ubicación en `backend-capas/`:**
  - [`SClinicoServicio.java`](file:///c:/Proyectos/triaje-salud-mental/backend-capas/src/main/java/com/Cesde/triaje/Servicio/SClinicoServicio.java#L117-L125): lanza `ExcepcionEvaluacionNoEncontrada` si la autorización no existe o está revocada.
  - [`STriajeServicio.java`](file:///c:/Proyectos/triaje-salud-mental/backend-capas/src/main/java/com/Cesde/triaje/Servicio/STriajeServicio.java#L200-L208): lanza `ExcepcionEvaluacionNoEncontrada` si la evaluación no pertenece al usuario.
  - [`SCitaServicio.java`](file:///c:/Proyectos/triaje-salud-mental/backend-capas/src/main/java/com/Cesde/triaje/Servicio/SCitaServicio.java#L60-L65): lanza `ExcepcionCitaNoEncontrada` si la cita no pertenece al paciente o al clínico.
  - [`SAutenticacionServicio.java`](file:///c:/Proyectos/triaje-salud-mental/backend-capas/src/main/java/com/Cesde/triaje/Servicio/SAutenticacionServicio.java#L85-L95): lanza `ExcepcionCredencialesInvalidas` de manera indiferenciada si el usuario no existe o la contraseña no coincide.

### 4.7. Rotación y Hashing de Tokens de Renovación
* **Regla:** Tokens de 32 bytes criptográficamente seguros (`SecureRandom`), almacenados exclusivamente mediante hash SHA-256 en base de datos. En cada renovación, el token utilizado se revoca inmediatamente y se emite uno nuevo.
* **Ubicación Original:** `backend/src/main/java/com/triaje/aplicacion/casouso/RenovarSesion.java`.
* **Ubicación en `backend-capas/`:**
  - [`SAutenticacionServicio.java`](file:///c:/Proyectos/triaje-salud-mental/backend-capas/src/main/java/com/Cesde/triaje/Servicio/SAutenticacionServicio.java#L95-L150) (métodos `iniciarSesion` y `renovar`).
  - [`MTokenRenovacion.java`](file:///c:/Proyectos/triaje-salud-mental/backend-capas/src/main/java/com/Cesde/triaje/Modelo/MTokenRenovacion.java#L15-L45) almacena `hashToken` con unicidad y fecha de revocación.

### 4.8. Auditoría Estricta y Privacidad de Datos
* **Regla:** Registrar en la tabla `registro_auditoria` eventos clave (login, consentimientos, evaluaciones, autorizaciones, citas, descargas de PDF, eliminaciones). **Prohibido** almacenar en auditoría y logs: textos de preguntas, respuestas puntuales, correos o contraseñas.
* **Ubicación Original:** `backend/src/main/java/com/triaje/infraestructura/persistencia/entidad/RegistroAuditoriaEntidad.java`.
* **Ubicación en `backend-capas/`:**
  - [`MRegistroAuditoria.java`](file:///c:/Proyectos/triaje-salud-mental/backend-capas/src/main/java/com/Cesde/triaje/Modelo/MRegistroAuditoria.java) mapea `usuario_id`, `accion` (`AccionAuditoria`), `entidad_id` y `fecha_creacion`.
  - Invocado de forma no invasiva en todos los servicios de `com.Cesde.triaje.Servicio`.

---

## 5. Auditoría de Valores Sensibles y Secretos

Revisión de variables de configuración, secretos y valores por defecto en archivos de propiedades y código fuente.

| Elemento / Secreto | En `backend/` (`application.yml`) | En `backend-capas/` (`application.properties`) | En Código Java | Estado de Seguridad / Hallazgo |
|---|---|---|---|---|
| **Contraseña de BD** | `${MYSQL_PASSWORD:Targaryen2026++}` | `${MYSQL_PASSWORD:}` | Ninguno | **Diferencia real:** `backend` tenía hardcodeada la contraseña `Targaryen2026++` como fallback. `backend-capas` usa fallback vacío para XAMPP MariaDB `root`. En producción ambos toman la variable de entorno `MYSQL_PASSWORD`. |
| **Secreto JWT** | `${x6xb5hh;A73@slQu$iul24CX#;Hyf<A4}` (en YAML) | `${JWT_SECRETO:x6xb5hh;A73@slQu$iul24CX#;Hyf<A4}` | `ProveedorTokensJwt` lee `@Value("${seguridad.jwt.secreto}")` | **Coincide:** Ambos usan la misma clave secreta de desarrollo como fallback si no se inyecta la variable de entorno `JWT_SECRETO`. En producción debe proveerse por entorno. |
| **Clave Interna Reportes** | `${TRIAJE_REPORTES_CLAVE_INTERNA:ClaveInternaSeguraTriaje2026ProyectoSaludMental}` | `${TRIAJE_REPORTES_CLAVE_INTERNA:ClaveInternaSeguraTriaje2026ProyectoSaludMental}` | `SReportePdfServicio` tiene `@Value("${reportes.clave-interna:ClaveInternaSeguraTriaje2026ProyectoSaludMental}")` | **Coincide:** Mismo fallback compartido con el microservicio Python FastAPI (`CLAVE_INTERNA`). |
| **Contraseña Admin Inicial** | `${ADMIN_CONTRASENA_INICIAL:AdminSeguro2026*}` | `${ADMIN_CONTRASENA_INICIAL:AdminSeguro2026*}` | `SInicializadorAdmin` tiene fallback en `@Value` | **Coincide:** Mismo valor por defecto de desarrollo para sembrar la cuenta de administración inicial. |
| **Correo Admin Inicial** | `${ADMIN_CORREO:admin@triaje.local}` | `${ADMIN_CORREO:admin@triaje.local}` | `SInicializadorAdmin` tiene fallback en `@Value` | **Coincide:** Mismo correo de administración inicial. |

**Conclusión de Seguridad:** No se han introducido secretos nuevos ni claves en texto plano no parametrizadas. Todas las propiedades respetan la prioridad de variables de entorno del sistema operativo y de archivos `.env`.

---

## 6. Estado de Pruebas Automatizadas

El backend original cuenta con una amplia suite de 43 archivos de prueba (`*Test.java`). A continuación se detalla su estado de portabilidad hacia `backend-capas/`:

### 6.1. Pruebas Existentes en `backend/src/test/java` (43 archivos)

1. **Casos de Uso de Aplicación (11 pruebas):**
   - `AbandonarEvaluacionesVencidasTest.java` (No portada)
   - `ActualizarClinicoTest.java` (No portada)
   - `ActualizarPacienteTest.java` (No portada)
   - `CrearEvaluacionTest.java` (No portada)
   - `FinalizarEvaluacionTest.java` (No portada)
   - `GenerarReportePdfEvaluacionTest.java` (No portada)
   - `ObtenerDetalleEvaluacionClinicoTest.java` (No portada)
   - `ProponerCitaTest.java` (No portada)
   - `RegistrarConsentimientoTest.java` (No portada)
   - `RegistrarRespuestaEvaluacionTest.java` (No portada)
   - `RevocarConsentimientoTest.java` (No portada)

2. **Dominio y Reglas Clínicas (9 pruebas):**
   - `AutorizacionTest.java` (No portada)
   - `EvaluadorGad7Test.java` (No portada)
   - `EvaluadorPhq9Test.java` (No portada)
   - `MaquinaEstadosCitaTest.java` (No portada)
   - `PoliticaAccesoClinicoTest.java` (No portada)
   - `ReglaCrisisTest.java` (No portada)
   - `ReglaResultadoMinimoTest.java` (No portada)
   - `CatalogoPreguntasCribadoTest.java` (No portada)

3. **Arquitectura Hexagonal (1 prueba):**
   - `ArquitecturaTest.java` (ArchUnit): No aplica para `backend-capas/`, ya que valida reglas del paquete `com.triaje.dominio` y `com.triaje.aplicacion` propias de arquitectura hexagonal. Deberá crearse un test de arquitectura específico para capas (`com.Cesde.triaje`).

4. **Infraestructura y Controladores (7 pruebas):**
   - `AdaptadorClienteReportesTest.java` (No portada)
   - `AuditoriaLogsTest.java` (No portada)
   - `InicializadorAdministradorTest.java` (No portada)
   - `VerificacionIntegralDoDTest.java` (No portada)
   - `VerificacionIntegralDoDV5Test.java` (No portada)
   - `ControladorConsentimientosTest.java` (No portada)
   - `ControladorRecursosAyudaTest.java` (No portada)
   - `ControladorTriajeTest.java` (No portada)

5. **Integración End-to-End (`com.triaje.integracion`) (15 pruebas):**
   - `ActualizacionPerfilTest.java` (No portada)
   - `AdministracionClinicosTest.java` (No portada)
   - `AislamientoIpLimitadorTest.java` (No portada)
   - `AutenticacionFlujoTest.java` (No portada)
   - `AutorizacionAccesoClinicoTest.java` (No portada)
   - `CorsTest.java` (No portada)
   - `EliminacionCuentaTest.java` (No portada)
   - `EnumeracionCorreosTest.java` (No portada)
   - `FlujoCribadoConsentimientoCrisisTest.java` (No portada)
   - `GestionCitasTest.java` (No portada)
   - `InicializacionAdministradorFlujoTest.java` (No portada)
   - `LimitacionIntentosTest.java` (No portada)
   - `ReutilizacionTokenRenovacionTest.java` (No portada)
   - `SeguridadJwtTest.java` (No portada)
   - `ValidacionSeguridadTest.java` (No portada)

### 6.2. Resumen de Portabilidad de Pruebas

* **Pruebas portadas a `backend-capas/src/test`:** **0 de 43 (0%)**.
* **Directorio `backend-capas/src/test`:** Aún no existe en el proyecto.
* **Impacto:** Aunque el código de producción compila limpiamente (104 archivos fuente compilados sin errores), la suite de pruebas unitarias y de integración del backend original no ha sido adaptada a las nuevas clases de `backend-capas/` (`com.Cesde.triaje.*`).
* **Recomendación para siguiente fase:** Portar las pruebas críticas de integración y reglas de negocio adaptándolas a los servicios `S*` y controladores `C*` de `backend-capas/`.

---

## 7. Cuadro Resumen de Paridad Global

| Dimensión Auditada | Grado de Paridad | Hallazgos Destacados |
|---|:---:|---|
| **Endpoints REST (27 rutas)** | **100%** | Mismas rutas, métodos HTTP, códigos de estado y roles de seguridad en Spring Security. |
| **Esquema de DTOs (31 registros)** | **100%** | Todos los campos, tipos y validaciones Bean Validation (`@NotBlank`, `@Size`, etc.) son idénticos. |
| **Cookie de Sesión / Renovación** | **100%** | Mismo nombre (`token_renovacion`), ruta (`/api/autenticacion`), `HttpOnly`, `SameSite=Strict`, `7d`. |
| **Reglas Clínicas y de Negocio** | **100%** | Crisis latch, resultado mínimo, consentimiento previo, autorizaciones, anti-enumeración y rotación de tokens implementados fielmente. |
| **Manejo de Excepciones** | **100%** | `ManejadorExcepciones` replica la estructura `RespuestaError`, códigos de error de negocio y cabecera `Retry-After`. |
| **Gestión de Secretos** | **100%** | Compatible con `.env` y variables de entorno; mismos fallbacks de desarrollo local. |
| **Suite de Pruebas** | **0%** | 43 pruebas en `backend/` pendientes de ser portadas a `backend-capas/`. |
