# Triaje de Salud Mental

Sistema web de **cribado preliminar en salud mental**. Una persona adulta crea su cuenta, acepta el consentimiento informado y responde los cuestionarios **PHQ-9** (depresión) y **GAD-7** (ansiedad). El sistema calcula los puntajes, indica un nivel de severidad, detecta señales de crisis y genera un reporte en PDF. Si la persona lo decide, puede autorizar a un clínico para que vea su resultado y le proponga una cita.

> El sistema **no emite diagnósticos**: orienta y facilita el paso a un profesional de la salud.

**Siglas**

| Sigla | En inglés | En español | Mide |
| :--- | :--- | :--- | :--- |
| PHQ-9 | *Patient Health Questionnaire-9* | Cuestionario sobre la Salud del Paciente (9 ítems) | Depresión (0 a 27) |
| GAD-7 | *Generalized Anxiety Disorder-7* | Escala del Trastorno de Ansiedad Generalizada (7 ítems) | Ansiedad (0 a 21) |

## A quién está dirigido

| Rol | Qué hace en el sistema |
| :--- | :--- |
| **Paciente** | Se registra, da su consentimiento, responde los cuestionarios, descarga su PDF, autoriza o revoca el acceso de un clínico y gestiona sus citas. |
| **Clínico** | Consulta solo las evaluaciones que los pacientes le autorizaron, descarga su PDF, y propone, pospone, cumple o cancela citas. |
| **Administrador** | Gestiona clínicos y pacientes, y consulta el tablero estadístico con datos agregados y anónimos. |

## Integrantes

- Mariana Rico
- Bryan Mendoza
- Sebastián González
- Yadir Morales Hernández

---

## Tecnologías utilizadas

| Pieza | Tecnologías |
| :--- | :--- |
| **Backend** | Java 21, Spring Boot 3.3.5 (Web, Data JPA, Security, Validation), Maven |
| **Seguridad** | JWT (JJWT, firma HS256), BCrypt, Bucket4j y Caffeine para limitar intentos |
| **Mapeo** | MapStruct |
| **Base de datos** | MariaDB / MySQL (XAMPP). Hibernate con `ddl-auto=update` |
| **Frontend** | React 18, Vite 6, Tailwind CSS 3, React Router 6 |
| **Reportes** | Python, FastAPI, ReportLab, matplotlib, pandas, numpy |
| **Pruebas** | JUnit y Spring Test con H2 en memoria, pytest, Vitest |

El repositorio contiene cuatro carpetas:

```
backend-capas/   API REST en Spring Boot con arquitectura en capas
frontend/        Interfaz en React + Vite
reportes/        Microservicio Python para los PDF y los gráficos del tablero
docs/            Contrato de la API, políticas, comparación de arquitecturas y colección de Postman
```

## Arquitectura utilizada

El backend sigue la **arquitectura en capas**. El paquete raíz es `com.Cesde.triaje` y cada tipo de clase lleva un prefijo que lo identifica.

```
Frontend  ->  Controlador  ->  Servicio  ->  Repositorio  ->  Base de datos
              (usa Dto y Mapper)  (usa Regla)    (usa Modelo)
```

| Capa | Paquete | Qué hace |
| :--- | :--- | :--- |
| **Entity** | `Modelo` | Entidades JPA con sus atributos y relaciones (`MUsuario`, `MEvaluacion`, `MCita`...) y las enumeraciones |
| **Repository** | `Repositorio` | Acceso a datos con interfaces de Spring Data JPA (`I...Repositorio`) y proyecciones |
| **Service** | `Servicio` | Casos de uso y orquestación (`SEvaluacion`, `SCita`, `SAutenticacion`, `SReporte`...). Son `@Transactional` |
| **Controller** | `Controlador` | Recibe las solicitudes HTTP (`CAutenticacion`, `CTriaje`, `CCita`...) y traduce los errores con `ManejadorGlobalExcepciones` |
| **DTO** | `Dto` | Contratos de la API: `Solicitud*` (lo que entra, validado) y `Respuesta*` (lo que sale) |
| **Mapper** | `Mapper` | Convierte entre entidades y DTOs con MapStruct (`I...Mapper`) y mappers manuales |

Paquetes adicionales:

| Paquete | Qué contiene |
| :--- | :--- |
| `Regla` | Reglas de dominio puras: `EvaluadorPhq9`, `EvaluadorGad7`, `ReglaCrisis`, `ReglaResultadoMinimo`, `MaquinaEstadosCita`, `PoliticaAccesoClinico`, `PoliticaContrasena` |
| `Seguridad` | `FiltroAutenticacionJwt`, `ProveedorTokensJwt`, `GestorCookieRenovacion`, `LimitadorIntentos` |
| `Configuracion` | `ConfiguracionSeguridad` (CORS y permisos por rol) y `PropiedadesRecursosAyuda` |
| `Excepcion` | Excepciones de negocio |
| `Tarea` | Tareas programadas y creación del administrador inicial |

