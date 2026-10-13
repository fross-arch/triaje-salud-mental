# Plan Maestro de Desarrollo v4: Sistema de Cribado y Orientación en Salud Mental

> **Enfoque:** el sistema es una herramienta de **cribado (tamizaje) psicométrico**, no de diagnóstico. El orden de construcción prioriza lo que más riesgo y valor tiene (motor clínico, protocolo de crisis, consentimiento) y deja para el final lo cosmético (avatar, PDF elaborado).
>
> **Cambios de la v3 respecto a la v2:** todo el proyecto en español; restricción a mayores de 18 años; consentimiento y política de tratamiento de datos como documentos propios; revocación del consentimiento.
>
> **Cambios de la v4 respecto a la v3:** mensaje específico para resultados mínimos (`ReglaResultadoMinimo`); nota informativa en todos los resultados; pantalla de introducción que explica qué son y qué miden los cuestionarios (`docs/informacion-cuestionarios-v1.md`).

---

## 1. Ficha Técnica

| Elemento | Decisión |
|---|---|
| Nombre | Sistema Web de Cribado Clínico Preliminar y Orientación en Salud Mental |
| Naturaleza | Cribado con PHQ-9 y GAD-7. No diagnostica ni reemplaza la valoración profesional |
| Población | **Solo mayores de 18 años** (declaración explícita en el consentimiento) |
| Arquitectura | Backend Spring Boot (hexagonal) + microservicio Python interno para reportes. Comunicación REST síncrona |
| Frontend | React (Vite), Tailwind CSS, Lucide Icons, avatar en SVG |
| Backend | Java 21, Spring Boot 3.x (Security, Data JPA, Validation, RestClient) |
| Base de datos | MySQL 8.0.16+ (necesario para `CHECK`), migraciones con **Flyway**. phpMyAdmin solo como visor |
| Reportes | Python 3.10+, FastAPI, Matplotlib, ReportLab |
| Pruebas | JUnit 5, Mockito, Testcontainers (MySQL), Vitest + Testing Library, pytest |
| Asistente de código | Antigravity, con prompts por sprint |

---

## 2. Convenciones de Idioma (obligatorias)

Todo el proyecto está en español. Esta tabla es la regla única.

| Elemento | Criterio | Ejemplo |
|---|---|---|
| Clases, métodos y variables | Español, sin tildes ni ñ | `EvaluadorPhq9`, `puntajeTotal`, `anio` |
| Paquetes Java | Español | `com.triaje.dominio`, `com.triaje.aplicacion`, `com.triaje.infraestructura` |
| Tablas y columnas | Español, `snake_case` | `respuestas_evaluacion`, `fecha_creacion` |
| Valores de enumeración | Español, mayúsculas | `MODERADAMENTE_SEVERA` |
| Rutas de la API | Español, minúsculas con guiones | `/api/triaje/evaluaciones` |
| Campos JSON | Español, `camelCase` | `riesgoCrisis`, `indicePregunta` |
| Componentes React | Español, `PascalCase` | `AsistenteTriaje`, `AvisoCrisis` |
| Archivos Python | Español, `snake_case` | `generador_pdf.py` |
| Comentarios, mensajes de error, commits y documentación | Español | — |

**Excepciones inevitables (no se traducen):**
* Anotaciones y APIs de los frameworks (`@Service`, `@Entity`, `useState`, `FastAPI`).
* Métodos HTTP (`GET`, `POST`, `PUT`, `DELETE`) y cabeceras estándar (`Content-Type`, `Authorization`).
* Nombres de archivos de configuración fijos (`application.yml`, `pom.xml`, `package.json`).
* **Convención de pruebas:** las clases de prueba Java deben terminar en `Test` (Maven Surefire no detecta otro sufijo por defecto), los archivos de Vitest en `.test.jsx` y los de pytest empiezan por `test_`. El resto del nombre sí va en español: `EvaluadorPhq9Test`, `test_generador_pdf.py`.
* Siglas clínicas (`PHQ9`, `GAD7`, `JWT`, `PDF`).

---

## 3. Arquitectura

```
┌────────────────────────────────────────────────────────┐
│                     FRONTEND REACT                     │
│  - Autenticación (token en memoria, renovación cookie) │
│  - Consentimiento informado (+ mayoría de edad)        │
│  - Asistente de cribado (una pregunta a la vez)        │
│  - Aviso de crisis (prioritario, NO bloqueante)        │
│  - Resultados y descarga de PDF                        │
└──────────────────────────┬─────────────────────────────┘
                           │ HTTPS / JSON (Bearer + cookie de renovación)
                           ▼
┌────────────────────────────────────────────────────────┐
│          SPRING BOOT (Hexagonal) - Puerto 8080         │
│  dominio/          Lógica clínica pura (sin Spring)    │
│  aplicacion/       Casos de uso y puertos              │
│  infraestructura/  Web, JPA, seguridad, cliente Python │
└─────────────┬────────────────────────────┬─────────────┘
              │ JDBC                       │ HTTP interno + X-Clave-Interna
              ▼                            ▼
┌───────────────────────────┐ ┌──────────────────────────┐
│          MySQL            │ │   MICROSERVICIO PYTHON   │
│  usuarios, consentimientos│ │   FastAPI: 8000          │
│  evaluaciones,            │ │   Solo red interna       │
│  respuestas_evaluacion,   │ │   Gráfico + PDF          │
│  registro_auditoria       │ │                          │
└───────────────────────────┘ └──────────────────────────┘
```

Estructura de carpetas:

```
Raíz del Proyecto /
├── backend/    (Maven / Spring Boot)
├── frontend/   (Vite / React)
├── reportes/   (Python / FastAPI)
└── docs/       (consentimiento, política de datos, información de cuestionarios, límites, diagramas)
```

