package com.kaos.evcryptoscanner.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.kaos.evcryptoscanner.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetCustomizerScreen(navController: NavController) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(context.getString(R.string.widget_customizer_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "Available Widgets",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            WidgetTemplateCard(
                name = context.getString(R.string.template_minimal),
                description = context.getString(R.string.widget_minimal_badge_desc)
            )

            Spacer(modifier = Modifier.height(8.dp))

            WidgetTemplateCard(
                name = context.getString(R.string.template_compact),
                description = context.getString(R.string.widget_compact_card_desc)
            )

            Spacer(modifier = Modifier.height(8.dp))

            WidgetTemplateCard(
                name = context.getString(R.string.template_full_plan),
                description = context.getString(R.string.widget_full_plan_desc)
            )

            Spacer(modifier = Modifier.height(8.dp))

            WidgetTemplateCard(
                name = context.getString(R.string.template_targets),
                description = context.getString(R.string.widget_targets_stops_desc)
            )

            Spacer(modifier = Modifier.height(8.dp))

            WidgetTemplateCard(
                name = context.getString(R.string.template_matrix),
                description = context.getString(R.string.widget_matrix_desc)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "How to Add Widgets",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "1. Long-press on your home screen\n" +
                                "2. Tap 'Widgets'\n" +
                                "3. Find 'Kaos EV Scanner'\n" +
                                "4. Drag a widget to your home screen\n" +
                                "5. Configure and tap 'Save'",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Widget Quick Edit",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Long-press any widget to quickly edit:\n" +
                                "• Template style\n" +
                                "• Theme (light/dark/system)\n" +
                                "• Content density\n" +
                                "• Auto refresh settings\n" +
                                "• Show/hide readiness & timestamps",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun WidgetTemplateCard(name: String, description: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
