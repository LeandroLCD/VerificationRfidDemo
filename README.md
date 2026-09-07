# VerificationRfidDemo

Aplicación Android de demostración que consume `ExternalPrecheckActivity` desde
[`hivetire_android`](https://github.com/LeandroLCD/hivetire_android). Permite
seleccionar el tipo de búsqueda y los identificadores del **truck** y del
**trailer** (por `pk`, `identification` o `licence plate`), lanzar la
verificación mediante `ActivityResultContracts.StartActivityForResult` y mostrar
el JSON devuelto por la activity externa para cada vehículo.

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
   - Seleccionar el **tipo de búsqueda** (`ID (pk)`, `Identificación`,
     `Licencia / patente`). El mismo tipo se aplica tanto al truck como al
     trailer.
   - Capturar el valor del **TRUCK** y/o del **TRAILER**. Al menos uno de los
     dos debe estar completo para habilitar el botón. Si se omite uno, la
     verificación se ejecuta sólo para el que se haya enviado.
   - Pulsar **Iniciar verificación**.
5. HiveTire abrirá la pantalla de verificación. Al terminar:
   - **RESULT_OK** con extras `TRUCK_RESULT` y/o `TRAILER_RESULT` en formato
     JSON si todo salió bien.
   - **RESULT_CANCELED** con extras `TRUCK_ERROR` / `TRAILER_ERROR` si alguno
      de los vehículos no se encontró, no hay red, etc.

---

## Demo

<video src="videos/precheck%20externo.mp4" controls width="180"></video>

*Video de la app disparando `ExternalPrecheckActivity` y mostrando la respuesta
JSON del truck y del trailer.*

> Descargalo directamente desde [`videos/precheck externo.mp4`](videos/precheck%20externo.mp4).

---

## Descarga · APK release

Para probar la app sin compilar desde el código, descargar el APK firmado de
release:

[**⬇ Descargar `app-release.apk`**](https://github.com/LeandroLCD/VerificationRfidDemo/raw/master/app/release/app-release.apk)

| Campo | Valor |
| --- | --- |
| `applicationId` | `com.verification.demo` |
| `versionCode` | `1` |
| `versionName` | `1.0` |
| Tamaño | ~13 MB |
| Ruta en el repo | `app/release/app-release.apk` |

### Instalación en el dispositivo

1. Descargar el APK al teléfono (o transferirlo vía `adb` / USB).
2. Habilitar **Instalar apps de orígenes desconocidos** para el navegador o el
   explorador de archivos (si Android lo solicita).
3. Abrir el `.apk` y pulsar **Instalar**.
4. Asegurarse de tener HiveTire instalado y con sesión iniciada antes de
   lanzar la verificación.


## 3. Uso de `ExternalPrecheckActivity`

### 3.1 Intent a enviar

Lanzar con el action `app.hivetire.android.intent.PRECHECK_VEHICLE` apuntando al componente exacto:

```kotlin
val intent = Intent().apply {
    action = "app.hivetire.android.intent.PRECHECK_VEHICLE"
    addCategory(Intent.CATEGORY_DEFAULT)

    // Tipo de búsqueda (0 = ID/pk, 1 = identification, 2 = licence).
    putExtra("app.hivetire.android.extra.PRECHECK_TYPE", 0)

    // Identificadores del TRUCK y TRAILER según el tipo elegido.
    // Cada uno es opcional: si se omite el trailer, sólo se verifica el truck.
    putExtra("app.hivetire.android.extra.TRUCK_ID", 103)
    putExtra("app.hivetire.android.extra.TRAILER_ID", 309)

    // Alternativas equivalentes para los otros tipos:
    // putExtra("app.hivetire.android.extra.TRUCK_IDENTIFICATION", "JH3333")
    // putExtra("app.hivetire.android.extra.TRAILER_IDENTIFICATION", "BDDW73")
    // putExtra("app.hivetire.android.extra.TRUCK_LICENCE", "JH-3333")
    // putExtra("app.hivetire.android.extra.TRAILER_LICENCE", "BDDW-73")
}

val launcher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
) { result ->
    val extras = result.data?.extras
    val truckJson   = extras?.getString("app.hivetire.android.extra.TRUCK_RESULT")
    val trailerJson = extras?.getString("app.hivetire.android.extra.TRAILER_RESULT")
    val truckError   = extras?.getString("app.hivetire.android.extra.TRUCK_ERROR")
    val trailerError = extras?.getString("app.hivetire.android.extra.TRAILER_ERROR")
}
```

### 3.2 Extras disponibles

#### Extras de entrada (en el `Intent`)

| Constante | Tipo | Descripción |
| --- | --- | --- |
| `app.hivetire.android.extra.PRECHECK_TYPE` | `Int` | Tipo de búsqueda aplicado tanto al truck como al trailer: `0` = `ID (pk)`, `1` = `Identificación`, `2` = `Licencia / patente`. |
| `app.hivetire.android.extra.TRUCK_ID` | `Int` | `pk` del truck (cuando `PRECHECK_TYPE = 0`). Opcional. |
| `app.hivetire.android.extra.TRUCK_IDENTIFICATION` | `String` | `identification` del truck (cuando `PRECHECK_TYPE = 1`). Opcional. |
| `app.hivetire.android.extra.TRUCK_LICENCE` | `String` | `licence_plate` del truck (cuando `PRECHECK_TYPE = 2`). Opcional. |
| `app.hivetire.android.extra.TRAILER_ID` | `Int` | `pk` del trailer (cuando `PRECHECK_TYPE = 0`). Opcional. |
| `app.hivetire.android.extra.TRAILER_IDENTIFICATION` | `String` | `identification` del trailer (cuando `PRECHECK_TYPE = 1`). Opcional. |
| `app.hivetire.android.extra.TRAILER_LICENCE` | `String` | `licence_plate` del trailer (cuando `PRECHECK_TYPE = 2`). Opcional. |

**Reglas:**

- `PRECHECK_TYPE` es obligatorio; condiciona qué extra se lee para cada
  vehículo.
- Cada vehículo es **opcional de forma independiente**: se debe enviar al
  menos uno (truck o trailer). Si se omite el trailer, la verificación se
  realiza únicamente sobre el truck y sólo se devuelve `TRUCK_RESULT`. Lo
  mismo aplica a la inversa.
- Para un mismo vehículo, sólo se acepta el extra que coincide con el
  `PRECHECK_TYPE` elegido (p. ej. con `PRECHECK_TYPE = 0` sólo se lee
  `TRUCK_ID`); cualquier otro se ignora.

#### Extras de salida (en `result.data.extras`)

| Constante | Tipo | Descripción |
| --- | --- | --- |
| `app.hivetire.android.extra.TRUCK_RESULT` | `String` (JSON) | Payload serializado con la información del truck verificado. Presente si la verificación del truck fue exitosa. |
| `app.hivetire.android.extra.TRAILER_RESULT` | `String` (JSON) | Payload serializado con la información del trailer verificado. Presente sólo si se envió un trailer y la verificación fue exitosa. |
| `app.hivetire.android.extra.TRUCK_ERROR` | `String` | Mensaje de error cuando falla la verificación del truck (vehículo no encontrado, falta de red, sesión expirada, etc.). |
| `app.hivetire.android.extra.TRAILER_ERROR` | `String` | Mensaje de error cuando falla la verificación del trailer. |

El `resultCode` puede ser `Activity.RESULT_OK` (verificación cerrada con
éxito, al menos para uno de los vehículos) o `Activity.RESULT_CANCELED` (no
se pudo verificar ninguno).

---

## 4. Estructura de la respuesta

HiveTire devuelve, para cada vehículo verificado, un **JSON** con la
información del mismo y la lista de neumáticos (`tires_assigned`) que le
pertenecen, junto con el estado de verificación de cada uno. Ejemplo real
incluido en `app/src/main/res/raw/truck_response_example`:

```json
{
  "pk": 103,
  "identification": "JH3333",
  "licence_plate": "JH-3333",
  "tires_assigned": [
    {
      "pk": 12101,
      "position_index": 0,
      "position_configuration": "spare-13144",
      "is_rfid_assigned": true,
      "number": "22010",
      "verification_state": 1
    },
    {
      "pk": 9696,
      "position_index": 5,
      "position_configuration": "2-1-1",
      "is_rfid_assigned": false,
      "number": "1000",
      "verification_state": 0
    }
  ]
}
```

### 4.1 Campos del vehículo

| Campo | Tipo | Descripción |
| --- | --- | --- |
| `pk` | `Int` | Identificador interno único del vehículo (truck o trailer) dentro de HiveTire. Es el mismo valor que se envía como `TRUCK_ID` / `TRAILER_ID`. |
| `identification` | `String` | Identificación visible del vehículo. Es el valor que se envía como `TRUCK_IDENTIFICATION` / `TRAILER_IDENTIFICATION`. |
| `licence_plate` | `String` | Patente del vehículo con el formato que se muestra en HiveTire (puede incluir guiones, p. ej. `"JH-3333"`). Es el valor que se envía como `TRUCK_LICENCE` / `TRAILER_LICENCE`. |
| `tires_assigned` | `Array<Tire>` | Lista de neumáticos asignados al vehículo, con la configuración de posición y el estado de verificación de cada uno. Ver [§4.2](#42-campos-de-cada-neumático-tire). |

### 4.2 Campos de cada neumático (`Tire`)

| Campo | Tipo | Descripción                                                                                                                                                                                                                                                                 |
| --- | --- |-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `pk` | `Int` | Identificador interno único del neumático dentro de HiveTire.                                                                                                                                                                                                               |
| `position_index` | `Int` | Índice numérico que identifica la posición física del neumático dentro de la configuración del vehículo (eje + lado). El orden y los valores dependen del `position_configuration`.                                                                                         |
| `position_configuration` | `String` | Configuración de la posición del neumático. Puede tener dos formas: (a) `"eje-lado-posición"` para posiciones activas, p. ej. `"1-1-1"`, `"2-2-2"`, `"3-2-1"`; (b) `"spare-<pk>"` para identificar un neumático de repuesto, donde `<pk>` es el `pk` del repuesto asociado. |
| `is_rfid_assigned` | `Boolean` | Indica si el neumático tiene una etiqueta RFID vinculada en HiveTire. `true` → hay RFID asignado, `false` → no hay RFID (neumático sin tag o tag sin asignar).                                                                                                              |
| `number` | `String` | Número visible / de inventario del neumático. Sirve para identificarlo visualmente en la operación.                                                                                                                                                                         |
| `verification_state` | `Int` | Estado de verificación del neumático durante el precheck: `0` = no verificado, `1` = verificado ENCONTRADO, `2` = verificado NO ENCONTRADO. |

> **Nota sobre `verification_state`:**
> - **`0` (no verificado)** se da cuando el neumático no tiene un tag RFID asignado, **o** cuando la verificación fue cancelada antes de iniciarse (por ejemplo, el operador se dio cuenta de que iba a verificar el vehículo incorrecto y abortó el flujo).
> - **`1` (verificado ENCONTRADO)** indica que el tag RFID esperado fue leído y coincide con el neumático asignado a esa posición.
> - **`2` (verificado NO ENCONTRADO)** indica que el tag RFID esperado **no** fue leído. Esto suele representar un reemplazo no autorizado, o bien un daño físico del chip RFID que imposibilita su lectura. En ambos casos se recomienda verificar manualmente/visualmente con el número de fuego del neumático, que se corresponde con el campo `number` del JSON.

---

## 5. Estructura del proyecto

```
VerificationRfidDemo/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/verification/demo/
│       │   ├── Constants.kt
│       │   └── MainActivity.kt
│       └── res/
│           └── raw/
│               └── truck_response_example      # JSON de ejemplo de un truck
├── build.gradle.kts
├── gradle/
│   └── libs.versions.toml
├── gradle.properties
├── settings.gradle.kts
├── videos/
│   └── precheck externo.mp4   # Demo del flujo de verificación
└── README.md
```

---