---

## 4. Reglas Clínicas (referencia única del proyecto)

### PHQ-9 (depresión): 9 ítems, valores 0-3, total 0-27

| Puntaje | Severidad | Valor de enumeración |
|---|---|---|
| 0 - 4 | Mínima | `MINIMA` |
| 5 - 9 | Leve | `LEVE` |
| 10 - 14 | Moderada | `MODERADA` |
| 15 - 19 | Moderadamente severa | `MODERADAMENTE_SEVERA` |
| 20 - 27 | Severa | `SEVERA` |

### GAD-7 (ansiedad): 7 ítems, valores 0-3, total 0-21

| Puntaje | Severidad | Valor de enumeración |
|---|---|---|
| 0 - 4 | Mínima | `MINIMA` |
| 5 - 9 | Leve | `LEVE` |
| 10 - 14 | Moderada | `MODERADA` |
| 15 - 21 | Severa | `SEVERA` |

### Regla de crisis

Si el **ítem 9 del PHQ-9** (pensamientos de que estaría mejor muerto/a o de hacerse daño de alguna manera) tiene valor **≥ 1**, `riesgoCrisis = true`. Se calcula **en el backend en el momento en que se guarda esa respuesta**, no solo al finalizar.

### Fuentes (citar en la sustentación)

* PHQ-9: Kroenke K, Spitzer RL, Williams JBW. *J Gen Intern Med*, 2001.
* GAD-7: Spitzer RL, Kroenke K, Williams JBW, Löwe B. *Arch Intern Med*, 2006.
* Usar una **versión en español con validación publicada** y citar su referencia. Verificar los términos de uso de los instrumentos antes de publicar.

### Escala de respuesta

`0 = Ningún día` · `1 = Varios días` · `2 = Más de la mitad de los días` · `3 = Casi todos los días`

### Regla de resultado mínimo

Un resultado en nivel mínimo no significa "no le pasa nada": el PHQ-9 y el GAD-7 solo exploran depresión y ansiedad en las últimas dos semanas. Por eso el sistema define un mensaje propio para ese caso.

`resultadoMinimo = true` **solo si** se cumplen las tres condiciones: severidad de depresión `MINIMA`, severidad de ansiedad `MINIMA` y `riesgoCrisis = false`.

| Severidad depresión | Severidad ansiedad | Riesgo de crisis | `resultadoMinimo` |
|---|---|---|---|
| MINIMA | MINIMA | No | **Sí** |
| MINIMA | MINIMA | **Sí** (ítem 9 ≥ 1, aunque el total sea bajo) | No |
| LEVE o mayor | MINIMA | No | No |
| MINIMA | LEVE o mayor | No | No |

**Regla de oro:** si hay riesgo de crisis, **nunca** se muestra el mensaje de resultado mínimo. Un puntaje total bajo con ítem 9 ≥ 1 es posible y debe mostrar el aviso de crisis. El valor `resultadoMinimo` se deriva de los datos existentes; no se guarda en la base de datos.

### Mensajes de resultado (textos para el frontend y el PDF)

Los textos deben ser revisados por un docente o profesional de psicología.

* **Mensaje de resultado mínimo** (solo si `resultadoMinimo = true`):
  *"Tus respuestas no sugieren síntomas significativos de depresión ni de ansiedad en las últimas dos semanas. Esto no descarta que estés pasando por otras dificultades: este cribado solo explora esas dos áreas. Si te sientes mal o preocupado/a, puedes consultar con un profesional de todas formas; no necesitas un puntaje alto para pedir ayuda."*
* **Mensaje por instrumento** (si solo uno de los dos es mínimo, la tarjeta de ese instrumento muestra):
  *"Tus respuestas en este cuestionario no sugieren síntomas significativos."*
* **Nota informativa** (se muestra en **todos** los resultados, sin excepción):
  *"Este resultado es orientativo. No mide todas las situaciones de salud mental (por ejemplo duelo, estrés, trauma o consumo de sustancias) y un puntaje bajo no invalida cómo te sientes."*

### Información sobre los cuestionarios

Antes de comenzar, la persona ve una introducción (`docs/informacion-cuestionarios-v1.md`) que explica qué son el PHQ-9 y el GAD-7, qué miden, el periodo de dos semanas, la escala de respuesta, cómo se interpretan los niveles, qué no hacen y por qué se pregunta por pensamientos de hacerse daño. Un enlace "Sobre los cuestionarios" permanece accesible durante toda la evaluación.

---

## 5. Modelo de Datos (migración Flyway `V1__esquema_inicial.sql`)

Ubicación: `backend/src/main/resources/db/migration/V1__esquema_inicial.sql`. No se ejecuta a mano en phpMyAdmin: Flyway lo aplica al arrancar.

