package com.kaos.evcryptoscanner.ui.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kaos.evcryptoscanner.ui.theme.KaosEVCryptoScannerTheme

class WidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setResult(Activity.RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            KaosEVCryptoScannerTheme {
                WidgetConfigScreen(
                    onSave = { template, theme, density ->
                        saveWidgetConfig(template, theme, density)
                        finishWithSuccess()
                    },
                    onCancel = {
                        finish()
                    }
                )
            }
        }
    }

    private fun saveWidgetConfig(template: String, theme: String, density: String) {
        val prefs = getSharedPreferences("widget_prefs", MODE_PRIVATE)
        prefs.edit().apply {
            putString("widget_${appWidgetId}_template", template)
            putString("widget_${appWidgetId}_theme", theme)
            putString("widget_${appWidgetId}_density", density)
            apply()
        }
    }

    private fun finishWithSuccess() {
        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(Activity.RESULT_OK, resultValue)
        finish()
    }
}

@Composable
fun WidgetConfigScreen(
    onSave: (String, String, String) -> Unit,
    onCancel: () -> Unit
) {
    var selectedTemplate by remember { mutableStateOf("Minimal Badge") }
    var selectedTheme by remember { mutableStateOf("System") }
    var selectedDensity by remember { mutableStateOf("Medium") }

    Scaffold(
        topBar = {
            SmallTopAppBar(title = { Text("Configure Widget") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Widget Template", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            listOf("Minimal Badge", "Compact Card", "Full Plan", "Targets/Stops", "Matrix").forEach { template ->
                FilterChip(
                    selected = selectedTemplate == template,
                    onClick = { selectedTemplate = template },
                    label = { Text(template) },
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Theme", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row {
                listOf("Light", "Dark", "System").forEach { theme ->
                    FilterChip(
                        selected = selectedTheme == theme,
                        onClick = { selectedTheme = theme },
                        label = { Text(theme) },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Content Density", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row {
                listOf("Minimal", "Medium", "Dense").forEach { density ->
                    FilterChip(
                        selected = selectedDensity == density,
                        onClick = { selectedDensity = density },
                        label = { Text(density) },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = { onSave(selectedTemplate, selectedTheme, selectedDensity) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save")
                }
            }
        }
    }
}
