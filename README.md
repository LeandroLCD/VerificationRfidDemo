# VerificationRfidDemo

Aplicación Android de demostración que consume `ExternalPrecheckActivity` desde
[`hivetire_android`](https://github.com/LeandroLCD/hivetire_android). Permite
seleccionar el identificador del vehículo (`pk`, `licence` o `identification`),
lanzar la verificación mediante `ActivityResultContracts.StartActivityForResult`
y mostrar los extras devueltos por la activity externa.

> **Nota:** esta app **no** incluye la pantalla de verificación. Sólo dispara
> `ExternalPrecheckActivity` vía `Intent` y muestra el resultado.

---

## 1. Requisitos

- Android Studio Ladybug+ / Koala
- JDK 17
- `compileSdk` 36, `minSdk` 26
- La app **HiveTire** instalada en el mismo dispositivo (cualquier flavor):
  - `app.hivetire.android.hivetireapp` (release)
  - `app.hivetire.android.hivetireapp.debug` (debug, valor por defecto en la demo)
  - `app.hivetire.android.hivetireapp.demo` (demo)

> El bloque `<queries>` del `AndroidManifest.xml` declara los paquetes de
> HiveTire para que la demo pueda resolver el `ComponentName` en Android 11+.

---

## 2. Cómo ejecutar

1. Clonar este repositorio.
2. Abrir el directorio en Android Studio.
3. Sincronizar Gradle y ejecutar la app en un dispositivo/emulador con HiveTire
   instalado y la sesión iniciada.
4. En la pantalla principal:
   - Seleccionar el tipo de búsqueda (`ID (pk)`, `Licence plate`,
     `Identification`).
   - Capturar el valor correspondiente (ej. `309`, `BDDW-73`, `BDDW73`).
   - Verificar que el paquete destino apunta al flavor correcto de HiveTire.
   - Pulsar **Iniciar verificación**.
5. HiveTire abrirá la pantalla de verificación. Al terminar:
   - **RESULT_OK** con extras del precheck si todo salió bien.
   - **RESULT_CANCELED** con `EXTRA_ERROR_MESSAGE` si el vehículo no se
     encontró, no hay red, etc.

---

## 3. Uso de `ExternalPrecheckActivity`

### 3.1 Intent a enviar

Lanzar con `ComponentName` apuntando al componente exacto:

```kotlin
val intent = Intent().apply {
    component = ComponentName(
        "app.hivetire.android.hivetireapp.debug",
        "app.hivetire.android.hivetireapp.ui.handheld.externalPrecheck.ExternalPrecheckActivity"
    )

    // Elige UNO de los siguientes extras:
    putExtra("app.hivetire.android.extra.VEHICLE_ID_PK", 309)
    // putExtra("app.hivetire.android.extra.VEHICLE_LICENCE", "BDDW-73")
    // putExtra("app.hivetire.android.extra.VEHICLE_IDENTIFICATION", "BDDW73")
}

val launcher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
        val precheckPk = result.data?.getIntExtra("app.hivetire.android.extra.PRECHECK_PK", 0)
        val vehiclePk  = result.data?.getIntExtra("app.hivetire.android.extra.PRECHECK_VEHICLE_PK", 0)
        val dateTime   = result.data?.getLongExtra("app.hivetire.android.extra.PRECHECK_DATE_TIME", 0L)
        val isSync     = result.data?.getBooleanExtra("app.hivetire.android.extra.PRECHECK_SYNC", false)
    } else {
        val error = result.data?.getStringExtra("app.hivetire.android.extra.ERROR_MESSAGE")
    }
}

launcher.launch(intent)
```

### 3.2 Extras disponibles

| Constante | Tipo | Descripción |
| --- | --- | --- |
| `app.hivetire.android.extra.VEHICLE_ID_PK` | `Int` | Identificador interno (`pk`) del vehículo. |
| `app.hivetire.android.extra.VEHICLE_LICENCE` | `String` | Patente / licence plate. |
| `app.hivetire.android.extra.VEHICLE_IDENTIFICATION` | `String` | Identificación visible del vehículo. |

Se debe enviar **uno y sólo uno**. Si se omite o ninguno es válido la activity
finaliza con `RESULT_CANCELED` y `EXTRA_ERROR_MESSAGE = "Missing vehicle identifier"`.

### 3.3 Resultado devuelto

`result.resultCode`:

- `Activity.RESULT_OK` → inspección cerrada con éxito (puede incluir
  precheck sin escaneo si el operador salió desde el dialog "Exit").
- `Activity.RESULT_CANCELED` → entrada inválida, vehículo no encontrado,
  fallo de red, sesión expirada, etc.

`result.data.extras`:

| Constante | Tipo | Presente en |
| --- | --- | --- |
| `app.hivetire.android.extra.PRECHECK_PK` | `Int` | OK |
| `app.hivetire.android.extra.PRECHECK_VEHICLE_PK` | `Int` | OK |
| `app.hivetire.android.extra.PRECHECK_DATE_TIME` | `Long` | OK |
| `app.hivetire.android.extra.PRECHECK_SYNC` | `Boolean` | OK |
| `app.hivetire.android.extra.ERROR_MESSAGE` | `String` | CANCELED |

### 3.4 Flujo interno en HiveTire

1. `ExternalPrecheckActivity` recibe el `Intent` y construye un
   `ExternalPrecheckInput` (`Pk` / `Licence` / `Identification`).
2. Estado inicial: `PrecheckVerificationState.SearchVehicle`.
3. Si la entrada es `Licence` o `Identification`, se resuelve el `pk` mediante
   `ISearchVehicleUseCase` (`searchBy = "licence"` o `"identification"`).
   Si la búsqueda falla, la activity termina con `RESULT_CANCELED`.
4. Una vez resuelto el `pk`, se llama a
   `IGetVehicleDataUseCase.getVehicleData(pk)` que sincroniza el detalle y
   los neumáticos asignados desde el backend.
5. Se abre la verificación (`IVerificationRfidUseCase.openVerification`) y se
   reutiliza la pantalla original `PrecheckScreen` para que el operador
   escanee RFID o cancele.
6. Al cerrar la verificación (`closedVerification`) se entrega el
   `DataPrechecked` resultante por `onSuccess(...)` → `RESULT_OK`.
   Cualquier excepción se entrega por `onError(throwable)` →
   `RESULT_CANCELED` con el mensaje.

### 3.5 Permisos

No se requieren permisos adicionales. La activity externa se beneficia de los
permisos ya concedidos a HiveTire (red, bluetooth, etc.).

---

## 4. Estructura del proyecto

```
VerificationRfidDemo/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/verification/demo/
│       │   └── MainActivity.kt
│       └── res/
├── build.gradle.kts
├── gradle/
│   └── libs.versions.toml
├── gradle.properties
├── settings.gradle.kts
└── README.md
```

---

## 5. Pruebas sugeridas con vehículos de ejemplo

| Tipo | Valor esperado |
| --- | --- |
| `ID (pk)` | `309` |
| `Licence plate` | `BDDW-73` |
| `Identification` | `BDDW73` |

Cualquiera de los tres debería abrir la misma inspección y devolver el mismo
`PRECHECK_PK`/`PRECHECK_VEHICLE_PK`.

---

## 6. Licencia

MIT.