```sql
CREATE TABLE usuarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre_completo VARCHAR(120) NOT NULL,
    correo VARCHAR(120) NOT NULL UNIQUE,
    contrasena_hash VARCHAR(255) NOT NULL,
    rol ENUM('PACIENTE', 'CLINICO') NOT NULL DEFAULT 'PACIENTE', -- CLINICO reservado, sin endpoints en el MVP
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE consentimientos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    version_consentimiento VARCHAR(20) NOT NULL,
    version_politica VARCHAR(20) NOT NULL,
    confirma_mayoria_edad BOOLEAN NOT NULL,
    fecha_aceptacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_revocacion TIMESTAMP NULL,
    CHECK (confirma_mayoria_edad = TRUE),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);

CREATE TABLE evaluaciones (
    id CHAR(36) PRIMARY KEY, -- UUID
    usuario_id BIGINT NOT NULL,
    estado ENUM('EN_CURSO', 'COMPLETADA', 'ABANDONADA') NOT NULL DEFAULT 'EN_CURSO',
    puntaje_phq9 TINYINT NULL,
    puntaje_gad7 TINYINT NULL,
    severidad_depresion ENUM('MINIMA','LEVE','MODERADA','MODERADAMENTE_SEVERA','SEVERA') NULL,
    severidad_ansiedad ENUM('MINIMA','LEVE','MODERADA','SEVERA') NULL,
    riesgo_crisis BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_finalizacion TIMESTAMP NULL,
    CHECK (puntaje_phq9 BETWEEN 0 AND 27),
    CHECK (puntaje_gad7 BETWEEN 0 AND 21),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);

CREATE TABLE respuestas_evaluacion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    evaluacion_id CHAR(36) NOT NULL,
    instrumento ENUM('PHQ9', 'GAD7') NOT NULL,
    indice_pregunta TINYINT NOT NULL, -- PHQ9: 1-9, GAD7: 1-7 (se valida también en el dominio)
    valor_puntaje TINYINT NOT NULL,
    CHECK (indice_pregunta BETWEEN 1 AND 9),
    CHECK (valor_puntaje BETWEEN 0 AND 3),
    UNIQUE (evaluacion_id, instrumento, indice_pregunta),
    FOREIGN KEY (evaluacion_id) REFERENCES evaluaciones(id) ON DELETE CASCADE
);

CREATE TABLE registro_auditoria (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NULL,
    accion VARCHAR(50) NOT NULL, -- INICIO_SESION, CONSENTIMIENTO_ACEPTADO, CONSENTIMIENTO_REVOCADO, EVALUACION_COMPLETADA, PDF_DESCARGADO, CUENTA_ELIMINADA
    entidad_id VARCHAR(36) NULL,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE SET NULL
);
```

---

## 6. Marco Legal y Ético (Colombia)

Verificar con el docente o con alguien con formación jurídica antes de usar el sistema con personas reales. Esto es orientación, no asesoría legal.

| Norma | Relevancia |
|---|---|
| Ley 1581 de 2012 y Decreto 1377 de 2013 (compilado en el Decreto 1074 de 2015) | Protección de datos personales. Los datos de salud son **sensibles**: requieren autorización previa, expresa e informada y su suministro es facultativo |
| Ley 1616 de 2013 | Ley de salud mental |
| Ley 1090 de 2006 | Ejercicio de la psicología y código deontológico |

Requisitos derivados:

1. **Consentimiento informado y autorización de tratamiento de datos** antes de cualquier evaluación (`docs/consentimiento-v1.md`), versionado y registrado.
2. **Política de tratamiento de datos** (`docs/politica-tratamiento-datos-v1.md`), enlazada desde el consentimiento.
3. **Solo mayores de 18 años:** declaración explícita en el consentimiento. Es una declaración, no una verificación; debe quedar dicho.
4. **Derechos del titular:** conocer, actualizar, rectificar, suprimir y revocar. El sistema los implementa con la revocación del consentimiento y la eliminación de cuenta.
5. **Minimización:** solo nombre, correo y respuestas. No se guarda fecha de nacimiento ni documento de identidad.
6. **Límite declarado:** el sistema **no notifica a ningún profesional ni servicio de emergencia**. Solo muestra recursos. Debe decirse en el consentimiento, en la interfaz y en la sustentación.
7. Los teléfonos de ayuda **no se escriben en el código**: viven en configuración y se verifican antes de cada entrega.
8. Verificar con el docente si aplica el registro de bases de datos ante la SIC (probablemente no en un proyecto académico, pero confirmarlo).

Líneas de referencia a verificar (pueden cambiar): **123** (emergencias), **Línea 192 opción 4** (salud mental, nacional) y la **Línea Amiga Saludable de Medellín**. La Línea 106 es de Bogotá; no usarla como única referencia.

---

## 7. Prompt Base para Antigravity (pegar al inicio de cada sesión)

> *"Reglas del proyecto: 1) TODO el proyecto está en español: clases, métodos, variables, paquetes, tablas, columnas, rutas, campos JSON, componentes, comentarios, mensajes de error y commits, sin tildes ni ñ en los identificadores. Las únicas excepciones son las anotaciones y APIs de los frameworks, los métodos HTTP y las convenciones de nombres de archivos de prueba (`*Test` en Java, `*.test.jsx` en Vitest y `test_*.py` en pytest). 2) Es un sistema de CRIBADO, nunca de diagnóstico; no generes texto que diagnostique. 3) No inventes lógica clínica: usa exclusivamente las tablas de puntuación que te doy. 4) No escribas teléfonos de ayuda en el código: léelos de configuración. 5) La capa `dominio` no puede importar Spring, JPA ni ninguna librería de infraestructura. 6) Cada funcionalidad nueva incluye sus pruebas. 7) Nunca registres en logs respuestas del cuestionario, nombres ni correos. 8) Antes de modificar archivos existentes, explícame qué vas a cambiar."*

---

## 8. Plan de Sprints

Cada sprint termina con su **Definición de Terminado (DoD)** verificable. No se pasa al siguiente sin cumplirla. Se entrega **un sprint a la vez** a Antigravity.

### Sprint 0: Preparación (lo haces tú, sin Antigravity)

