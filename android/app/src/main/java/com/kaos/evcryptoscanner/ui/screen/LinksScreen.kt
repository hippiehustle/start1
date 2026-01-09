package com.kaos.evcryptoscanner.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kaos.evcryptoscanner.R
import com.kaos.evcryptoscanner.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinksScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val baseUrl by viewModel.baseUrl.collectAsStateWithLifecycle(initialValue = "")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(context.getString(R.string.links_title)) },
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
                text = "Quick Links",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LinkCard(
                title = context.getString(R.string.open_coinbase),
                description = "Trade on Coinbase",
                icon = Icons.Default.OpenInBrowser,
                onClick = { openUrl(context, "coinbase://") }
            )

            Spacer(modifier = Modifier.height(8.dp))

            LinkCard(
                title = context.getString(R.string.open_flyio),
                description = baseUrl.ifBlank { "Configure in Settings" },
                icon = Icons.Default.OpenInBrowser,
                onClick = {
                    val flyioUrl = baseUrl.replace("https://", "").replace(".fly.dev", "")
                        .let { "https://fly.io/apps/$it" }
                    openUrl(context, flyioUrl)
                },
                enabled = baseUrl.isNotBlank()
            )

            Spacer(modifier = Modifier.height(8.dp))

            LinkCard(
                title = context.getString(R.string.copy_backend_url),
                description = baseUrl.ifBlank { "Not configured" },
                icon = Icons.Default.ContentCopy,
                onClick = {
                    if (baseUrl.isNotBlank()) {
                        copyToClipboard(context, baseUrl)
                        Toast.makeText(context, context.getString(R.string.url_copied), Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = baseUrl.isNotBlank()
            )
        }
    }
}

@Composable
fun LinkCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(end = 16.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Unable to open link", Toast.LENGTH_SHORT).show()
    }
}
