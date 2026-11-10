# Preparar MySQL (parte manual del Sprint 0)

### Responsables (CESDE - Medellín, Colombia)
- **Mariana Rico**
- **Bryan Rafael Mendoza**
- **Sebastián Gonzales**
- **Yadir Morales**

> **XAMPP trae MariaDB, no MySQL.** Para este proyecto usa Docker o el instalador MySQL Community (8.0.16 o superior).


## Opción A: Docker (recomendada)

1. Copia `.env.example` como `.env` y completa las contraseñas.
2. Desde la raíz del proyecto:

```bash
docker compose up -d
docker compose ps
```

El contenedor crea la base `triaje_salud_mental_db` y el usuario de la aplicación automáticamente.

## Opción B: MySQL Community instalado en el equipo

Conéctate como administrador y ejecuta (cambia la contraseña):

```sql
CREATE DATABASE triaje_salud_mental_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'triaje_app'@'localhost' IDENTIFIED BY 'cambia_esta_clave_app';
GRANT ALL PRIVILEGES ON triaje_salud_mental_db.* TO 'triaje_app'@'localhost';
FLUSH PRIVILEGES;
```

## Opción C: XAMPP (MariaDB en localhost:3306) - Para backend-capas

XAMPP incluye el motor **MariaDB** (bifurcación comunitaria de MySQL). Para ejecutar `backend-capas`, MariaDB es totalmente compatible mediante el dialecto MySQL de Hibernate y generación automática de esquema (`ddl-auto=update`):

1. **Iniciar el servicio**: Abre el **XAMPP Control Panel** e inicia el módulo **MySQL** (verificando que esté escuchando en el puerto estándar `3306`).
2. **Crear la base de datos**:
   - Ingresa a phpMyAdmin en [http://localhost/phpmyadmin](http://localhost/phpmyadmin) o abre la consola de MariaDB.
   - En la pestaña **Bases de datos**, crea una base llamada `triaje_salud_mental_db` seleccionando el cotejamiento `utf8mb4_unicode_ci` (o `utf8mb4_general_ci`).
   - O ejecuta la sentencia SQL:
     ```sql
     CREATE DATABASE IF NOT EXISTS triaje_salud_mental_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
     ```
3. **Credenciales por defecto**:
   - Host: `localhost:3306`
   - Usuario: `root`
   - Contraseña: *(vacía por defecto en XAMPP)*
4. **Sin Flyway**: A diferencia del backend original, `backend-capas` **no requiere Flyway**. Hibernate creará y actualizará automáticamente las 8 tablas al arrancar la aplicación (`ddl-auto=update`).

---

## Verificación

Conéctate al motor y comprueba la versión y la base:

```sql
SELECT VERSION();            -- Muestra la versión (MySQL 8.0+ o MariaDB 10.4+)
SHOW DATABASES;              -- Debe aparecer triaje_salud_mental_db
```

* Para `backend/` (arquitectura hexagonal): usa Docker o MySQL Community 8.0+ con Flyway.
* Para `backend-capas/` (arquitectura en capas): usa MariaDB en XAMPP (puerto 3306) con `ddl-auto=update`.
* phpMyAdmin es útil para inspeccionar las tablas generadas por JPA (`usuarios`, `consentimientos`, `evaluaciones`, `respuestas_evaluacion`, `autorizaciones_acceso`, `citas`, `tokens_renovacion`, `registro_auditoria`).