**Tareas**
1. Instalar MySQL 8 con Docker (`docker-compose.yml` incluido en el repositorio) o con el instalador MySQL Community, y crear la base `triaje_salud_mental_db` vacía. **Ojo:** XAMPP trae MariaDB, no MySQL; no lo uses para este proyecto.
2. Crear la estructura de carpetas y un repositorio Git con `.gitignore` (excluir `.env`, `target/`, `node_modules/`, `venv/`).
3. Colocar en `docs/` los documentos `consentimiento-v1.md` y `politica-tratamiento-datos-v1.md`, completando los campos entre corchetes.
4. Crear `docs/limites-del-sistema.md` con las limitaciones de la sección 9.
5. Colocar en `docs/` el documento `informacion-cuestionarios-v1.md` (qué son y qué miden el PHQ-9 y el GAD-7).
6. Copiar la migración `V1__esquema_inicial.sql` de la sección 5 a `backend/src/main/resources/db/migration/`.

**DoD:** repositorio creado, MySQL accesible, documentos base escritos y la migración colocada.

---

### Sprint 1: Backend base, seguridad y migraciones

**Tareas**
1. Inicializar Spring Boot con: *Web, Data JPA, MySQL Driver, Security, Validation, Flyway, Lombok, jjwt*.
2. Verificar que Flyway aplique `V1__esquema_inicial.sql` y configurar `spring.jpa.hibernate.ddl-auto=validate`.
3. Estructura hexagonal: `dominio`, `aplicacion`, `infraestructura`.
4. Autenticación: registro, inicio de sesión, renovación y cierre de sesión. Contraseña con BCrypt y mínimo 10 caracteres.
5. Token de acceso de 15 minutos (en memoria en el frontend). Token de renovación en cookie `httpOnly`, `SameSite=Strict`.
6. Limitación de intentos de inicio de sesión (p. ej. Bucket4j).
7. CORS explícito para `http://localhost:5173` con credenciales.
8. Manejo global de excepciones y `DELETE /api/usuarios/yo` (eliminación de cuenta y datos).

#### Prompt para Antigravity (carpeta `/backend`)
> *"Crea un proyecto Spring Boot 3 con Java 21 y arquitectura hexagonal con los paquetes `com.triaje.dominio`, `com.triaje.aplicacion` y `com.triaje.infraestructura`. Todo el código, los comentarios y los mensajes deben estar en español, sin tildes ni ñ en los identificadores. La migración Flyway `V1__esquema_inicial.sql` ya está en `src/main/resources/db/migration/`; no la modifiques. Usa `ddl-auto=validate`. Implementa autenticación con Spring Security 6: `POST /api/autenticacion/registro`, `/inicio-sesion`, `/renovar` y `/cierre-sesion`. Contraseñas con BCrypt (mínimo 10 caracteres). Token JWT de acceso de 15 minutos devuelto en el cuerpo; token de renovación en cookie httpOnly SameSite=Strict. Añade limitación de intentos al inicio de sesión, CORS explícito para `http://localhost:5173` con credenciales, manejo global de excepciones con respuestas de error uniformes en español y el endpoint `DELETE /api/usuarios/yo` que elimina al usuario y sus datos. Registra en `registro_auditoria` los eventos INICIO_SESION y CUENTA_ELIMINADA. No agregues lógica de cribado todavía. Incluye pruebas del flujo de autenticación con Testcontainers para MySQL."*

**DoD:** registro e inicio de sesión funcionan; un endpoint protegido responde 401 sin token y 200 con token; la eliminación de cuenta borra los datos en cascada; pruebas en verde.

---

### Sprint 2: Motor clínico (dominio puro)

**Tareas**
1. Crear en `dominio` las clases `EvaluadorPhq9`, `EvaluadorGad7`, `SeveridadDepresion`, `SeveridadAnsiedad`, `ReglaCrisis`, `ReglaResultadoMinimo` y `ExcepcionDominio`, **sin dependencias de Spring**.
2. Validar cantidad exacta de ítems (9 y 7) y valores entre 0 y 3.
3. Pruebas unitarias de fronteras:

| Instrumento | Valores de frontera a probar |
|---|---|
| PHQ-9 | 0, 4, 5, 9, 10, 14, 15, 19, 20, 27 |
| GAD-7 | 0, 4, 5, 9, 10, 14, 15, 21 |
| Ítem 9 | 0 (sin riesgo); 1, 2 y 3 (con riesgo) |
| Inválidos | valor -1, valor 4, lista de 8 ítems en PHQ-9 |
| Resultado mínimo | MINIMA+MINIMA sin crisis = sí; MINIMA+MINIMA **con** crisis = no (PHQ-9 con total 1 e ítem 9 = 1); LEVE+MINIMA = no; MINIMA+LEVE = no |

#### Prompt para Antigravity (carpeta `/backend`)
> *"En el paquete `com.triaje.dominio` crea la lógica de cribado como clases Java puras, sin ninguna dependencia de Spring ni JPA: `EvaluadorPhq9` (9 ítems, valores 0-3, total 0-27) y `EvaluadorGad7` (7 ítems, valores 0-3, total 0-21), con la severidad según estas tablas exactas. PHQ-9: 0-4 MINIMA, 5-9 LEVE, 10-14 MODERADA, 15-19 MODERADAMENTE_SEVERA, 20-27 SEVERA (enum `SeveridadDepresion`). GAD-7: 0-4 MINIMA, 5-9 LEVE, 10-14 MODERADA, 15-21 SEVERA (enum `SeveridadAnsiedad`). Crea `ReglaCrisis` que devuelve true si el ítem 9 del PHQ-9 es >= 1. Crea `ReglaResultadoMinimo` que devuelve true SOLO si la severidad de depresión es MINIMA, la de ansiedad es MINIMA y `ReglaCrisis` es false (si hay riesgo de crisis, nunca es resultado mínimo, aunque el puntaje total sea bajo). Lanza `ExcepcionDominio` ante valores fuera de rango o cantidad incorrecta de ítems. Escribe pruebas JUnit 5 parametrizadas (clases terminadas en `Test`, nombres de métodos en español) que cubran todas las fronteras (0,4,5,9,10,14,15,19,20,27 para PHQ-9; 0,4,5,9,10,14,15,21 para GAD-7) los casos inválidos y las combinaciones de `ReglaResultadoMinimo` (MINIMA+MINIMA sin crisis = true; MINIMA+MINIMA con crisis = false; LEVE+MINIMA = false; MINIMA+LEVE = false)."*

