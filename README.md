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
- La app **HiveTire** instalada en el mismo dispositivo (cualquier flavor)


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

Lanzar con el action `app.hivetire.android.intent.PRECHECK_VEHICLE` apuntando al componente exacto:

```kotlin
val intent = Intent().apply {
    action = "app.hivetire.android.intent.PRECHECK_VEHICLE"
    addCategory(Intent.CATEGORY_DEFAULT)

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
### Recomendaciones
De los 3 extras disponibles se recomienda usar el `app.hivetire.android.extra.VEHICLE_ID_PK`, ya que es el más
directo y fácil de usar. Los dos siguientes requieren comprobaciones con el backend, por lo que conllevan una consulta
adicional al servidor.

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
