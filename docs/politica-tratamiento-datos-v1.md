# Política de Tratamiento de Datos Personales

**Versión:** 1.0
**Sistema:** Sistema Web de Cribado Clínico Preliminar y Orientación en Salud Mental
**Fecha de entrada en vigencia:** [FECHA]

> **Nota para quien completa este borrador:** reemplaza los campos entre corchetes `[ ]` y haz revisar el texto por tu docente o alguien con formación jurídica antes de usarlo con personas reales. Es un borrador de orientación basado en la Ley 1581 de 2012 y sus normas reglamentarias, no asesoría legal.

---

## 1. Identificación del responsable

* **Responsable del tratamiento:** [NOMBRE COMPLETO DEL ESTUDIANTE]
* **Contexto:** proyecto académico de [PROGRAMA / INSTITUCIÓN]
* **Domicilio:** [CIUDAD, COLOMBIA]
* **Correo para consultas y reclamos:** [CORREO]

## 2. Marco normativo

Esta política se rige por la Ley 1581 de 2012, el Decreto 1377 de 2013 (compilado en el Decreto 1074 de 2015) y demás normas que los modifiquen o complementen.

## 3. Definiciones básicas

* **Dato personal:** información vinculada o vinculable a una persona determinada.
* **Dato sensible:** dato que afecta la intimidad del titular o cuyo uso indebido puede generar discriminación; entre ellos, los datos relativos a la salud.
* **Titular:** la persona a quien pertenecen los datos.
* **Tratamiento:** cualquier operación sobre los datos (recolección, almacenamiento, uso, supresión).

## 4. Datos que se tratan

| Tipo | Datos | Carácter |
|---|---|---|
| Identificación y contacto | Nombre completo, correo electrónico | Personal |
| Cuenta | Contraseña (almacenada cifrada) | Personal |
| Resultados de cribado | Respuestas a PHQ-9 y GAD-7, puntajes y niveles de severidad, indicador de riesgo | **Sensible (salud)** |
| Consentimiento | Versiones aceptadas, fecha de aceptación y de revocación, declaración de mayoría de edad | Personal |
| Auditoría | Registros de eventos de uso (inicio de sesión, descarga de reportes, etc.) | Personal |

El sistema **no recolecta** documento de identidad, fecha de nacimiento, dirección ni teléfono.

## 5. Finalidades

1. Calcular y presentar los resultados del cribado al titular.
2. Generar el reporte en PDF solicitado por el titular.
3. Permitir la consulta del historial de evaluaciones por parte del propio titular.
4. Garantizar la seguridad del sistema y mantener un registro de auditoría.
5. Cumplir fines académicos de demostración del funcionamiento del sistema, **sin identificar a los titulares**.

Los datos no se venden, no se ceden a terceros y no se usan para publicidad.

## 6. Autorización

El tratamiento requiere la **autorización previa, expresa e informada** del titular, obtenida mediante el consentimiento informado del sistema. El suministro de datos sensibles es **facultativo**: el titular no está obligado a responder las preguntas ni a usar el sistema.

## 7. Menores de edad

El sistema está dirigido **exclusivamente a mayores de 18 años**. Cada titular debe declararlo expresamente. Si se detecta que un menor se registró, se eliminará su cuenta y sus datos.

## 8. Derechos del titular

* Conocer, actualizar y rectificar sus datos.
* Solicitar prueba de la autorización otorgada.
* Ser informado sobre el uso dado a sus datos.
* Revocar la autorización y solicitar la supresión de sus datos.
* Presentar quejas ante la Superintendencia de Industria y Comercio (SIC).

**Cómo ejercerlos:**
* Revocación y eliminación de cuenta: directamente desde la sección de perfil del sistema.
* Consultas, rectificaciones y reclamos: escribiendo a [CORREO]. Se responderá dentro de los plazos legales aplicables, que debes verificar con tu docente.

## 9. Medidas de seguridad

* Contraseñas almacenadas con cifrado irreversible (BCrypt).
* Autenticación con tokens de corta duración.
* Comunicación entre servicios internos protegida con clave compartida y sin exposición pública.
* Limitación de intentos de inicio de sesión.
* Registro de auditoría de eventos relevantes.
* Los registros técnicos (logs) **no contienen** respuestas, nombres ni correos.
* Acceso a la base de datos restringido al responsable.

Ningún sistema es completamente invulnerable; estas medidas reducen, pero no eliminan, los riesgos.

## 10. Conservación y supresión

Los datos se conservan mientras el titular mantenga su cuenta. Al eliminarla, se suprimen sus datos personales, respuestas y evaluaciones. Los registros de auditoría se conservan sin vínculo con la identidad del titular.

## 11. Limitaciones del sistema (transparencia)

* Es una herramienta de cribado, no de diagnóstico.
* **No notifica a profesionales ni a servicios de emergencia** sobre las respuestas de los titulares.
* Es un proyecto académico sin validación clínica propia.

## 12. Cambios a esta política

Cualquier cambio sustancial se publicará con una nueva versión y se solicitará nuevamente la autorización del titular cuando corresponda.
