# Lista de verificación del Sprint 0

### Responsables (CESDE - Medellín, Colombia)
- **Mariana Rico**
- **Bryan Rafael Mendoza**
- **Sebastián Gonzales**
- **Yadir Morales**

Marca cada punto antes de pasar al Sprint 1.


## Repositorio
- [ ] Carpeta del proyecto creada con `backend/`, `frontend/`, `reportes/` y `docs/`
- [ ] `git init` ejecutado y primer commit realizado
- [ ] `.env` creado a partir de `.env.example` y **no** aparece en `git status`

```bash
git init
git add .
git commit -m "Sprint 0: estructura inicial del proyecto"
git status                 # .env no debe aparecer
```

## Base de datos
- [ ] MySQL 8.0.16 o superior en marcha (no MariaDB)
- [ ] Base `triaje_salud_mental_db` creada y vacía
- [ ] Usuario de aplicación creado y probado (no `root`)

## Documentos
- [ ] `docs/consentimiento-v1.md` completado (nombre, institución, correo, ubicación de datos)
- [ ] `docs/politica-tratamiento-datos-v1.md` completado (nombre, correo, ciudad, fecha)
- [ ] Teléfonos de ayuda verificados (marcados `[VERIFICAR]`)
- [ ] Referencia de la versión en español de los instrumentos completada en `docs/informacion-cuestionarios-v1.md`
- [ ] Textos enviados a revisión de un docente o profesional de psicología

Para encontrar campos sin completar:

```bash
grep -rnE "\[(NOMBRE|CORREO|FECHA|CIUDAD|UBICACI|VERIFICAR|COMPLETAR|PROGRAMA|INSTITUCI)" docs/
```

## Migración
- [ ] `backend/src/main/resources/db/migration/V1__esquema_inicial.sql` presente (no se ejecuta a mano)

## Listo para el Sprint 1
- [ ] Java 21, Maven, Node 20+, Python 3.10+ instalados (`java -version`, `mvn -v`, `node -v`, `python --version`)