**DoD:** todas las fronteras probadas y en verde; `ReglaResultadoMinimo` probada, incluido el caso de ítem 9 ≥ 1 con puntaje total bajo; el paquete `dominio` compila sin dependencias de framework.

---

### Sprint 3: Casos de uso de cribado, crisis en backend y consentimiento

**Flujo con borrador** (evita perder el riesgo si la persona abandona):

| Endpoint | Función |
|---|---|
| `POST /api/consentimientos` | Registra la aceptación (con `confirmaMayoriaEdad` obligatorio en `true`) |
| `GET /api/consentimientos/vigente` | Indica si hay consentimiento vigente |
| `POST /api/consentimientos/revocar` | Revoca el consentimiento; bloquea nuevas evaluaciones |
| `POST /api/triaje/evaluaciones` | Crea evaluación `EN_CURSO` (exige consentimiento vigente) |
| `PUT /api/triaje/evaluaciones/{id}/respuestas` | Guarda una respuesta y devuelve `riesgoCrisis` |
| `POST /api/triaje/evaluaciones/{id}/finalizar` | Valida las 16 respuestas, calcula, marca `COMPLETADA` y devuelve `resultadoMinimo` |
| `GET /api/triaje/evaluaciones` y `/{id}` | Historial y detalle (solo del propio usuario) |
| `GET /api/recursos-ayuda` | Líneas de ayuda leídas desde configuración |

**Tareas**
1. Casos de uso en `aplicacion` que usen el dominio del Sprint 2.
2. Al guardar el ítem 9 con valor ≥ 1, marcar `riesgo_crisis = true` de inmediato.
3. Tarea programada (`@Scheduled`) que pasa a `ABANDONADA` las evaluaciones `EN_CURSO` con más de 24 horas.
4. Verificar siempre que la evaluación pertenezca al usuario autenticado.
5. Registrar en `consentimientos` las versiones de consentimiento y de política aceptadas (`1.0`).
6. Incluir `resultadoMinimo` (derivado con `ReglaResultadoMinimo`, sin guardarlo en la base de datos) en la respuesta de `finalizar` y en el detalle de la evaluación.

#### Prompt para Antigravity (carpeta `/backend`)
> *"Implementa en las capas `aplicacion` e `infraestructura` los casos de uso de cribado reutilizando el dominio existente. Endpoints: `POST /api/consentimientos` (exige `confirmaMayoriaEdad=true` y registra las versiones de consentimiento y de política), `GET /api/consentimientos/vigente`, `POST /api/consentimientos/revocar` (llena `fecha_revocacion` y bloquea nuevas evaluaciones), `POST /api/triaje/evaluaciones` (crea evaluación EN_CURSO y exige consentimiento vigente), `PUT /api/triaje/evaluaciones/{id}/respuestas` (guarda UNA respuesta con `instrumento`, `indicePregunta` y `valor`; si es el ítem 9 del PHQ9 con valor >= 1, marca `riesgo_crisis=true` en ese mismo momento y devuélvelo en la respuesta como `riesgoCrisis`), `POST /api/triaje/evaluaciones/{id}/finalizar` (exige las 16 respuestas, calcula puntajes y severidades con el dominio, calcula `resultadoMinimo` con `ReglaResultadoMinimo` (derivado, sin guardarlo en la base de datos), marca COMPLETADA y devuelve `resultadoMinimo` en la respuesta y en el detalle de la evaluación), `GET /api/triaje/evaluaciones` y `GET /api/triaje/evaluaciones/{id}`, y `GET /api/recursos-ayuda` que lee las líneas de ayuda desde `application.yml`. Toda operación debe verificar que la evaluación pertenezca al usuario autenticado. Añade una tarea `@Scheduled` que marque como ABANDONADA las evaluaciones EN_CURSO de más de 24 horas. Registra en `registro_auditoria`: CONSENTIMIENTO_ACEPTADO, CONSENTIMIENTO_REVOCADO y EVALUACION_COMPLETADA. Incluye pruebas de integración con Testcontainers y MockMvc, incluyendo: acceso a evaluación ajena (debe fallar), finalizar incompleta (debe fallar), evaluar sin consentimiento o con consentimiento revocado (debe fallar) y marca de crisis al guardar el ítem 9."*

**DoD:** el riesgo queda registrado aunque la persona abandone; nadie ve evaluaciones ajenas; no se puede evaluar sin consentimiento vigente; pruebas en verde.

---

### Sprint 4: Frontend funcional (sin avatar todavía)

**Componentes**
* `ContextoAutenticacion`: token de acceso en memoria, renovación mediante `/api/autenticacion/renovar`.
* `PantallaConsentimiento`: muestra el texto de `docs/consentimiento-v1.md`, enlaza la política de datos y exige dos casillas: aceptación de la autorización y declaración de mayoría de edad. Incluye la opción de **no aceptar** y salir.
* `PantallaIntroduccion`: se muestra después del consentimiento y antes del asistente, con el texto de `docs/informacion-cuestionarios-v1.md`. Un enlace "Sobre los cuestionarios" queda accesible durante toda la evaluación.
* `AsistenteTriaje`: una pregunta a la vez, 4 opciones de la escala, barra de progreso, botón **Salir** siempre visible, guardado de cada respuesta en el backend.
* `AvisoCrisis`: aviso **prioritario y no bloqueante**.
* `PanelResultados`: severidades, tarjetas informativas y botón de PDF. Muestra `MensajeResultadoMinimo` solo si `resultadoMinimo = true` y `NotaInformativa` en **todos** los resultados (textos en la sección 4). Nunca muestra `MensajeResultadoMinimo` si hay `riesgoCrisis`.
* Opción en el perfil para **revocar el consentimiento** y **eliminar la cuenta**.

