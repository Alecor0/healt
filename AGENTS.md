# AGENTS.md - Guía, Reglas y Stack Tecnológico del Proyecto HealthMate

Este documento define las directrices operativas, principios de ingeniería y el stack tecnológico oficial para cualquier agente o desarrollador que trabaje en el proyecto **HealthMate: Sistema de Gestión Personal de Salud y Medicamentos**.

---

## 1. Reglas Fundamentales para Agentes

Cualquier contribución, cambio de código o propuesta arquitectónica debe regirse estrictamente por las siguientes directivas:

1. **Fidelidad Estricta a los Requerimientos (Cero Invención de Funcionalidades):**
   * **Jamás inventar características, flujos o funciones que no estén explícitamente declaradas en el documento o informe técnico.**
   * Todo requerimiento funcional debe ceñirse a los alcances y especificaciones aprobadas en el proyecto.

2. **Rigor e Investigación en Funcionalidades Médicas:**
   * **Para características médicas o afines (como gestión de dosis, adherencia terapéutica, registro de signos vitales, sintomatología, rangos clínicos, etc.), investigar rigurosamente en diversas fuentes confiables en internet.**
   * Las implementaciones médicas deben ser seguras, clínicamente fundamentadas, precisas y ajustadas a buenas prácticas de salud digital (mHealth).

3. **Adherencia Absoluta al Stack Tecnológico:**
   * **Jamás salirse del stack de tecnologías formalmente definido.**
   * No se permite la introducción de librerías, frameworks, bases de datos o servicios externos alternativos (por ejemplo: Flutter, React Native, Node.js, MongoDB, etc.) sin aprobación explícita. Se debe trabajar únicamente con las tecnologías autorizadas.

4. **Metodología y Principios SOLID:**
   * **Siempre seguir los principios SOLID para el diseño y elaboración del código:**
     * **S**ingle Responsibility Principle (Responsabilidad única en clases, servicios y módulos).
     * **O**pen/Closed Principle (Abierto a extensión, cerrado a modificación).
     * **L**iskov Substitution Principle (Subtipos intercambiables por sus tipos base).
     * **I**nterface Segregation Principle (Interfaces pequeñas y específicas).
     * **D**ependency Inversion Principle (Depender de abstracciones, no de implementaciones concretas).

5. **Pragmatismo y Cero Sobreingeniería (KISS y YAGNI):**
   * **Jamás realizar sobreingeniería en funciones o módulos que no lo requieran.**
   * Escribir soluciones limpias, directas, mantenibles y eficientes. No agregar capas de abstracción innecesarias ni patrones complejos donde una solución simple y estándar resuelva el problema con claridad.

6. **Jamas dejar un bloque de codigo o funcionalidad  importante sin una prueba unitaria**

---

## 2. Stack Tecnológico Oficial

### 2.1. Frontend (Multiplataforma Móvil)

| Componente | Tecnología | Versión | Propósito / Alcance |
| :--- | :--- | :--- | :--- |
| **Framework Multiplataforma** | **Kotlin Multiplatform (KMP / KMM)** | `1.9+` | Compartir la lógica de negocio (`commonMain`): modelos de datos, validaciones, casos de uso, repositorios y gestión de estado reactivo con Corrutinas y Flows. |
| **UI Nativa Android** | **Jetpack Compose** | `2024.01+` (Kotlin/JVM 11+) | Interfaces declarativas nativas para Android siguiendo lineamientos de *Google Material Design*. |
| **UI Nativa iOS** | **SwiftUI** | `iOS 15+` (Swift 5.9+) | Interfaces declarativas nativas para iOS siguiendo lineamientos de *Apple Human Interface Guidelines (HIG)*, consumiendo el framework KMP mediante wrapper. |
| **Cliente de Red / HTTP** | **Ktor Client** | `Latest stable` | Cliente HTTP asíncrono multiplataforma para comunicación con la API REST. |
| **Notificaciones Push** | **Firebase Cloud Messaging (FCM)** / **APNS** | — | FCM para notificaciones en Android; Apple Push Notification Service para iOS. |
| **Diseño y Prototipos** | **Figma** | — | Prototipos de baja y alta fidelidad según principios WCAG 2.1 AA. |
| **Pruebas Frontend** | **JUnit, kotlin.test, Robolectric, Espresso, XCTest, XCUITest** | — | Pruebas unitarias, de integración y E2E para Android e iOS. |
| **Linters y Estilo** | **Detekt** (Kotlin), **SwiftLint** (Swift) | — | Cumplimiento de estándares de código y convenciones de plataforma. |

