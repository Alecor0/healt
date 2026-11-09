# HealthMate

- [app/](app/) contiene la app Android con Jetpack Compose y el cliente Ktor.
- [backend/](backend/) contiene la API FastAPI, las migraciones Alembic y las pruebas.
- [docs/requisitos.md](docs/requisitos.md) contiene la especificación del proyecto.

El flujo conectado permite crear una cuenta, iniciar sesión y consultar o editar el perfil personal. PostgreSQL guarda la cuenta y el perfil. El registro requiere fecha de nacimiento en formato `AAAA-MM-DD`. La opción **Explorar demostración** abre los módulos de medicamentos, citas, recordatorios y salud con datos ficticios y cambios temporales.

## Ejecutar en Windows

El backend usa el esquema de la base Neon del proyecto: `usuarios`, `perfiles`, `roles`, `usuario_rol`, `alergias`, `perfil_alergias` y `antecedentes`. Crea `backend/.env` (está en `.gitignore`, nunca lo subas) con estas variables:

```
DATABASE_URL=postgresql://USUARIO:CLAVE@HOST/neondb?sslmode=require&channel_binding=require
JWT_SECRET=<64 caracteres hexadecimales: python -c "import secrets; print(secrets.token_hex(32))">
HEALTHMATE_TEST_DATABASE_URL=postgresql://USUARIO:CLAVE@HOST/healthmate_test?sslmode=require&channel_binding=require
```

```powershell
Set-Location backend
py -3.12 -m venv .venv
& .\.venv\Scripts\python.exe -m pip install -r requirements-dev.txt
Get-Content .env | ForEach-Object { $k, $v = $_ -split '=', 2; Set-Item "env:$k" $v }

& .\.venv\Scripts\python.exe -m alembic upgrade head
& .\.venv\Scripts\python.exe -m uvicorn healthmate.main:app --host 127.0.0.1 --port 8000
```

La migración `0001_esquema_neon` describe las tablas que ya existen en Neon; en una base nueva o de pruebas las crea. En una base Neon que ya tiene esas tablas y nunca usó Alembic, márcala antes como aplicada con `python -m alembic stamp 0001_esquema_neon`. La `0002_sesiones_y_perfil` solo agrega las sesiones, los desafíos de Google y las columnas `peso`, `altura` y `contacto_emergencia` de `perfiles`.

La documentación interactiva de la API está en [Swagger local](http://127.0.0.1:8000/docs). El backend verifica el certificado y requiere TLS 1.3 para conexiones remotas.

En otra terminal, desde la raíz del repositorio, compila el APK para el emulador Android.

```powershell
.\gradlew.bat :app:assembleDebug -PhealthMateApiUrl=http://10.0.2.2:8000
```

El APK queda en `app/build/outputs/apk/debug/`. La excepción HTTP se limita a direcciones locales en compilaciones de depuración. Configura `healthMateApiUrl` con la URL HTTPS de la API al compilar para un entorno remoto. La app mantiene los tokens en memoria durante la sesión.

## Publicar la API en Render

[render.yaml](render.yaml) define el servicio `healthmate-api` (Python 3.12, región Ohio, plan gratuito). En Render elige **New > Blueprint**, conecta este repositorio y la rama, y pega la cadena de Neon en `DATABASE_URL`. Render genera `JWT_SECRET`. Cada push a esa rama vuelve a desplegar la API y aplica las migraciones pendientes al arrancar.

Con la URL HTTPS que asigna Render, compila el APK para un dispositivo real:

```powershell
.\gradlew.bat :app:assembleDebug '-PhealthMateApiUrl=https://healthmate-api.onrender.com'
```

En el plan gratuito la API se duerme tras unos 15 minutos sin uso; la primera petición después puede tardar hasta un minuto.

## Activar el inicio de sesión con Google

1. Configura Google Auth Platform en tu proyecto de Google Cloud. Si usas la audiencia externa en modo de prueba, agrega la cuenta que usará el emulador como usuario de prueba.
2. En **Clients**, crea un cliente OAuth de tipo **Android** para el paquete `com.healmate` con el SHA-1 de la firma de la app. Para una compilación de depuración, el SHA-1 aparece en `.\gradlew.bat :app:signingReport`, en la variante `debug`.
3. En el mismo proyecto de Google Cloud, crea un cliente OAuth de tipo **Web application** y copia su ID público, que termina en `.apps.googleusercontent.com`. Consulta los pasos en el [codelab de Google](https://codelabs.developers.google.com/sign-in-with-google-android).
4. En la terminal del backend, con `DATABASE_URL` y `JWT_SECRET` configurados, instala las dependencias actualizadas, aplica la migración y reinicia la API con el ID web.

```powershell
$env:GOOGLE_CLIENT_ID = 'PEGA_AQUI_EL_ID_WEB'
& .\.venv\Scripts\python.exe -m pip install -r requirements.txt
& .\.venv\Scripts\python.exe -m alembic upgrade head
& .\.venv\Scripts\python.exe -m uvicorn healthmate.main:app --host 127.0.0.1 --port 8000
```

5. En otra terminal, desde la raíz del proyecto, instala la app en un emulador con Google Play. Usa el mismo ID web en la compilación.

```powershell
.\gradlew.bat :app:installDebug '-PhealthMateApiUrl=http://10.0.2.2:8000' '-PhealthMateGoogleClientId=PEGA_AQUI_EL_ID_WEB'
```

Abre HealthMate y pulsa **Continuar con Google**. En el primer acceso, completa la fecha de nacimiento. Las cuentas de correo existentes conservan su forma de acceso. Si el correo ya está registrado, usa el método original de esa cuenta.

## Pruebas

- Android. Ejecuta `.\gradlew.bat :app:testDebugUnitTest` desde la raíz.
- Backend. Ejecuta `python -m pytest` desde `backend/` con el entorno virtual activo. Para incluir las pruebas de integración, configura `HEALTHMATE_TEST_DATABASE_URL` con una base exclusiva de pruebas y aplica primero `python -m alembic upgrade head` apuntando `DATABASE_URL` a esa base.
