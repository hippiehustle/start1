package com.kaos.evcryptoscanner.ui.screen

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kaos.evcryptoscanner.R
import com.kaos.evcryptoscanner.data.api.NetworkResult
import com.kaos.evcryptoscanner.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val baseUrl by viewModel.baseUrl.collectAsStateWithLifecycle(initialValue = "")
    val apiKey by viewModel.apiKey.collectAsStateWithLifecycle(initialValue = "")
    val autoRefreshInterval by viewModel.autoRefreshInterval.collectAsStateWithLifecycle(initialValue = 0)
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle(initialValue = false)
    val smartNotifications by viewModel.smartNotifications.collectAsStateWithLifecycle(initialValue = true)
    val connectionTestState by viewModel.connectionTestState.collectAsStateWithLifecycle()

    var localBaseUrl by remember { mutableStateOf(baseUrl) }
    var localApiKey by remember { mutableStateOf(apiKey) }
    var apiKeyVisible by remember { mutableStateOf(false) }
    var showIntervalDialog by remember { mutableStateOf(false) }

    LaunchedEffect(baseUrl) { localBaseUrl = baseUrl }
    LaunchedEffect(apiKey) { localApiKey = apiKey }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.updateNotificationsEnabled(context, true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(context.getString(R.string.settings_title)) },
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
                text = "Backend Configuration",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = localBaseUrl,
                onValueChange = { localBaseUrl = it },
                label = { Text(context.getString(R.string.base_url)) },
                placeholder = { Text(context.getString(R.string.base_url_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = localApiKey,
                onValueChange = { localApiKey = it },
                label = { Text(context.getString(R.string.api_key)) },
                placeholder = { Text(context.getString(R.string.api_key_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                        Icon(
                            imageVector = if (apiKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle visibility"
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.updateBaseUrl(localBaseUrl)
                        viewModel.updateApiKey(localApiKey)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save")
                }

                OutlinedButton(
                    onClick = { viewModel.testConnection() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(context.getString(R.string.test_connection))
                }
            }

            connectionTestState?.let { state ->
                Spacer(modifier = Modifier.height(8.dp))
                when (state) {
                    is NetworkResult.Loading -> {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    is NetworkResult.Success -> {
                        Text(state.data, color = MaterialTheme.colorScheme.primary)
                        LaunchedEffect(Unit) {
                            kotlinx.coroutines.delay(3000)
                            viewModel.clearConnectionTestState()
                        }
                    }
                    is NetworkResult.Error -> {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            Text(
                text = "Preferences",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(context.getString(R.string.auto_refresh), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = getIntervalText(context, autoRefreshInterval),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { showIntervalDialog = true }) {
                        Text("Change")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(context.getString(R.string.notifications_enabled), style = MaterialTheme.typography.bodyLarge)
                    }
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                when {
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) == PackageManager.PERMISSION_GRANTED -> {
                                        viewModel.updateNotificationsEnabled(context, true)
                                    }
                                    else -> {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                            } else {
                                viewModel.updateNotificationsEnabled(context, enabled)
                            }
                        }
                    )
                }
            }

            if (notificationsEnabled) {
                Spacer(modifier = Modifier.height(8.dp))

                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(context.getString(R.string.smart_notifications), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = context.getString(R.string.smart_notifications_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = smartNotifications,
                            onCheckedChange = { viewModel.updateSmartNotifications(it) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { showTestNotification(context) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(context.getString(R.string.test_notification))
                }
            }
        }
    }

    if (showIntervalDialog) {
        IntervalSelectionDialog(
            currentInterval = autoRefreshInterval,
            onDismiss = { showIntervalDialog = false },
            onSelect = { interval ->
                viewModel.updateAutoRefreshInterval(context, interval)
                showIntervalDialog = false
            }
        )
    }
}

@Composable
fun IntervalSelectionDialog(
    currentInterval: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    val context = LocalContext.current
    val intervals = listOf(0, 15, 30, 60, 120)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(context.getString(R.string.auto_refresh)) },
        text = {
            Column {
                intervals.forEach { interval ->
                    TextButton(
                        onClick = { onSelect(interval) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = getIntervalText(context, interval),
                            color = if (interval == currentInterval) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(context.getString(R.string.cancel))
            }
        }
    )
}

fun getIntervalText(context: Context, interval: Int): String = when (interval) {
    0 -> context.getString(R.string.interval_off)
    15 -> context.getString(R.string.interval_15)
    30 -> context.getString(R.string.interval_30)
    60 -> context.getString(R.string.interval_60)
    120 -> context.getString(R.string.interval_120)
    else -> "$interval minutes"
}

fun showTestNotification(context: Context) {
    createNotificationChannel(context)

    val notification = NotificationCompat.Builder(context, "trade_alerts")
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(context.getString(R.string.notification_test_title))
        .setContentText(context.getString(R.string.notification_test_body))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .build()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context).notify(999, notification)
        }
    } else {
        NotificationManagerCompat.from(context).notify(999, notification)
    }
}

fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            "trade_alerts",
            context.getString(R.string.notification_channel_trade_alerts),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
}