Los DTOs evitan exponer campos sensibles (por ejemplo, ninguna respuesta incluye el hash de la contraseña) y los Mappers concentran la conversión entre entidad y DTO en un solo lugar.

---

## Requisitos previos

| Requisito | Versión mínima |
| :--- | :--- |
| **Java** (OpenJDK, por ejemplo Amazon Corretto) | 21 |
| **Gestor de dependencias del backend** | Maven (se incluye el Maven Wrapper `mvnw`) |
| **Base de datos** | XAMPP con MySQL / MariaDB 10.4 o superior, en el puerto 3306 |
| **Node.js** con npm | 18 |
| **Python** con pip y soporte para `venv` | 3.10 |

### Variables de entorno

Los secretos no se escriben en el código. Se leen de variables de entorno o del archivo `backend-capas/.env`.

| Variable | Para qué sirve | Obligatoria |
| :--- | :--- | :---: |
| `ADMIN_CORREO` | Correo del administrador inicial | Sí |
| `ADMIN_CONTRASENA_INICIAL` | Contraseña inicial del administrador | Sí |
| `JWT_SECRETO` | Clave para firmar los tokens JWT | Sí |
| `CLAVE_INTERNA` | Clave compartida entre el backend y el microservicio de reportes. Debe ser igual en `reportes/.env` | Sí |
| `TRIAJE_REPORTES_URL` | Dirección del microservicio de reportes (por defecto `http://127.0.0.1:8000`) | No |
| `BD_PUERTO`, `BD_USUARIO`, `MYSQL_DATABASE` | Conexión a la base de datos (por defecto `3306`, `root` y `triaje_salud_mental_db`) | No |
| `COOKIE_SEGURA` | Marca la cookie de renovación como segura. Usar `true` con HTTPS (por defecto `false`) | No |

> **Importante:** el archivo `.env` está en el `.gitignore` y **nunca se sube al repositorio**. Usa valores propios, largos y aleatorios.

---

## Instalación y configuración

### 1. Clonar el repositorio

```bash
git clone <URL-del-repositorio>
cd <carpeta-del-repositorio>
```

### 2. Configurar las variables de entorno

Crea `backend-capas/.env`:

```env
ADMIN_CORREO=<correo-del-administrador>
ADMIN_CONTRASENA_INICIAL=<contraseña-segura>
JWT_SECRETO=<clave-larga-y-aleatoria>
CLAVE_INTERNA=<clave-compartida>
TRIAJE_REPORTES_URL=http://127.0.0.1:8000
```

Crea `reportes/.env` con **la misma** clave interna:

```env
CLAVE_INTERNA=<clave-compartida>
```

### 3. Configurar la base de datos

1. Abre el **XAMPP Control Panel** y presiona **Start** en el módulo **MySQL** (puerto 3306).
2. Crea la base de datos desde phpMyAdmin (`http://localhost/phpmyadmin`) o desde una terminal:

```sql
CREATE DATABASE IF NOT EXISTS triaje_salud_mental_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Hibernate crea las 8 tablas automáticamente al iniciar el backend (`ddl-auto=update`), así que no hay que ejecutar scripts.

Si tu base de datos ya existía y la columna `accion` de `registro_auditoria` quedó como `ENUM`, ejecuta:

```sql
ALTER TABLE registro_auditoria MODIFY accion VARCHAR(50) NOT NULL;
```

### 4. Iniciar el aplicativo (en este orden)

**a) Microservicio de reportes (puerto 8000)**

```powershell
cd reportes
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
python -m uvicorn principal:aplicacion --port 8000 --host 127.0.0.1
```

**b) Backend (puerto 8080)**

```powershell
cd backend-capas
.\mvnw.cmd spring-boot:run
```

En la primera ejecución se crea automáticamente el administrador inicial con los valores de `ADMIN_CORREO` y `ADMIN_CONTRASENA_INICIAL`.

**c) Frontend (puerto 5173)**

```powershell
cd frontend
npm install
npm run dev
```

La interfaz queda disponible en `http://localhost:5173`.

### Configuración interna del aplicativo

| Elemento | Valor |
| :--- | :--- |
| Frontend | `http://localhost:5173` |
| Backend (API) | `http://localhost:8080`, rutas bajo `/api/...` |
| Microservicio de reportes | `http://127.0.0.1:8000` |
| Base de datos | `localhost:3306`, base `triaje_salud_mental_db` |
| CORS permitido | `http://localhost:5173` |
| Token de acceso (JWT) | Vence a los 15 minutos |
| Cookie de renovación | `token_renovacion`, 7 días |
| Límite de intentos | 20 por minuto por IP y 5 por minuto por correo e IP |

