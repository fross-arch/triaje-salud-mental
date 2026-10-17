# Decisiones de Diseño y Seguridad - Sprint 1 y 1.1

### Equipo Responsable (CESDE - Medellín, Colombia)
- **Mariana Rico**
- **Bryan Rafael Mendoza**
- **Sebastián Gonzales**
- **Yadir Morales**

Este documento compila las decisiones técnicas y de seguridad aplicadas en el backend del sistema de cribado preliminar en salud mental.


## 1. Migración V2 y Almacenamiento Seguro de Tokens de Renovación

- **Inmutabilidad de V1:** La migración `V1__esquema_inicial.sql` se mantiene intacta para garantizar la consistencia del historial de la base de datos.
- **Creación de `V2__tokens_renovacion.sql`:** Se añade la tabla `tokens_renovacion` vinculada a `usuarios(id)` mediante clave foránea con `ON DELETE CASCADE`.
- **Almacenamiento exclusivo de Hashes:** La base de datos nunca almacena el token de renovación en texto claro; solo guarda el hash SHA-256 (`hash_token VARCHAR(64) UNIQUE`). El token original es generado de forma criptográficamente segura (32 bytes aleatorios codificados en Base64 URL-safe) y viaja exclusivamente hacia el navegador dentro de una cookie `httpOnly`, `SameSite=Strict` y con ruta acotada a `/api/autenticacion`.

## 2. Rotación de Tokens y Detección de Reutilización

- **Rotación en cada llamada a `/renovar`:** Cada vez que el cliente renueva su token de acceso, el token de renovación utilizado se marca inmediatamente con su `fecha_revocacion` y se emite un nuevo par de tokens.
- **Detección de reutilización (Token Theft Detection):** Si un atacante o cliente intercepta o reutiliza un token que ya fue revocado previamente, el sistema detecta la anomalía e inmediatamente revoca **todos** los tokens de renovación vigentes asociados a dicho usuario, forzando un cierre de sesión global.
- **Limitación conocida de concurrencia:** Si la misma persona usuaria tiene dos pestañas abiertas y ambas disparan la renovación simultáneamente al cumplirse los 15 minutos, la segunda pestaña presentará un token que acaba de ser revocado por la primera, provocando la revocación total de las sesiones del usuario. Esta es una compensación aceptada en favor de la seguridad estricta frente al robo de credenciales.

## 3. Manejo de Enumeración de Correos y Respuesta 409 en Registro

- **Registro (HTTP 409):** Cuando se intenta registrar un correo que ya existe en la base de datos, el sistema responde con HTTP 409 `CORREO_YA_REGISTRADO`. En este alcance no se cuenta con flujo de verificación por correo electrónico (enlace o código de confirmación), por lo que se asume esta limitación conocida de privacidad en favor de la usabilidad y retroalimentación directa a la persona usuaria.
- **Inicio de sesión anti-enumeración:** En el endpoint `/api/autenticacion/inicio-sesion`:
  - Devuelve exactamente el mismo código HTTP (`401 Unauthorized`) y el mismo mensaje (`"Correo o contraseña incorrectos."`) tanto si el correo no existe como si la contraseña es incorrecta.
  - Cuando el correo no existe, el sistema ejecuta una verificación de contraseña contra un hash ficticio de BCrypt precomputado, asegurando que los tiempos de respuesta sean uniformes y evitando ataques de canal lateral basados en temporización (timing attacks).
