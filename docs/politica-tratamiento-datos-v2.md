# Política de Tratamiento de Datos Personales

**Versión:** 2.0  
**Sistema:** Sistema Web de Cribado Clínico Preliminar y Orientación en Salud Mental  
**Fecha de entrada en vigencia:** 17 de octubre de 2026  

---

## 1. Identificación de los Responsables del Tratamiento

El tratamiento de los datos personales en esta plataforma es gestionado de manera conjunta por el equipo responsable del proyecto:

* **Responsables del tratamiento:**
  - **Mariana Rico**
  - **Bryan Rafael Mendoza**
  - **Sebastián Gonzales**
  - **Yadir Morales**
* **Contexto institucional:** Proyecto formativo y académico del programa Técnico en Desarrollo de Software, CESDE.
* **Domicilio:** Medellín, Antioquia, Colombia.
* **Canales para el ejercicio de derechos y consultas:** Directamente desde la plataforma en la sección de Perfil o mediante solicitud al equipo del proyecto.

---

## 2. Marco Normativo

La presente política se rige por la Constitución Política de Colombia (artículo 15), la Ley Estatutaria 1581 de 2012, el Decreto Reglamentario 1377 de 2013 (incorporado en el Decreto Único Reglamentario 1074 de 2015) y las directrices emitidas por la Superintendencia de Industria y Comercio (SIC) en materia de Protección de Datos Personales y Habeas Data.

---

## 3. Definiciones Básicas

* **Dato personal:** Cualquier información vinculada o susceptible de ser vinculada a una o varias personas naturales determinadas o determinables.
* **Dato sensible:** Aquel dato que afecta la intimidad del titular o cuyo uso indebido puede generar discriminación; en particular, información sobre el estado de salud física o psicológica, vida emocional y sintomatología subjetiva.
* **Titular:** Persona natural cuyos datos personales son objeto de tratamiento.
* **Tratamiento:** Cualquier operación técnica o conjunto de operaciones sobre datos personales, tales como recolección, almacenamiento, uso, consulta, circulación o supresión.
* **Autorización:** Consentimiento previo, expreso e informado del titular para llevar a cabo el tratamiento de sus datos personales.

---

## 4. Categorías de Datos Objeto de Tratamiento

| Categoría | Datos Tratados | Naturaleza Jurídica |
|---|---|---|
| **Identificación y contacto** | Nombre completo y correo electrónico | Personal público/semiprivado |
| **Credenciales y acceso** | Contraseña protegida mediante hash irreversible (BCrypt) | Personal privado |
| **Resultados de cribado** | Respuestas individuales a PHQ-9 y GAD-7, puntajes totales, severidades derivadas e indicador de riesgo de crisis | **Sensible (salud mental)** |
| **Consentimiento y autorizaciones** | Registro de versión aceptada, estampas de tiempo de aceptación/revocación, declaración de mayoría de edad, autorizaciones de acceso a profesionales clínicos y estado de citas | Personal privado |
| **Trazabilidad y auditoría** | Eventos del sistema (inicios de sesión, aceptación de consentimientos, compartición de evaluaciones, cambios en citas, generación de reportes) | Técnico / Seguridad |

> **Principio de minimización:** El sistema no recolecta número de documento de identidad, fecha de nacimiento, número telefónico, dirección física ni datos biométricos.

---

## 5. Finalidades Específicas del Tratamiento

1. **Cribado psicométrico:** Procesar las respuestas a los cuestionarios estandarizados PHQ-9 y GAD-7 para calcular puntajes orientativos y clasificarlos según baremos clínicos validados.
2. **Generación de reportes:** Producir informes en formato PDF descargables exclusivamente a solicitud del titular.
3. **Consulta de historial:** Permitir al titular revisar la cronología y evolución de sus evaluaciones en su panel privado.
4. **Interacción con profesionales clínicos autorizados:** Facultar la consulta selectiva del reporte por parte de un profesional de la salud registrado en el sistema, única y exclusivamente cuando el titular otorgue una *Autorización para Compartir*, permitiendo la coordinación de citas de orientación.
5. **Seguridad y auditoría:** Mantener registros de auditoría técnica que garanticen la integridad del servicio y la protección contra accesos no autorizados.
6. **Fines académicos demostrativos:** Evidenciar el funcionamiento técnico del software en el marco evaluativo de CESDE, garantizando siempre el anonimato y la confidencialidad.
7. **Estadísticas agregadas, métricas de actividad y reportes consolidados para el rol de administración:** Generar indicadores demográficos, clínicos globales y métricas de actividad operativa del sistema a través del tablero de control y sus módulos de exportación. El administrador únicamente accede a datos consolidados y agregados. Al componente de análisis estadístico solo viajan registros anonimizados (fechas a nivel de día en zona horaria local `America/Bogota`, sin hora, puntajes numéricos y severidades categóricas, y tipos de evento de auditoría agregados), sin identificadores de usuario, nombres, correos ni respuestas individuales a cada ítem.
   - **Métricas de pacientes y flujo asistencial:** Las métricas de pacientes (registros, consentimientos, evaluaciones completadas y citas confirmadas) aplican estrictamente la regla de **umbral mínimo de grupo** ($N \ge 5$). Si el período seleccionado cuenta con menos de $N$ casos o eventos, las celdas se suprimen o enmascaran rotulándose como `<N` para evitar cualquier deducción por resta o reidentificación residual.
   - **Gestión de personal clínico:** Los indicadores de gestión de cuentas de profesionales clínicos (altas, activaciones, desactivaciones y eliminaciones) son conteos exactos de gestión administrativa institucional, sin desglose ni perfilamiento individual.
   - **Descargas y exportaciones (CSV y PDF):** Ninguna descarga ni reporte exportable contiene listas de personas, nombres, correos, documentos ni identificadores individuales. El archivo CSV generado utiliza codificación UTF-8 con BOM, delimitador punto y coma (`;`), y sanitización obligatoria contra inyección de fórmulas CSV (prefijando `'` si un campo inicia con `=`, `+`, `-` o `@`). El informe PDF generado incorpora tablas agregadas, gráficos decodificados y advertencias explícitas de confidencialidad. Toda exportación genera un evento de auditoría `TABLERO_EXPORTADO`.

