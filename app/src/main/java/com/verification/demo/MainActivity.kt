package com.verification.demo

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kr.pokeum.jsonviewer_compose.JsonParser
import kr.pokeum.jsonviewer_compose.ui.adapter.JsonViewerAdapter
import kr.pokeum.jsonviewer_compose.ui.values.JsonViewerColor

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                DemoScreen()
            }
        }
    }
}

private enum class SearchType(val display: String, val extraValue: Int) {
    ID("ID (pk)", 0),
    IDENTIFICATION("Identificacion.", 1),
    LICENCE("Licencia o patente", 2);

    companion object {
        fun fromDisplay(value: String?): SearchType =
            entries.firstOrNull { it.display == value } ?: ID
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DemoScreen() {
    var selectedType by rememberSaveable { mutableStateOf(SearchType.ID.display) }
    var truckValue by rememberSaveable { mutableStateOf("103") }
    var trailerValue by rememberSaveable { mutableStateOf("309") }
    var lastResult by remember { mutableStateOf<PrecheckResult?>(null) }

    val result = remember {
        mutableStateOf(false)
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { tack ->
        lastResult = PrecheckResult.from(tack)
        result.value = true
    }

    Scaffold { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
                Text(
                    text = "Verification RFID Demo",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Lanza PrecheckActivity desde la app de HiveTire y observa la respuesta.",
                    style = MaterialTheme.typography.bodyMedium
                )


            AnimatedContent(result.value) {state->
                if(!state){
                    // ---- Fila 1: controles para disparar la verificación ----
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Configura la verificación",
                                style = MaterialTheme.typography.titleMedium
                            )

                            var dropdownExpanded by remember { mutableStateOf(false) }
                            val currentType = SearchType.fromDisplay(selectedType)
                            Column{
                                ExposedDropdownMenuBox(
                                    expanded = dropdownExpanded,
                                    onExpandedChange = { dropdownExpanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = currentType.display,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text(stringResource(R.string.hint_select_type)) },
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                                        },
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = dropdownExpanded,
                                        onDismissRequest = { dropdownExpanded = false }
                                    ) {
                                        SearchType.entries.forEach { type ->
                                            DropdownMenuItem(
                                                text = { Text(type.display) },
                                                onClick = {
                                                    selectedType = type.display
                                                    dropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = truckValue,
                                    onValueChange = { truckValue = it },
                                    label = { Text("TRUCK") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = trailerValue,
                                    onValueChange = { trailerValue = it },
                                    label = { Text("TRAILER") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }


                            val activity = LocalActivity.current

                            Button(
                                onClick = {
                                    val type = SearchType.fromDisplay(selectedType)
                                    val intent = Intent().apply {
                                        action = "app.hivetire.android.intent.PRECHECK_VEHICLE"
                                        addCategory(Intent.CATEGORY_DEFAULT)
                                        //Define el tipo de busqueda para los vehiculos en el Precheo
                                        putExtra(EXTRA_PRECHECK_TYPE, type.extraValue)
                                        when(type.extraValue){
                                            0 ->{
                                                //Agrega el id 0 pk del truck que es un integer
                                                if (truckValue.isNotBlank()){
                                                    putExtra(EXTRA_TRUCK_ID, truckValue.toInt())
                                                }
                                                //Agrega el id 0 pk del trailer que es un integer
                                                if(trailerValue.isNotBlank()){
                                                    putExtra(EXTRA_TRAILER_ID, trailerValue.toInt())
                                                }
                                            }
                                            1->{
                                                //Agrega el truck EXTRA_TRUCK_IDENTIFICATION
                                                if (truckValue.isNotBlank()){
                                                    putExtra(EXTRA_TRUCK_IDENTIFICATION, truckValue)
                                                }
                                                //Agrega el trailer EXTRA_TRAILER_IDENTIFICATION
                                                if(trailerValue.isNotBlank()){
                                                    putExtra(EXTRA_TRAILER_IDENTIFICATION, trailerValue)
                                                }
                                            }
                                            2->{
                                                //Agrega el EXTRA_TRUCK_LICENCE
                                                if (truckValue.isNotBlank()){
                                                    putExtra(EXTRA_TRUCK_LICENCE, truckValue)
                                                }
                                                //Agrega el EXTRA_TRAILER_LICENCE
                                                if(trailerValue.isNotBlank()){
                                                    putExtra(EXTRA_TRAILER_LICENCE, trailerValue)
                                                }
                                            }
                                        }

                                    }
                                    try {
                                        launcher.launch(intent)
                                    }catch (e: ActivityNotFoundException) {
                                        Toast.makeText(activity, "HiveTire Not Install", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = trailerValue.isNotBlank() || truckValue.isNotEmpty(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.btn_launch))
                            }
                        }
                    }

                }else{
                    // ---- Fila 2: respuesta del activity ----
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(onClick = {
                                        result.value = false
                                    }) {
                                        Icon(Icons.Filled.ArrowBackIosNew, contentDescription = null)
                                    }
                                    Text(
                                        text = stringResource(R.string.result_title),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                AssistChip(
                                    onClick = { },
                                    label = {
                                        Text(
                                            text = lastResult?.statusLabel()
                                                ?: stringResource(R.string.empty_result),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                )
                            }
                            HorizontalDivider()

                            val result = lastResult
                            if (result == null) {
                                Text(
                                    text = stringResource(R.string.empty_result),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            } else {
                                result.truck?.let {
                                    val json = remember {
                                        JsonParser.Builder().build().parse(it)
                                    }
                                    Log.i("TruckTrailer truck",it)
                                    Log.i("TruckTrailer truck",json.toString())
                                    ResultRow(label = "TRUCK", value = truckValue)
                                    JsonViewerAdapter(
                                        jsonElement = json,
                                        keyColor = JsonViewerColor(
                                            color = Color.Black,
                                            darkModeColor = Color.White
                                        ),
                                        splitterColor = JsonViewerColor(
                                            color = Color.Black,
                                            darkModeColor = Color.White
                                        )
                                    )
                                }
                                result.trailer?.let {
                                    Log.i("TruckTrailer truck",it)
                                    val json = remember {
                                        JsonParser.Builder().build().parse(it)
                                    }
                                    Log.i("TruckTrailer truck",json.toString())
                                    ResultRow(label = "TRAILER", value = truckValue)
                                    JsonViewerAdapter(
                                        jsonElement = json,
                                        keyColor = JsonViewerColor(
                                            color = Color.Black,
                                            darkModeColor = Color.White
                                        ),
                                        splitterColor = JsonViewerColor(
                                            color = Color.Black,
                                            darkModeColor = Color.White
                                        )
                                    )
                                }
                                result.truckError?.let { error->
                                    Column {
                                        Text(
                                            text = stringResource(R.string.mensaje_de_error, "TRUCK"),
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = error,
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall
                                        )

                                    }
                                }
                                result.trailerError?.let {error->
                                    Column {
                                        Text(
                                            text = stringResource(R.string.mensaje_de_error, "TRAILER"),
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = error,
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall
                                        )

                                    }
                                }

                            }
                        }}
                }
            }

        }
    }
}

@Composable
private fun ResultRow(label: String, value: Any?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                text = "$label:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value?.toString().takeUnless { it.isNullOrBlank() } ?: "—",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private data class PrecheckResult(
    val success: Boolean,
    val truck: String?,
    val trailer: String?,
    val truckError: String?,
    val trailerError: String?
) {
    fun statusLabel(): String =
        if (success) "OK · RESULT_OK" else "Cancelado · RESULT_CANCELED"

    companion object {
        fun from(activityResult: ActivityResult): PrecheckResult {
            val extras = activityResult.data?.extras
            return PrecheckResult(
                success = activityResult.resultCode == Activity.RESULT_OK,
                truck = extras?.getString(EXTRA_TRUCK_RESULT),
                trailer = extras?.getString(EXTRA_TRAILER_RESULT),
                truckError = extras?.getString(EXTRA_TRUCK_ERROR),
                trailerError = extras?.getString(EXTRA_TRAILER_ERROR)
            )
        }
    }
}