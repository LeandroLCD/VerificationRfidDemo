package com.verification.demo

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

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

private enum class SearchType(val display: String, val extraKey: String) {
    ID("ID (pk)", "app.hivetire.android.extra.VEHICLE_ID"),
    LICENCE("Licence", "app.hivetire.android.extra.VEHICLE_LICENCE"),
    IDENTIFICATION("Identif.", "app.hivetire.android.extra.VEHICLE_IDENTIFICATION");

    companion object {
        fun fromDisplay(value: String?): SearchType =
            entries.firstOrNull { it.display == value } ?: ID
    }
}



private const val EXTRA_PRECHECK_PK = "app.hivetire.android.extra.PRECHECK_PK"
private const val EXTRA_PRECHECK_VEHICLE_PK = "app.hivetire.android.extra.PRECHECK_VEHICLE_PK"
private const val EXTRA_PRECHECK_DATE_TIME = "app.hivetire.android.extra.PRECHECK_DATE_TIME"
private const val EXTRA_PRECHECK_SYNC = "app.hivetire.android.extra.PRECHECK_SYNC"
private const val EXTRA_ERROR_MESSAGE = "app.hivetire.android.extra.ERROR_MESSAGE"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DemoScreen() {
    var selectedType by rememberSaveable { mutableStateOf(SearchType.ID.display) }
    var value by rememberSaveable { mutableStateOf("309") }
    var lastResult by remember { mutableStateOf<PrecheckResult?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result -> lastResult = PrecheckResult.from(result) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween){
                        ExposedDropdownMenuBox(
                            modifier = Modifier.weight(0.45f),
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
                            value = value,
                            onValueChange = { value = it },
                            label = { Text(stringResource(R.string.hint_value)) },
                            singleLine = true,
                            modifier = Modifier.weight(0.45f),
                        )
                    }


                    val activity = LocalActivity.current

                    Button(
                        onClick = {
                            val type = SearchType.fromDisplay(selectedType)
                            val intent = Intent().apply {
                                action = "app.hivetire.android.intent.PRECHECK_VEHICLE"
                                addCategory(Intent.CATEGORY_DEFAULT)
                                when (type) {
                                    SearchType.ID -> putExtra(type.extraKey, value.toIntOrNull() ?: 0)
                                    SearchType.LICENCE -> putExtra(type.extraKey, value)
                                    SearchType.IDENTIFICATION -> putExtra(type.extraKey, value)
                                }
                            }
                            try {
                                launcher.launch(intent)
                            }catch (e: ActivityNotFoundException) {
                                Toast.makeText(activity, "No se encontró HiveTire", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = value.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.btn_launch))
                    }
                }
            }

            // ---- Fila 2: respuesta del activity ----
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.result_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        AssistChip(
                            onClick = {},
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
                        if(result.errorMessage.isNullOrBlank()){
                            ResultRow(label = stringResource(R.string.hint_precheck_pk), value = result.precheckPk)
                            ResultRow(label = stringResource(R.string.hint_precheck_vehicle_pk), value = result.vehiclePk)
                            ResultRow(label = stringResource(R.string.hint_precheck_date_time), value = result.dateTime)
                            ResultRow(label = stringResource(R.string.hint_precheck_sync), value = result.isSync)
                        }else{
                            ResultRow(
                                label = stringResource(R.string.hint_error_message),
                                value = result.errorMessage
                            )
                        }
                    }
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
    val precheckPk: Int?,
    val vehiclePk: Int?,
    val dateTime: Long?,
    val isSync: Boolean?,
    val errorMessage: String?
) {
    fun statusLabel(): String =
        if (success) "OK · RESULT_OK" else "Cancelado · RESULT_CANCELED"

    companion object {
        fun from(activityResult: ActivityResult): PrecheckResult {
            val extras = activityResult.data?.extras
            return PrecheckResult(
                success = activityResult.resultCode == Activity.RESULT_OK,
                precheckPk = extras?.getInt(EXTRA_PRECHECK_PK)?.takeIf { it > 0 },
                vehiclePk = extras?.getInt(EXTRA_PRECHECK_VEHICLE_PK)?.takeIf { it > 0 },
                dateTime = extras?.getLong(EXTRA_PRECHECK_DATE_TIME)?.takeIf { it > 0L },
                isSync = extras?.getBoolean(EXTRA_PRECHECK_SYNC),
                errorMessage = extras?.getString(EXTRA_ERROR_MESSAGE)
            )
        }
    }
}