**Comportamiento de `AvisoCrisis`**
1. Aparece de inmediato al elegir ≥ 1 en el ítem 9, con las líneas que devuelve `/api/recursos-ayuda`.
2. Tono sobrio, sin alarmismo, sin culpabilizar.
3. Ofrece "Continuar con la evaluación" y "Salir"; no impide seguir.
4. Reaparece en los resultados mientras `riesgoCrisis` sea verdadero.

#### Prompt para Antigravity (carpeta `/frontend`)
> *"Construye una SPA en React con Vite y Tailwind CSS, con todo el código, los componentes, los textos y los comentarios en español. Implementa: 1) Inicio de sesión y registro contra `/api/autenticacion`, guardando el token de acceso SOLO en memoria (no localStorage) y renovándolo con `/api/autenticacion/renovar` (cookie httpOnly, usa `credentials: 'include'`). 2) `PantallaConsentimiento` previa a la evaluación, que muestre el texto de `docs/consentimiento-v1.md`, enlace a `docs/politica-tratamiento-datos-v1.md`, y exija dos casillas: autorización de tratamiento de datos y declaración de ser mayor de 18 años; debe permitir no aceptar y salir. 3) `AsistenteTriaje` que muestre una sola pregunta a la vez (PHQ9 y luego GAD7) con las opciones 0 'Ningún día', 1 'Varios días', 2 'Más de la mitad de los días', 3 'Casi todos los días', barra de progreso y botón 'Salir' siempre visible; cada respuesta se envía a `PUT /api/triaje/evaluaciones/{id}/respuestas`. 4) Cuando el backend devuelva `riesgoCrisis=true`, muestra un componente `AvisoCrisis` prominente y NO bloqueante con las líneas de `GET /api/recursos-ayuda`, con tono sobrio y los botones 'Continuar' y 'Salir'. 5) `PanelResultados` con las severidades, el aviso de crisis si aplica y un botón que descargue el PDF con una petición autenticada. 6) Una sección de perfil con 'Revocar consentimiento' y 'Eliminar mi cuenta'. 7) `PantallaIntroduccion` que se muestra después del consentimiento y antes del asistente, con el texto de `docs/informacion-cuestionarios-v1.md`, y un enlace 'Sobre los cuestionarios' accesible durante toda la evaluación; antes de cada cuestionario muestra el enunciado estándar 'Durante las últimas 2 semanas, ¿con qué frecuencia le han molestado los siguientes problemas?'. 8) En `PanelResultados`, si el backend devuelve `resultadoMinimo=true`, muestra `MensajeResultadoMinimo` con este texto: 'Tus respuestas no sugieren síntomas significativos de depresión ni de ansiedad en las últimas dos semanas. Esto no descarta que estés pasando por otras dificultades: este cribado solo explora esas dos áreas. Si te sientes mal o preocupado/a, puedes consultar con un profesional de todas formas; no necesitas un puntaje alto para pedir ayuda.'; en TODOS los resultados muestra `NotaInformativa` con este texto: 'Este resultado es orientativo. No mide todas las situaciones de salud mental (por ejemplo duelo, estrés, trauma o consumo de sustancias) y un puntaje bajo no invalida cómo te sientes.'; si solo uno de los dos instrumentos es MINIMA, su tarjeta muestra 'Tus respuestas en este cuestionario no sugieren síntomas significativos.'. Si hay `riesgoCrisis=true`, NUNCA muestres `MensajeResultadoMinimo`. Cuida la accesibilidad: contraste suficiente, navegación por teclado y etiquetas ARIA. Incluye pruebas con Vitest y Testing Library (archivos `.test.jsx`) del flujo del asistente, de las casillas obligatorias del consentimiento de la aparición de `AvisoCrisis`, de que la introducción se muestre antes de empezar y de que `MensajeResultadoMinimo` no aparezca cuando hay `riesgoCrisis`."*

**DoD:** flujo completo registro → consentimiento → introducción → evaluación → resultados funciona; no se puede continuar sin las dos casillas; el aviso de crisis aparece con ítem 9 ≥ 1 y no impide continuar; pruebas en verde.

---

### Sprint 5: Microservicio de reportes e integración

**Contenido del PDF (2 páginas, sobrio)**
1. Encabezado y fecha.
2. Aviso: *"Este informe es un cribado orientativo y no constituye un diagnóstico."*
3. Tabla de resultados (puntajes y severidad).
4. Gráfico de barras horizontal con los umbrales de severidad.
5. **Síntomas reportados** (descripción de lo respondido, **sin** mapeo a categorías diagnósticas DSM-5).
6. Orientación según severidad (medicina general, psicología, EPS) con textos fijos por plantilla, **revisados por un docente o profesional de psicología**.
7. Psicoeducación general y neutral: el malestar puede tener sentidos distintos para cada persona y existen diversos enfoques de atención (incluido el psicodinámico). **Sin interpretaciones derivadas de los puntajes ni recomendación de un enfoque específico.**
8. Líneas de ayuda si hay riesgo.
9. Sección "Sobre este cribado": qué son el PHQ-9 y el GAD-7, qué miden y sus límites.
10. Nota informativa general (en todos los reportes) y, si `resultado_minimo` es verdadero, el mensaje de resultado mínimo en lugar de la orientación por severidad.

