# Sistema Web de Cribado Clínico Preliminar y Orientación en Salud Mental

Proyecto académico que combina psicología y desarrollo de software. Es una herramienta de **cribado** con los cuestionarios PHQ-9 y GAD-7. **No diagnostica** y **no notifica a ningún profesional**; solo orienta y muestra recursos de ayuda. Está dirigido exclusivamente a **mayores de 18 años**.

## Estructura

```
├── backend/     Spring Boot (arquitectura hexagonal) - Java 21
├── frontend/    React (Vite) + Tailwind CSS
├── reportes/    Microservicio Python (FastAPI) para el informe en PDF
└── docs/        Plan, consentimiento, política de datos, límites y guías
```

## Requisitos

| Herramienta | Versión |
|---|---|
| Java | 21 |
| Maven | 3.9+ |
| Node.js | 20+ |
| Python | 3.10+ |
| MySQL | 8.0.16+ (usar Docker o MySQL Community; **XAMPP trae MariaDB y no sirve**) |
| Git | cualquiera reciente |

## Cómo empezar

1. Copia `.env.example` como `.env` y completa los valores.
2. Levanta la base de datos (ver `docs/preparar-mysql.md`).
3. Sigue el plan por sprints en `docs/plan-de-desarrollo-v4.md`, **un sprint a la vez**, verificando su definición de terminado antes de avanzar.
4. Al abrir una sesión con el asistente de código, pega primero `docs/prompt-base-antigravity.md`.

## Reglas del proyecto

* Todo en español (código, base de datos, rutas, comentarios, commits). Excepciones en el plan, sección 2.
* Sin teléfonos de ayuda escritos en el código: viven en configuración y se verifican antes de cada entrega.
* Nunca registrar en logs respuestas, nombres ni correos.

## Estado

- [ ] Sprint 0: Preparación
- [ ] Sprint 1: Backend base, seguridad y migraciones
- [ ] Sprint 2: Motor clínico (dominio puro)
- [ ] Sprint 3: Casos de uso, crisis en backend y consentimiento
- [ ] Sprint 4: Frontend funcional
- [ ] Sprint 5: Microservicio de reportes e integración
- [ ] Sprint 6: Avatar, pulido y verificación integral
