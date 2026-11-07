# HealthMate

Aplicación Android para la gestión personal de salud y medicamentos, desarrollada
con Kotlin y Jetpack Compose.

## Estructura del proyecto

```text
.
├── AGENTS.md                      # Reglas y stack tecnológico del proyecto
├── docs/
│   └── requirements.md             # Requerimientos, invariantes y casos límite
├── app/
│   ├── build.gradle.kts            # Configuración del módulo Android
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/healmate/
│       │   │   ├── MainActivity.kt # Punto de entrada de Android
│       │   │   ├── data/
│       │   │   │   └── repository/ # Implementación y datos en memoria
│       │   │   ├── domain/
│       │   │   │   ├── model/      # Entidades y estados, un tipo por archivo
│       │   │   │   ├── repository/ # Contratos de acceso a datos
│       │   │   │   └── validation/ # Validaciones y cálculos existentes
│       │   │   └── ui/
│       │   │       ├── components/ # Componentes visuales compartidos
│       │   │       ├── navigation/ # Rutas de navegación
│       │   │       ├── screens/
│       │   │       │   ├── appointments/
│       │   │       │   ├── auth/
│       │   │       │   ├── dashboard/
│       │   │       │   ├── health/
│       │   │       │   ├── main/
│       │   │       │   ├── medications/
│       │   │       │   └── profile/
│       │   │       ├── theme/      # Colores y tema de Compose
│       │   │       └── viewmodel/  # Estado y acciones de la interfaz
│       │   ├── keepRules/          # Reglas de conservación de recursos
│       │   └── res/                # Recursos nativos de Android
│       ├── test/java/com/healmate/
│       │   ├── data/repository/    # Pruebas del repositorio
│       │   └── domain/validation/  # Pruebas de validaciones y cálculos
│       └── androidTest/            # Pruebas instrumentadas de Android
├── gradle/                        # Wrapper, versiones y toolchain de Gradle
├── build.gradle.kts               # Plugins del proyecto
├── settings.gradle.kts            # Módulos y repositorios de dependencias
├── gradle.properties              # Propiedades de compilación
├── gradlew
└── gradlew.bat
```

Los archivos de configuración de Gradle y `AGENTS.md` permanecen en la raíz para
que las herramientas los encuentren. `.idea/` contiene la configuración de
Android Studio. Las pruebas de ejemplo generadas por Android Studio se conservan
en sus paquetes originales.

## Dónde colocar cada archivo

- **Modelos y estados:** `domain/model/`, con el nombre del tipo como nombre del archivo.
- **Contratos de repositorio:** `domain/repository/`, una interfaz por archivo.
- **Implementaciones de acceso a datos:** `data/repository/`.
- **Validaciones de negocio:** `domain/validation/`.
- **Pantallas:** la carpeta de su función dentro de `ui/screens/`.
- **Elementos visuales reutilizables:** `ui/components/`; estilos en `ui/theme/`.
- **Pruebas unitarias:** en `src/test/`, siguiendo el paquete del código probado.
- **Documentación:** `docs/`. Consultar los [requerimientos](docs/requirements.md)
  y las [reglas de contribución](AGENTS.md) antes de modificar funcionalidades.

## Abrir y verificar

Abrir la raíz del repositorio en Android Studio y sincronizar Gradle. El módulo
Android utiliza `compileSdk = 35`; la versión del JDK del daemon está declarada
en `gradle/gradle-daemon-jvm.properties`.

```sh
# Pruebas unitarias
bash gradlew testDebugUnitTest

# Compilación de depuración
bash gradlew assembleDebug
```

En Windows, utilizar `gradlew.bat` en lugar de `bash gradlew`.