**Seguridad del microservicio:** solo escucha en red interna y exige la cabecera `X-Clave-Interna` con un secreto compartido. No guarda ni registra el contenido recibido.

#### Prompt para Antigravity (carpeta `/reportes`)
> *"Crea una API FastAPI con todos los nombres de archivos, funciones y variables en español (por ejemplo `principal.py`, `generador_pdf.py`, `plantillas_orientacion.py`). Endpoint `POST /api/reportes/generar` que reciba JSON con: nombre del paciente, fecha, puntaje_phq9, puntaje_gad7, severidades, riesgo_crisis, resultado_minimo y las líneas de ayuda. Exige la cabecera `X-Clave-Interna` comparada con la variable de entorno `CLAVE_INTERNA` y rechaza con 401 si no coincide. Con Matplotlib genera en memoria (BytesIO) un gráfico de barras horizontal con los puntajes frente a los umbrales de severidad. Con ReportLab arma un PDF sobrio de 2 páginas en español con: encabezado, aviso de que es un cribado y no un diagnóstico, tabla de resultados, gráfico, sección 'Síntomas reportados' que describa en lenguaje claro lo respondido SIN asignar diagnósticos DSM-5, orientación por severidad usando plantillas de texto fijas en `plantillas_orientacion.py`, una sección breve de psicoeducación general y neutral, y las líneas de ayuda si riesgo_crisis es verdadero. Añade también una sección 'Sobre este cribado' que explique qué son el PHQ9 y el GAD7, qué miden y sus límites; la nota informativa general en todos los reportes; y, si resultado_minimo es verdadero, el mensaje de resultado mínimo (que reemplaza a la orientación por severidad). resultado_minimo nunca puede ser verdadero si riesgo_crisis lo es. No registres en logs el contenido recibido. Devuelve `application/pdf`. Incluye pruebas con pytest (archivos `test_*.py`)."*

#### Prompt para Antigravity (carpeta `/backend`)
> *"Configura un `RestClient` hacia `http://localhost:8000` (URL y clave interna desde configuración). Implementa `GET /api/triaje/evaluaciones/{id}/pdf`: verifica que la evaluación esté COMPLETADA y pertenezca al usuario autenticado, arma el contenido incluyendo el indicador de resultado mínimo, llama al microservicio con la cabecera `X-Clave-Interna` y devuelve `ResponseEntity<byte[]>` con `Content-Type: application/pdf` y `Content-Disposition: attachment; filename=reporte-cribado.pdf`. Registra PDF_DESCARGADO en `registro_auditoria`. Maneja con un error claro en español la indisponibilidad del microservicio. Incluye una prueba con el microservicio simulado (MockRestServiceServer)."*

**DoD:** el PDF se descarga desde el frontend; sin la clave interna Python responde 401; un usuario no descarga PDFs ajenos; pruebas en verde.

---

### Sprint 6: Avatar, pulido y verificación integral

**Avatar (último, simple y opcional)**
* Estados: `BIENVENIDA`, `PREGUNTANDO`, `CRISIS`, `FINALIZADO`.
* En `CRISIS`: expresión sobria, sin animaciones alegres ni movimientos llamativos.
* Respetar `prefers-reduced-motion` y ofrecer un interruptor para ocultarlo.
* Diseño neutral, sin rasgos que impliquen una figura clínica o de autoridad.

#### Prompt para Antigravity (carpeta `/frontend`)
> *"Crea el componente `PresentadorAvatar` con SVG dinámico y cuatro estados: BIENVENIDA (cordial), PREGUNTANDO (escucha atenta), CRISIS (sobrio, sin animaciones llamativas) y FINALIZADO (reflexivo). Intégralo en `AsistenteTriaje` a la izquierda de la pregunta. Debe respetar `prefers-reduced-motion`, incluir un interruptor para ocultarlo, y el estado CRISIS debe activarse cuando `riesgoCrisis` sea verdadero. El avatar no debe tapar ni reemplazar a `AvisoCrisis`. Todo en español."*

**Verificación integral**

| # | Prueba | Resultado esperado |
|---|---|---|
| 1 | Registro → consentimiento → evaluación → resultados → PDF | Flujo completo sin errores |
| 2 | Intentar continuar sin marcar la mayoría de edad | Bloqueado |
| 3 | Ítem 9 = 0 | Sin aviso de crisis |
| 4 | Ítem 9 = 1, 2 o 3 | Aviso inmediato, no bloqueante, riesgo guardado en BD |
| 5 | Abandonar tras ítem 9 ≥ 1 | La evaluación queda con `riesgo_crisis = true` |
| 6 | Acceder a la evaluación de otra persona | 403 o 404 |
| 7 | Evaluar sin consentimiento o tras revocarlo | Rechazado |
| 8 | Llamar a Python sin `X-Clave-Interna` | 401 |
| 9 | Eliminar cuenta | Todos los datos de la persona desaparecen |
| 10 | Revisar logs | Sin respuestas, nombres ni correos |
| 11 | Revisar que no queden identificadores en inglés | Ninguno, salvo las excepciones de la sección 2 |
| 12 | Revisión de textos por docente o profesional de psicología | Aprobados |
| 13 | PHQ-9 y GAD-7 en nivel mínimo con ítem 9 = 0 | Se muestra el mensaje de resultado mínimo |
| 14 | PHQ-9 con total bajo pero ítem 9 ≥ 1 | Aviso de crisis y **nunca** el mensaje de resultado mínimo |
| 15 | Cualquier resultado | Se muestra la nota informativa |
| 16 | Inicio de una evaluación | Se muestra la introducción a los cuestionarios antes de la primera pregunta |
| 17 | PDF de un resultado mínimo | Incluye el mensaje de resultado mínimo y la sección "Sobre este cribado" |

