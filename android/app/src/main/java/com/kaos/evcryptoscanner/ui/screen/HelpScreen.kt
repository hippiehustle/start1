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
fun HelpScreen(navController: NavController) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(context.getString(R.string.help_title)) },
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
            HelpSection(
                title = context.getString(R.string.help_states_title),
                items = listOf(
                    context.getString(R.string.help_states_buy),
                    context.getString(R.string.help_states_wait),
                    context.getString(R.string.help_states_no_trade)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            HelpSection(
                title = context.getString(R.string.help_readiness_title),
                items = listOf(context.getString(R.string.help_readiness_desc))
            )

            Spacer(modifier = Modifier.height(16.dp))

            HelpSection(
                title = context.getString(R.string.help_stops_title),
                items = listOf(context.getString(R.string.help_stops_desc))
            )

            Spacer(modifier = Modifier.height(16.dp))

            HelpSection(
                title = context.getString(R.string.help_troubleshoot_title),
                items = listOf(
                    context.getString(R.string.help_troubleshoot_no_data),
                    context.getString(R.string.help_troubleshoot_auth),
                    context.getString(R.string.help_troubleshoot_down)
                )
            )
        }
    }
}

@Composable
fun HelpSection(title: String, items: List<String>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            items.forEach { item ->
                Text(
                    text = "• $item",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}
