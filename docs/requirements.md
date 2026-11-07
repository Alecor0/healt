# Especificación de Requerimientos, Invariantes y Casos Límite
## Proyecto: HealthMate - Sistema de Gestión Personal de Salud y Medicamentos

Este documento consolida la extracción formal de **Requerimientos Funcionales (RF)**, **Requerimientos No Funcionales (RNF)**, **Invariantes del Sistema** y **Casos Límite (Edge Cases)** identificados en el informe técnico del proyecto HealthMate.

---

## 1. Requerimientos Funcionales (RF)

| ID | Nombre | Descripción Oficial | Entidades / Parámetros Clave |
| :--- | :--- | :--- | :--- |
| **RF-01** | **Gestión de Perfil de Usuario** | El sistema debe permitir al usuario crear, ver, editar y eliminar su perfil digital. | Nombre, fecha de nacimiento, peso (kg), altura (cm), alergias, condiciones médicas crónicas y contacto de emergencia. |
| **RF-02** | **Gestión de Medicamentos** | El usuario debe poder agregar, consultar, modificar, pausar y eliminar medicamentos de su catálogo personal. | Nombre, dosis (ej. 500mg), frecuencia (ej. cada 8 horas), horarios de toma, fecha de inicio, fecha de fin y propósito. |
| **RF-03** | **Sistema de Recordatorios** | El sistema debe generar notificaciones push y alertas audibles en los horarios configurados para cada medicamento. El usuario podrá interactuar marcando el estado de la dosis. | Horas programadas, días de la semana, estados de dosis: `"Tomada"`, `"Omitida"` o `"Pospuesta"`. |
| **RF-04** | **Gestión de Citas Médicas** | El usuario debe poder programar, visualizar en calendario mensual/diario, editar y cancelar citas médicas con recordatorios preventivos. | Fecha, hora, nombre del especialista, centro médico / lugar, estado de confirmación y notas adicionales. Alerta preventiva 24h antes. |
| **RF-05** | **Registro de Síntomas y Signos Vitales** | El usuario podrá registrar diariamente parámetros de salud mediante controles visuales interactivos y notas. | Escala visual de dolor (1 al 10), estado de ánimo (emoticones), presión arterial (sistólica/diastólica), glucosa (mg/dL), peso (kg), pulso (lpm) y notas. |
| **RF-06** | **Visualización de Historial y Progreso** | El sistema debe generar gráficos y reportes simples basados en los datos registrados de síntomas y signos vitales para observar tendencias temporales. | Gráficos evolutivos semanales/mensuales, cálculo de porcentaje de adherencia terapéutica. |
| **RF-07** | **Exportación de Datos** | El sistema debe permitir exportar el historial consolidado de medicación y salud a un documento en formato PDF para compartir con profesionales sanitarios. | Generación de reporte en PDF almacenado en la nube (Neon Blob / AWS S3) y descargable localmente. |
| **RF-08** | **Autenticación y Seguridad** | El usuario debe poder registrarse, iniciar sesión de forma segura y recuperar contraseña usando email y password con manejo de tokens. | Email único, contraseña encriptada, access token JWT, refresh token y control de acceso basado en roles (*paciente, cuidador, admin*). |

---

## 2. Requerimientos No Funcionales (RNF)