---

## 9. Limitaciones Declaradas (incluir en `docs/limites-del-sistema.md` y defender en la sustentación)

1. Es una herramienta de cribado; no diagnostica ni sustituye la valoración profesional.
2. **No notifica a profesionales ni a servicios de emergencia**; solo muestra recursos de ayuda.
3. No cuenta con validación clínica propia; usa instrumentos validados por terceros.
4. No está pensada para reemplazar un servicio de atención de crisis.
5. La mayoría de edad es una declaración de la persona, no una verificación.
6. El rol `CLINICO` queda como trabajo futuro (revisión de resultados por un profesional).
7. El cribado explora solo depresión y ansiedad en las últimas dos semanas; no cubre otras situaciones, y un resultado mínimo no descarta malestar.

---

## 10. Fundamentación para la Sustentación

### 1. ¿Por qué cribado y no diagnóstico?
El diagnóstico exige valoración clínica presencial, descarte de causas médicas o por sustancias y evaluación del contexto. El sistema digitaliza un cribado estructurado (PHQ-9 / GAD-7) para orientar rutas de atención. Por eso el reporte describe **síntomas reportados** y no asigna categorías diagnósticas.

### 2. ¿Cómo se integran psicología y desarrollo?
* **En el diseño de la experiencia:** tono de las preguntas, avance de a una pregunta, posibilidad de salir en cualquier momento, comunicación de resultados sin etiquetar a la persona.
* **En la gestión de riesgo:** protocolo de crisis no bloqueante, sobrio, registrado en el servidor aunque la persona abandone.
* **En la psicoeducación:** texto general y neutral que reconoce la singularidad del malestar y la existencia de distintos enfoques, sin derivar interpretaciones de un puntaje.

### 3. Justificación de la arquitectura
* **Hexagonal en el backend:** la lógica clínica vive en un dominio puro, probable de forma aislada y sin depender del framework.
* **Spring Boot:** seguridad, transacciones e integridad referencial para datos sensibles.
* **Microservicio Python:** decisión deliberada para aprovechar sus librerías de visualización y generación de documentos, aislado en red interna y con clave compartida.
* **Flyway:** el esquema queda versionado y reproducible.
* **Normativa:** Ley 1581 de 2012 (datos sensibles), Ley 1616 de 2013 y Ley 1090 de 2006.

### 4. Preguntas difíciles que puede hacer el jurado

| Pregunta | Respuesta base |
|---|---|
| ¿Qué pasa si alguien marca riesgo? | Aviso inmediato con recursos, riesgo registrado en el servidor, límite declarado de que no se notifica a nadie |
| ¿Por qué no avisan a un profesional? | Sin un equipo clínico responsable no sería ético prometerlo; queda como trabajo futuro |
| ¿Cómo garantizan que son mayores de edad? | No se puede verificar; es una declaración explícita y está documentado como limitación |
| ¿Quién valida los textos del PDF? | Revisión por docente o profesional de psicología |
| ¿Qué pasa si el resultado es mínimo y la persona igual se siente mal? | Mensaje específico que aclara que el cribado solo explora dos áreas, que no descarta malestar y que puede consultar igual; nota informativa en todos los resultados |
| ¿La persona sabe qué se le está midiendo? | Introducción previa que explica qué son, qué miden y qué no hacen los cuestionarios, con enlace permanente "Sobre los cuestionarios" |
| ¿Cómo escalaría esto a un uso profesional? | Como trabajo futuro (sección 11): versión para el psicólogo, con el dominio clínico reutilizado y el rol `CLINICO` ya previsto en el modelo de datos |
| ¿Cómo protegen los datos? | Consentimiento y política de datos, minimización, BCrypt, token en memoria, auditoría, revocación, eliminación de cuenta y ausencia de datos en logs |

---

## 11. Trabajo Futuro

Líneas de extensión que **no forman parte del alcance actual**, pero que la arquitectura permite:

1. **Versión para el psicólogo en consulta:** el profesional aplica los cuestionarios a sus pacientes, recibe sus respuestas, consulta el historial y la evolución entre aplicaciones, y obtiene un informe clínico. El dominio puro (`EvaluadorPhq9`, `EvaluadorGad7`, `ReglaCrisis`) se reutiliza y el rol `CLINICO` ya está reservado en el modelo de datos. Con un profesional dentro del flujo, el riesgo de crisis dejaría de depender solo de mostrar recursos y pasaría a generar una alerta al profesional tratante.
2. **Compartir resultados con un profesional:** que la persona pueda enviar su resultado a un psicólogo, solo con su consentimiento explícito.
3. **Más instrumentos como piezas intercambiables**, de uso libre y con licencia verificada.
4. **Lista opcional "¿Hay algo más que te preocupe?"** con opciones cerradas (pendiente de decisión).

**Condiciones previas de esa versión (a resolver antes de implementarla):**
* Definir quién es el responsable del tratamiento de los datos de los pacientes (el profesional) y cómo se registra el consentimiento de cada paciente.
* Verificar si los resultados forman parte de la historia clínica; de ser así, aplican reglas de conservación y custodia (Resolución 1995 de 1999 y Ley 2015 de 2020) que entran en conflicto con la eliminación total de datos.
* Garantizar que cada psicólogo vea únicamente a sus propios pacientes.
* Definir con literatura cualquier umbral de "cambio clínicamente significativo" entre aplicaciones.