Los datos **jamás** se comercializan, se ceden a terceros con fines publicitarios ni se emplean para perfilamiento comercial.

---

## 6. Carácter Facultativo del Suministro de Datos Sensibles

Conforme al artículo 6 de la Ley 1581 de 2012, el tratamiento de datos sensibles requiere autorización explícita. Se informa expresamente al titular que **no está obligado a responder las preguntas ni a suministrar datos sobre su estado de ánimo o salud mental**. Su participación es estrictamente libre y voluntaria, pudiendo abandonar la evaluación en cualquier instante mediante el botón "Salir".

---

## 7. Exclusividad para Mayores de Edad

La plataforma está diseñada **exclusivamente para personas mayores de 18 años**. Cada usuario declara formalmente su mayoría de edad al otorgar el consentimiento. Si se identificara el registro de una persona menor de edad, se procederá a la cancelación inmediata de la cuenta y supresión de sus registros conforme a la ley.

---

## 8. Derechos del Titular y Canales de Ejercicio

El titular de los datos cuenta con las siguientes prerrogativas de ley:

* **Conocer, actualizar y rectificar** sus datos personales.
* **Solicitar prueba** de la autorización otorgada.
* **Ser informado** sobre el uso que se ha dado a su información.
* **Revocar la autorización y solicitar la supresión** de sus datos cuando no medie un deber legal o contractual de permanencia.
* **Presentar quejas** ante la Superintendencia de Industria y Comercio (SIC).

### Reglas y mecanismos para ejercerlos:
* **Supresión y eliminación de cuenta:** Puede ejecutarse de manera autónoma desde el perfil de usuario, suprimiendo las credenciales, respuestas y evaluaciones asociadas.
* **Revocación del consentimiento:** Se realiza directamente desde la configuración del perfil.

### Regla de bloqueo de revocación de consentimiento:
> [!IMPORTANT]
> **Evaluaciones en curso:** Si el titular tiene una evaluación en estado `EN_CURSO` (cuestionarios iniciados sin finalizar ni descartar), el sistema **bloquea la solicitud de revocación del consentimiento** (HTTP 409). Para poder revocar, el usuario debe finalizar o cancelar/abandonar previamente dicha evaluación.
>
> **Efectos en cascada de la revocación válida:** Al procesarse la revocación del consentimiento general:
> 1. Se revocan en cascada y de manera inmediata todas las autorizaciones vigentes otorgadas a profesionales clínicos para consultar evaluaciones.
> 2. Se cancelan automáticamente todas las citas en estado `PROPUESTA` vinculadas a dichas autorizaciones. Las citas en estado `CONFIRMADA` se preservan para garantizar el deber ético y asistencial previamente coordinado.

### Política de evaluación única activa:
> [!NOTE]
> Con el propósito de salvaguardar el orden clínico y la rigurosidad del proceso de tamizaje, un paciente con citas activas en estado `PROPUESTA` o `CONFIRMADA`, o con una evaluación compartida en espera de atención clínica, **no podrá registrar nuevas evaluaciones de cribado** hasta que culmine la cita en curso o se cierre el ciclo asistencial.

---

## 9. Medidas de Seguridad de la Información

Para garantizar la confidencialidad, integridad y disponibilidad de la información:
* **Cifrado de contraseñas:** Uso de BCrypt con factor de costo adecuado.
* **Autenticación robusta:** Tokens JWT de corta duración y tokens de renovación rotativos con detección de reutilización y almacenamiento de hashes SHA-256 en cookies `HttpOnly`, `SameSite=Strict`.
* **Protección de logs técnicos:** Los registros de auditoría y logs del servidor **nunca contienen** nombres de pacientes, correos electrónicos ni respuestas ítem a ítem de los instrumentos.
* **Aislamiento de bases de datos:** Accesos restringidos exclusivamente a nivel de aplicación con credenciales dedicadas.
* **Almacenamiento:** Los datos se procesan y almacenan en entornos locales protegidos de los responsables del proyecto.

---

## 10. Conservación y Supresión

Los datos se conservan mientras el titular mantenga su cuenta activa. Cuando el titular solicita la eliminación de su cuenta, se eliminan definitivamente sus credenciales, respuestas y registros sensibles. Los eventos de auditoría técnica se desvinculan de cualquier identidad personal.

---

## 11. Cambios y Versiones de la Política

Cualquier modificación sustancial que afecte las finalidades del tratamiento o la identificación de los responsables dará lugar a una nueva versión numerada, requiriendo una nueva manifestación de autorización por parte de los titulares cuando la ley lo exija.