### Credenciales de ingreso

| Rol | Cómo se obtiene la cuenta |
| :--- | :--- |
| **Administrador** | Se crea al primer arranque con `ADMIN_CORREO` y `ADMIN_CONTRASENA_INICIAL` |
| **Clínico** | Lo crea el administrador desde el panel de administración |
| **Paciente** | Se registra desde la pantalla de registro |

Las contraseñas deben tener mínimo 10 caracteres y máximo 72 bytes (límite de BCrypt).

---

## Endpoints

Todas las rutas parten de `http://localhost:8080`. La especificación completa (DTOs y códigos HTTP) está en [`docs/CONTRATO_API.md`](docs/CONTRATO_API.md) y hay una colección lista para importar en [`docs/postman/Triaje_Salud_Mental.postman_collection.json`](docs/postman/Triaje_Salud_Mental.postman_collection.json).

### Autenticación y cuenta (`CAutenticacion`, `CUsuario`, `CRecursosAyuda`)

| Método | Ruta | Descripción | Acceso |
| :--- | :--- | :--- | :--- |
| POST | `/api/autenticacion/registro` | Crea la cuenta de un paciente | Público |
| POST | `/api/autenticacion/inicio-sesion` | Inicia sesión y entrega los tokens | Público |
| POST | `/api/autenticacion/renovar` | Renueva el token de acceso con la cookie | Público |
| POST | `/api/autenticacion/cierre-sesion` | Cierra la sesión y revoca el token de renovación | Público |
| GET | `/api/recursos-ayuda` | Lista las líneas de ayuda en crisis | Público |
| GET, PUT, DELETE | `/api/usuarios/yo` | Consulta, edita o elimina la cuenta propia | Autenticado |

### Consentimiento (`CConsentimiento`)

| Método | Ruta | Descripción | Acceso |
| :--- | :--- | :--- | :--- |
| POST | `/api/consentimientos` | Registra el consentimiento informado | Paciente |
| GET | `/api/consentimientos/vigente` | Consulta el consentimiento vigente | Paciente |
| POST | `/api/consentimientos/revocar` | Revoca el consentimiento | Paciente |

### Evaluaciones del paciente (`CTriaje`)

| Método | Ruta | Descripción | Acceso |
| :--- | :--- | :--- | :--- |
| POST | `/api/triaje/evaluaciones` | Crea una evaluación | Paciente |
| GET | `/api/triaje/evaluaciones` | Lista las evaluaciones propias | Paciente |
| GET | `/api/triaje/evaluaciones/{id}` | Detalle de una evaluación | Paciente |
| PUT | `/api/triaje/evaluaciones/{id}/respuestas` | Guarda la respuesta de un ítem | Paciente |
| POST | `/api/triaje/evaluaciones/{id}/finalizar` | Calcula puntajes, severidad y riesgo de crisis | Paciente |
| GET | `/api/triaje/evaluaciones/{id}/pdf` | Descarga el reporte en PDF | Paciente |
| POST | `/api/triaje/evaluaciones/{id}/autorizaciones` | Autoriza a un clínico a ver la evaluación | Paciente |
| GET | `/api/triaje/evaluaciones/{id}/autorizaciones` | Lista las autorizaciones otorgadas | Paciente |
| POST | `/api/triaje/evaluaciones/{id}/autorizaciones/{clinicoId}/revocar` | Revoca una autorización | Paciente |

### Clínicos y citas del paciente (`CClinico`, `CCita`)

| Método | Ruta | Descripción | Acceso |
| :--- | :--- | :--- | :--- |
| GET | `/api/clinicos` | Lista los clínicos activos | Paciente |
| GET | `/api/citas` | Lista las citas del paciente | Paciente |
| POST | `/api/citas/{id}/confirmar` | Confirma una cita propuesta | Paciente |
| POST | `/api/citas/{id}/rechazar` | Rechaza una cita propuesta | Paciente |
| POST | `/api/citas/{id}/cancelar` | Cancela una cita | Paciente |

### Panel del clínico (`CClinicoEvaluacion`, `CClinicoCita`)

