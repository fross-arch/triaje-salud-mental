# Backend - Sistema de Cribado Clínico Preliminar en Salud Mental

Servicio backend desarrollado con Spring Boot 3 y Java 21 siguiendo una arquitectura hexagonal estricta. Es un sistema de **cribado**, nunca de diagnóstico clínico.

### Responsables del Proyecto
- Mariana Rico
- Bryan Rafael Mendoza
- Sebastián Gonzales
- Yadir Morales
*(CESDE - Medellín, Colombia)*


## Requisitos Previos

- Java 21 (JDK 21)
- MySQL 8.0 (ejecutándose en local o mediante Docker)

## Configuración y Variables de Entorno

La aplicación puede leer automáticamente las variables definidas en el archivo `.env` ubicado en la raíz del proyecto mediante la configuración `spring.config.import=optional:file:../.env[.properties]`.

Las variables requeridas son:

| Variable | Descripción | Ejemplo / Valor por defecto |
|---|---|---|
| `MYSQL_USER` | Usuario de base de datos de la aplicación | `triaje_app` |
| `MYSQL_PASSWORD` | Contraseña del usuario de base de datos | `cambia_esta_clave_app` |
| `MYSQL_DATABASE` | Nombre de la base de datos de desarrollo | `triaje_salud_mental_db` |
| `BD_HOST` | Host del servidor MySQL | `localhost` |
| `BD_PUERTO` | Puerto de conexión a MySQL | `3306` (o `3307` según la instalación local) |
| `JWT_SECRETO` | Clave secreta para firma de tokens JWT (HS256) | Debe tener **al menos 32 bytes** en UTF-8 |
| `COOKIE_SEGURA` | Flag `Secure` de la cookie de renovación | `false` en desarrollo local; **debe ser `true` en producción HTTPS** |

> **Nota de seguridad:** La aplicación no arrancará si `JWT_SECRETO` está ausente o tiene menos de 32 bytes.

## Ejecución de la Aplicación

Para iniciar el servidor en el puerto 8080 con el Maven Wrapper:

```powershell
.\mvnw.cmd spring-boot:run
```

Las migraciones de esquema se ejecutan automáticamente a través de Flyway (`V1__esquema_inicial.sql` y `V2__tokens_renovacion.sql`), y Hibernate valida el esquema con `ddl-auto=validate`.

## Ejecución de Pruebas

Para ejecutar la suite completa de pruebas:

```powershell
.\mvnw.cmd test
```

Las pruebas de integración se ejecutan contra MySQL 8 (usando Testcontainers si Docker está disponible, o el esquema independiente `triaje_prueba_db` en el puerto 3307 si no hay Docker en el host).