| Categoría | Requisito No Funcional | Criterio de Aceptación / Restricción Técnica |
| :--- | :--- | :--- |
| **Usabilidad** | **Facilidad de Uso e Intuición** | • El tiempo para que un usuario nuevo registre su primer medicamento **no debe exceder los 3 minutos**.<br>• Cumplimiento de guías de diseño nativas: *Material Design* (Android) y *Human Interface Guidelines* (iOS).<br>• Cumplimiento de estándares de accesibilidad **WCAG 2.1 AA** (contraste, fuentes legibles para adultos mayores).<br>• Puntuación SUS (*System Usability Scale*) **> 80 puntos**. |
| **Rendimiento** | **Tiempos de Respuesta y Latencia** | • Las pantallas principales del frontend deben cargarse en **menos de 2 segundos**.<br>• Latencia de respuesta de la API (p95) **< 200 ms** (en pruebas: 145 ms).<br>• Capacidad de procesamiento **> 300 req/s** (en pruebas: 420 req/s con 500 usuarios concurrentes en Locust).<br>• Activación de recordatorios push en el horario exacto con tolerancia de **±1 minuto** (latencia push < 1.5 s).<br>• Consumo de batería móvil inferior al **5%** por hora continua de uso (en pruebas: 4.1%).<br>• Consumo de memoria RAM móvil **< 150 MB**. |
| **Confiabilidad** | **Disponibilidad del Servicio** | • Disponibilidad del sistema de al menos **99.5%** (tiempo fuera de línea planificado por mantenimiento no superior a 4 horas al mes).<br>• Base de datos Neon Serverless con alta disponibilidad operativa registrada del **99.9%**. |
| **Seguridad** | **Protección de Datos Sanitarios (IPS)** | • Cifrado en tránsito obligatorio mediante **HTTPS / TLS 1.3**.<br>• Cifrado en reposo para datos sensibles y base de datos con **AES-256**.<br>• Almacenamiento de contraseñas mediante hashing seguro unidireccional (**bcrypt** o **argon2**), nunca texto claro.<br>• Sanitización y validación estricta de entradas en frontend y backend (Pydantic / SQLAlchemy parametrizado) para prevenir SQLi y XSS.<br>• Cumplimiento de marcos normativos: LFPDPPP (México), NOM-004-SSA3-2012, NOM-024-SSA3-2012, GDPR y directrices de referencia HIPAA. |
| **Compatibilidad** | **Soporte de Plataformas Móviles** | • Soporte completo para las dos últimas versiones principales de sistemas operativos móviles:<br>  - **iOS:** iOS 17 e iOS 18 (mínimo iOS 15+).<br>  - **Android:** Android 13 y Android 14. |
| **Portabilidad / Disponibilidad Local** | **Operabilidad Offline y Sincronización** | • Las funcionalidades críticas (**consultar medicamentos, visualizar citas y marcar dosis como tomadas/omitidas**) deben funcionar **sin conexión a internet**.<br>• Sincronización automática de datos hacia el servidor backend tan pronto se restablezca la conectividad. |

---

## 3. Invariantes del Sistema

Las invariantes representan condiciones lógicas y reglas de negocio que el software debe mantener verdaderas en todo momento:

### 3.1. Invariantes de Identidad y Seguridad
1. **Unicidad de Cuenta:** Ningún usuario puede registrarse con una dirección de correo electrónico que ya exista en el sistema (`email UNIQUE NOT NULL`).
2. **Cero Texto Plano en Credenciales:** Ninguna contraseña de usuario residirá en memoria o base de datos en texto plano; invariablemente se transformará mediante funciones criptográficas de hash salado (`bcrypt` o `argon2`).
3. **Aislamiento Multi-inquilino de Datos Médicos (Data Segregation):** Todo acceso a lecturas o mutaciones sobre medicamentos, citas, recordatorios y signos vitales debe estar asociado al `id_usuario` autenticado en el token JWT, garantizando que un usuario jamás acceda a los registros médicos de otro salvo delegación explícita de rol (*cuidador*).

### 3.2. Invariantes del Modelo de Datos y Medicación
4. **Integridad Temporal del Tratamiento:** Para cualquier relación `Usuario_Medicamento`, si la `fecha_fin` está definida, debe cumplirse invariablemente que:
   $$\text{fecha\_fin} \ge \text{fecha\_inicio}$$
5. **No Orfandad de Recordatorios:** Todo `Recordatorio` debe estar estrictamente vinculado a una relación `Usuario_Medicamento` existente. La eliminación o desvinculación debe resolver en cascada o preservar la integridad de registros pasados.
6. **Estados Discretos de Dosis:** Cada entrada en la tabla `Dosis_Tomada` solo puede poseer uno de los estados canónicos definidos:
   $$\text{estado} \in \{\text{"Tomada"}, \text{"Omitida"}, \text{"Pospuesta"}\}$$
7. **Inmutabilidad del Historial Clínico de Dosis:** Una vez que una dosis ha sido consolidada en el servidor como `"Tomada"` u `"Omitida"` con su `fecha_hora_tomada`, no puede ser editada ni eliminada arbitrariamente; únicamente admite adición de notas o auditoría de rectificación.

### 3.3. Invariantes de Citas y Calendario
8. **Validez Temporal de Citas:** No es admisible programar nuevas citas médicas en fechas y horas pasadas respecto al momento de creación.
9. **Campos Mandatorios de Cita:** Toda cita debe tener obligatoriamente `fecha_hora`, `especialista` y `lugar` definidos (`NOT NULL`).

### 3.4. Invariantes de Transacciones Offline
10. **Preservación FIFO en Cola Offline:** Las acciones registradas en el cliente sin conexión (ej. registrar toma de pastilla) deben almacenarse localmente con timestamp original inmutable y despacharse en orden cronológico estricto hacia el backend al reconectar.

---

## 4. Casos Límite y Condiciones de Borde (Edge Cases)

