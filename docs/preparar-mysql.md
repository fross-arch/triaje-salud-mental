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

## Verificación

Conéctate con el usuario de la aplicación y comprueba la versión y la base:

```sql
SELECT VERSION();            -- debe ser 8.0.16 o superior
SHOW DATABASES;              -- debe aparecer triaje_salud_mental_db
```

* La aplicación **no** debe usar el usuario `root`.
* **No ejecutes a mano** `V1__esquema_inicial.sql`: Flyway lo aplica al arrancar el backend (Sprint 1).
* phpMyAdmin, si lo usas, es solo un visor.
