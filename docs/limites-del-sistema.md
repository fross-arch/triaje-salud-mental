# Límites del Sistema y Reglas de Negocio

**Versión:** 2.0  
**Fecha de actualización:** 17 de octubre de 2026  
**Sistema:** Sistema Web de Cribado Clínico Preliminar y Orientación en Salud Mental  

### Responsables del Proyecto (CESDE - Medellín, Colombia)
- **Mariana Rico**
- **Bryan Rafael Mendoza**
- **Sebastián Gonzales**
- **Yadir Morales**

---

Este documento declara con claridad lo que el sistema **no** es ni hace, así como las fronteras operativas y reglas de negocio esenciales. Debe mantenerse en estricta coherencia con el consentimiento informado, la interfaz de usuario, las pruebas automatizadas y la sustentación académica.

## 1. Límites Clínicos y Éticos Fundamentales

1. **Herramienta de cribado, no de diagnóstico:** Es una herramienta de tamizaje preliminar psicométrico (PHQ-9 y GAD-7); no emite diagnósticos médicos ni psicológicos ni sustituye la valoración clínica presencial o especializada de un profesional de la salud.
2. **Sin notificación pasiva a emergencias:** El sistema **no notifica automáticamente a profesionales ni a servicios de emergencia**. Ante una señal de crisis (ej. ítem 9 de PHQ-9 o ideación suicida), se activa de inmediato el protocolo visual de crisis con líneas telefónicas oficiales para que la persona usuaria busque ayuda de manera activa e inmediata.
3. **Validación de instrumentos:** No cuenta con validación psicométrica propia; utiliza versiones de cuestionarios (PHQ-9 y GAD-7) previamente adaptadas y validadas culturalmente para Colombia y Latinoamérica por equipos de investigación externos.
4. **No es servicio de atención en crisis:** La plataforma no dispone de guardia clínica 24 horas ni de teleasistencia reactiva; es un canal orientativo y de articulación con recursos de ayuda.
5. **Declaración de mayoría de edad:** La condición de ser mayor de 18 años se fundamenta en la declaración expresa bajo fe de juramento del usuario durante la aceptación del consentimiento informado, no mediante validación biométrica o de registro civil.
6. **Alcance acotado de los instrumentos:** El cribado explora exclusivamente sintomatología referida a depresión y ansiedad en las últimas dos semanas. No evalúa duelo, estrés postraumático, trastornos psicóticos, afecciones del sueño aisladas ni consumo de sustancias psicoactivas. Un resultado "mínimo" no descarta malestar emocional subjetivo ni impide consultar a un profesional.
7. **Privacidad en el endpoint de registro:** El registro responde HTTP 409 si el correo electrónico ya se encuentra en uso, lo que constituye una limitación conocida de privacidad en favor de la usabilidad y la retroalimentación inmediata, en tanto no se cuente con un servicio de confirmación diferida de correo vía tokens temporales.

---

## 2. Gestión de Citas Clínicas y Máquina de Estados

El rol `CLINICO` permite a profesionales de la salud mental registrados acceder a los resultados de evaluaciones que los pacientes autoricen voluntariamente compartir, y coordinar citas de orientación y triaje profesional.

### Ciclo de vida y máquina de estados de las citas
Las citas de orientación se rigen por una máquina de estados estricta y finita:

* **PROPUESTA:** Cita creada exclusivamente por un clínico hacia un paciente sobre una evaluación compartida previamente autorizada.
* **CONFIRMADA:** Cita aceptada de forma explícita y voluntaria por el paciente. Solo el paciente titular tiene atribución para confirmar una cita propuesta.
* **RECHAZADA:** Cita declinada por el paciente (estado terminal). Solo el paciente titular puede rechazarla.
* **CANCELADA:** Cita anulada antes de su ejecución. Puede ser cancelada tanto por el paciente como por el clínico asignado con motivo justificado (estado terminal).
* **CUMPLIDA:** Cita llevada a cabo satisfactoriamente (estado terminal). La transición es ejecutada exclusivamente por el clínico tratante.

Transiciones permitidas:
- `PROPUESTA` $\rightarrow$ `CONFIRMADA` (paciente)
- `PROPUESTA` $\rightarrow$ `RECHAZADA` (paciente)
- `PROPUESTA` $\rightarrow$ `CANCELADA` (paciente o clínico)
- `PROPUESTA` $\rightarrow$ `CUMPLIDA` (clínico)
- `CONFIRMADA` $\rightarrow$ `CANCELADA` (paciente o clínico)
- `CONFIRMADA` $\rightarrow$ `CUMPLIDA` (clínico)

### Registro de observaciones clínicas al dar cumplimiento
* Al transicionar una cita hacia el estado `CUMPLIDA`, el profesional tratante debe registrar de manera formal y obligatoria las **observaciones clínicas** pertinentes del encuentro.
* El sistema estampa la marca de tiempo de cumplimiento (`fechaCumplimiento`) y emite un evento inmutable de auditoría (`CITA_CUMPLIDA`).

### Imposibilidad de reabrir citas canceladas o finalizadas
* Los estados `CANCELADA`, `RECHAZADA` y `CUMPLIDA` son **estados terminales definitivos**.
* **Bajo ninguna circunstancia es posible reabrir, reactivar o volver a transicionar una cita cancelada o cumplida.** Si las partes requieren una nueva sesión, el clínico debe proponer una nueva cita sobre una evaluación compartida vigente.

---

## 3. Política de Evaluación Única Activa

Para proteger la validez clínica del proceso de cribado, evitar sesgos por repetición inmediata o duplicidad diagnóstica y salvaguardar la relación paciente-profesional:

* **Bloqueo de nuevas evaluaciones ante cita activa:** Un paciente no puede iniciar ni enviar nuevas evaluaciones de cribado mientras mantenga una cita en estado `PROPUESTA` o `CONFIRMADA` pendiente de resolución o atención.
* **Bloqueo ante evaluación compartida en espera clínica:** Si el paciente tiene una autorización vigente para compartir una evaluación con un profesional y aún no se ha cumplido la atención clínica correspondiente, no se le permite iniciar una nueva evaluación hasta que culmine el ciclo de atención.

---

## 4. Regla de Bloqueo de Revocación de Consentimiento

El sistema implementa mecanismos estrictos de control sobre la revocación del consentimiento informado conforme a la Ley 1581 de 2012:

* **Bloqueo por evaluación en curso:** Si el usuario tiene una evaluación en estado `EN_CURSO` (cuestionarios en diligenciamiento sin finalizar ni descartar), el sistema **bloquea inmediatamente la solicitud de revocación** (generando HTTP 409 con el mensaje: *"Hay una evaluación en curso; debes finalizarla o cancelarla antes de revocar el consentimiento"*). El usuario debe concluir el flujo o abandonar la evaluación explícitamente antes de revocar.
* **Efectos en cascada al revocar válidamente:** Cuando la revocación se procesa con éxito (sin evaluaciones en curso):
  1. El consentimiento informado actual queda revocado.
  2. Todas las autorizaciones de acceso clínico vigentes del paciente se revocan en cascada de forma automática.
  3. Todas las citas en estado `PROPUESTA` asociadas a dichas autorizaciones quedan automáticamente canceladas (`CANCELADA`). Las citas ya `CONFIRMADA` se preservan para garantizar el deber de atención previamente pactado.