Los siguientes escenarios críticos deben ser contemplados, mitigados y probados exhaustivamente:

### 4.1. Conectividad y Sincronización Offline Prolongada
* **Escenario:** El usuario permanece sin conexión a internet durante varios días (ej. zonas rurales o vuelo prolongado) y registra múltiples tomas, omisiones o notas.
* **Comportamiento esperado:** La aplicación móvil debe almacenar cada evento en caché y cola local persistente. Al restablecer la red:
  * El backend debe procesar los eventos respetando la fecha/hora local en que ocurrió el evento, sin sobrescribir con la hora de recepción del servidor.
  * Si el token de sesión expiró en el periodo offline, la app debe intentar renovar vía `refresh_token` en segundo plano sin interrumpir la sincronización ni perder los datos capturados.

### 4.2. Cambio de Zona Horaria y Horario de Verano (DST)
* **Escenario:** El paciente viaja a otro país con diferente huso horario o se produce la transición del horario de verano/invierno.
* **Comportamiento esperado:** 
  * Los medicamentos programados por intervalos absolutos (ej. antibiótico "cada 8 horas exactas") deben conservar el espaciado biológico en UTC.
  * Los medicamentos programados por horario social (ej. "con el desayuno a las 08:00") deben alertar en la hora local del dispositivo, notificando opcionalmente al usuario sobre el desfase de viaje para evitar sobredosis.

### 4.3. Reposición y Acción Consecutiva de Pospuesto ("Snooze")
* **Escenario:** El usuario presiona reiteradamente "Posponer" (ej. cada 10 o 15 minutos) hasta que la hora pospuesta alcanza o se superpone con la siguiente dosis programada del mismo medicamento.
* **Comportamiento esperado:** El sistema debe alertar al usuario sobre el riesgo de acumulación de dosis y consultar si desea fusionar la toma o registrar la anterior como `"Omitida"` antes de registrar la nueva toma.

### 4.4. Fin de Tratamiento y Medicamentos Inactivos
* **Escenario:** Se alcanza la `fecha_fin` configurada para un medicamento con recordatorios activos.
* **Comportamiento esperado:**
  * El sistema debe suspender automáticamente la emisión de nuevas notificaciones push o alarmas a partir de las 23:59:59 de dicha fecha.
  * El medicamento debe transferirse automáticamente a la sección visual de *"Inactivos / Finalizados"*.
  * El historial previo de dosis no debe verse afectado ni borrado.

### 4.5. Eliminación de Medicamento con Historial Clínico Preexistente
* **Escenario:** El usuario elimina un medicamento del cual tiene semanas o meses de dosis registradas en `Dosis_Tomada`.
* **Comportamiento esperado:** No se debe ejecutar un borrado físico en cascada que destruya el historial de adherencia (`FOREIGN KEY CASCADE` ciego). Se debe implementar un borrado lógico (*soft delete*) o desvinculación segura para mantener los reportes estadísticos y exportaciones en PDF consistentes.

### 4.6. Valores Fisiológicos Atípicos o Erróneos en Signos Vitales (RF-05)
* **Escenario:** El usuario comete un error tipográfico ingresando valores extremos o físicamente incompatibles (ej. Presión arterial: 1200/80 mmHg, Pulso: 0 lpm, Glucosa: -10 mg/dL, Escala de dolor: 15).
* **Comportamiento esperado:**
  * Validación en frontend antes de permitir guardar, con advertencias visuales de valores fuera de rangos biológicos estándar.
  * Validación estricta en esquemas Pydantic del backend retornando error `422 Unprocessable Entity` si los valores violan los límites definidos.

### 4.7. Múltiples Alertas Simultáneas en el Mismo Minuto
* **Escenario:** Varios medicamentos distintos o una cita y un medicamento están configurados exactamente para la misma hora (ej. 08:00 AM).
* **Comportamiento esperado:** El sistema de notificaciones no debe colapsar ni omitir ninguna alerta. Debe agrupar las notificaciones de manera legible o encadenar las alertas sonoras de forma ordenada respetando el margen de tolerancia de ±1 minuto.

### 4.8. Generación de Reportes PDF Extensos (RF-07)
* **Escenario:** Un paciente con años de historial solicita exportar un PDF con cientos de registros y gráficas de síntomas.
* **Comportamiento esperado:** 
  * La generación en backend debe efectuarse de forma asíncrona mediante background tasks para no bloquear la API ni exceder tiempos de espera HTTP (> 2s).
  * La descarga móvil debe realizarse por streaming o descarga de archivo directo desde la nube (S3 / Neon Blob), evitando desbordar el límite de memoria del cliente móvil (< 150 MB).