---

### 2.2. Backend (API REST y Lógica de Negocio)

| Componente | Tecnología | Versión | Propósito / Alcance |
| :--- | :--- | :--- | :--- |
| **Lenguaje** | **Python** | `3.10+` | Tipado moderno, asincronía nativa y alto rendimiento (cumpliendo PEP 8). |
| **Framework API** | **FastAPI** | `0.115+` | API REST asíncrona, routers por dominio (`/auth`, `/usuarios`, `/medicamentos`, `/citas`, `/salud`, `/notificaciones`) y documentación interactiva OpenAPI / Swagger (`/docs`). |
| **Servidor ASGI** | **Uvicorn** | `0.30+` | Servidor ASGI de alto rendimiento para ejecutar FastAPI. |
| **Validación y Esquemas** | **Pydantic** | `v2.x` | Serialización, deserialización y validación estricta de datos de entrada/salida. |
| **ORM / Acceso a Datos** | **SQLAlchemy (async)** | `2.0+` | Mapeo objeto-relacional asíncrono con consultas parametrizadas para evitar SQL Injection. |
| **Driver de Base de Datos** | **asyncpg** | `Latest stable` | Conexión y ejecución asíncrona optimizada para PostgreSQL. |
| **Migraciones de BD** | **Alembic** | `1.13+` | Control de versiones y evolución del esquema de la base de datos. |
| **Autenticación y Criptografía** | **python-jose** + **passlib** | — | Tokens JWT seguros (*access tokens* y *refresh tokens*), RBAC (roles: paciente, cuidador, admin), y hash de contraseñas con *bcrypt* o *argon2*. |
| **Tareas en Segundo Plano** | **FastAPI Background Tasks** / **Celery** | — | Programación y despacho de recordatorios y notificaciones push. |
| **Pruebas Backend** | **pytest**, **pytest-asyncio**, **httpx**, **Locust** | — | Pruebas unitarias, asíncronas, de integración y pruebas de carga/rendimiento (Locust). |
| **Seguridad Backend** | **Bandit**, **Safety**, **OWASP ZAP**, **Snyk** | — | Análisis estático de vulnerabilidades y escaneo continuo de dependencias. |

---

### 2.3. Base de Datos y Almacenamiento en la Nube

| Componente | Tecnología | Versión | Propósito / Alcance |
| :--- | :--- | :--- | :--- |
| **Base de Datos Principal** | **PostgreSQL** | `15+` | Base de datos relacional estándar. |
| **Plataforma de BD Cloud** | **Neon Serverless Postgres** | Cloud | Escalabilidad automática (escala a cero en inactividad), copias point-in-time y branching para entornos de desarrollo, pruebas y producción. |
| **Almacenamiento de Objetos** | **Neon Blob Storage** o **AWS S3** | Cloud | Almacenamiento seguro de imágenes de perfil y exportación de reportes clínicos en PDF. |

---

### 2.4. DevOps y Gestión de Código

| Componente | Tecnología | Propósito |
| :--- | :--- | :--- |
| **Control de Versiones** | **Git** & **GitHub** | Gestión colaborativa del repositorio del proyecto. |
| **Integración y Despliegue** | **GitHub Actions** | Pipelines de CI/CD para compilación, ejecución de pruebas automáticas y despliegue. |
| **Seguridad de Red** | **TLS 1.3 / HTTPS** | Cifrado obligatorio en tránsito para todas las comunicaciones cliente-servidor. |
| **Cifrado en Reposo** | **AES-256** | Cifrado a nivel de almacenamiento y base de datos en Neon. |

---

## 3. Resumen de Dominios del Sistema

* **/auth**: Registro, inicio de sesión, refresh token, verificación de identidad y roles.
* **/usuarios**: Perfil médico digital, datos personales, contactos de emergencia y condiciones crónicas.
* **/medicamentos**: Catálogo y CRUD de medicamentos, pautas de dosificación y frecuencias.
* **/citas**: Calendario y programación de consultas con especialistas, recordatorios previos y estados.
* **/salud**: Registro y monitoreo de síntomas diarios, signos vitales y visualización de progreso/tendencias.
* **/notificaciones**: Configuración de alertas inteligentes, recordatorios auditivos/push y sincronización.