| Método | Ruta | Descripción | Acceso |
| :--- | :--- | :--- | :--- |
| GET | `/api/clinico/evaluaciones` | Lista las evaluaciones autorizadas | Clínico |
| GET | `/api/clinico/evaluaciones/{id}` | Detalle de una evaluación autorizada | Clínico |
| GET | `/api/clinico/evaluaciones/{id}/pdf` | Descarga el PDF de una evaluación autorizada | Clínico |
| POST | `/api/clinico/citas` | Propone una cita | Clínico |
| GET | `/api/clinico/citas` | Lista sus citas | Clínico |
| POST | `/api/clinico/citas/{id}/cumplir` | Marca la cita como cumplida | Clínico |
| POST | `/api/clinico/citas/{id}/cancelar` | Cancela una cita | Clínico |
| PUT | `/api/clinico/citas/{id}/posponer` | Pospone una cita | Clínico |

### Administración (`CAdministracion`)

| Método | Ruta | Descripción | Acceso |
| :--- | :--- | :--- | :--- |
| GET | `/api/administracion/clinicos` | Lista los clínicos | Administrador |
| POST | `/api/administracion/clinicos` | Crea un clínico | Administrador |
| PUT | `/api/administracion/clinicos/{id}` | Edita un clínico | Administrador |
| DELETE | `/api/administracion/clinicos/{id}` | Elimina un clínico | Administrador |
| POST | `/api/administracion/clinicos/{id}/activar` | Activa un clínico | Administrador |
| POST | `/api/administracion/clinicos/{id}/desactivar` | Desactiva un clínico | Administrador |
| GET | `/api/administracion/pacientes` | Lista los pacientes | Administrador |
| PUT | `/api/administracion/pacientes/{id}` | Edita un paciente | Administrador |
| GET | `/api/administracion/tablero?dias=30` | Tablero estadístico con datos agregados y anónimos | Administrador |
| GET | `/api/administracion/tablero/exportar?dias=30&formato=csv` | Exporta el tablero en CSV o PDF (`formato=pdf`) | Administrador |

### Códigos de respuesta

| HTTP | Cuándo ocurre |
| :--- | :--- |
| 400 | Datos inválidos, JSON mal formado o evaluación incompleta |
| 401 | Credenciales o token de renovación inválidos |
| 403 | El rol o la autorización no permiten la acción |
| 404 | El recurso no existe |
| 409 | Duplicado o regla de negocio incumplida (por ejemplo, una transición de cita no permitida) |
| 429 | Demasiados intentos por minuto |
| 503 | El microservicio de reportes no responde |
| 500 | Error inesperado (mensaje genérico) |

---

## Pruebas unitarias

**Backend.** Las pruebas de integración usan una base de datos **H2 en memoria** en modo MySQL, así que no necesitan XAMPP ni Docker:

```powershell
cd backend-capas
.\mvnw.cmd test
```

Incluyen pruebas de las reglas de dominio (evaluadores PHQ-9 y GAD-7, crisis, resultado mínimo, máquina de estados de la cita, política de contraseña), de seguridad (JWT y límite de intentos) y de integración de los flujos principales (autenticación, consentimiento, citas, autorizaciones, eliminación de cuenta y tablero).

**Microservicio de reportes:**

```powershell
cd reportes
python -m pytest
```

**Frontend:**

```powershell
cd frontend
npm test
```

---

## Reglas de negocio principales

| Regla | Detalle |
| :--- | :--- |
| Severidad PHQ-9 | 0-4 mínima, 5-9 leve, 10-14 moderada, 15-19 moderadamente severa, 20-27 severa |
| Severidad GAD-7 | 0-4 mínima, 5-9 leve, 10-14 moderada, 15-21 severa |
| Riesgo de crisis | Se activa si el ítem 9 del PHQ-9 vale 1 o más, aunque el puntaje total sea bajo |
| Resultado mínimo | Solo si depresión y ansiedad son mínimas y no hay riesgo de crisis |
| Acceso del clínico | Solo con una autorización vigente del paciente, que se puede revocar |
| Citas | PROPUESTA (clínico) pasa a CONFIRMADA o RECHAZADA (paciente); PROPUESTA o CONFIRMADA pasan a CANCELADA; el clínico marca CUMPLIDA |
| Tareas programadas | Cada hora se marcan como abandonadas las evaluaciones vencidas; cada 10 minutos se cancelan las citas propuestas que expiraron |

## Documentación adicional

| Documento | Contenido |
| :--- | :--- |
| [`docs/CONTRATO_API.md`](docs/CONTRATO_API.md) | Rutas REST, DTOs y códigos HTTP |
| [`docs/COMPARACION_ARQUITECTURAS.md`](docs/COMPARACION_ARQUITECTURAS.md) | Comparación entre la arquitectura hexagonal y la de capas |
| [`docs/politica-tratamiento-datos-v2.md`](docs/politica-tratamiento-datos-v2.md) | Política de tratamiento de datos personales |
| [`docs/preparar-mysql.md`](docs/preparar-mysql.md) | Preparación de MySQL / MariaDB (XAMPP y Docker) |
