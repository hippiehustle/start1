package com.kaos.evcryptoscanner.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kaos.evcryptoscanner.R
import com.kaos.evcryptoscanner.data.api.NetworkResult
import com.kaos.evcryptoscanner.domain.model.ScanResult
import com.kaos.evcryptoscanner.domain.model.TradeState
import com.kaos.evcryptoscanner.ui.navigation.Screen
import com.kaos.evcryptoscanner.ui.theme.*
import com.kaos.evcryptoscanner.ui.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val healthState by viewModel.healthState.collectAsStateWithLifecycle()
    val runScanState by viewModel.runScanState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.checkHealth()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(context.getString(R.string.dashboard_title)) },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = { navController.navigate(Screen.Help.route) }) {
                        Icon(Icons.Default.Help, contentDescription = "Help")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    onClick = { navController.navigate(Screen.Dashboard.route) },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                    label = { Text(context.getString(R.string.nav_dashboard)) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate(Screen.Widgets.route) },
                    icon = { Icon(Icons.Default.Widgets, contentDescription = null) },
                    label = { Text(context.getString(R.string.nav_widgets)) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate(Screen.Links.route) },
                    icon = { Icon(Icons.Default.Link, contentDescription = null) },
                    label = { Text(context.getString(R.string.nav_links)) }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            HealthIndicator(healthState)

                Spacer(modifier = Modifier.height(16.dp))

                when (scanState) {
                    is NetworkResult.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                    is NetworkResult.Success -> {
                        val scanResult = (scanState as NetworkResult.Success<ScanResult>).data
                        ScanResultCard(scanResult, context)

                        Spacer(modifier = Modifier.height(16.dp))

                        ActionButtons(
                            onRefresh = { viewModel.loadLatestScan() },
                            onRunScan = { viewModel.runScan() },
                            onCopy = { copyToClipboard(context, scanResult.formattedOutput) },
                            onShare = { shareText(context, scanResult.formattedOutput) }
                        )
                    }
                    is NetworkResult.Error -> {
                        ErrorMessage((scanState as NetworkResult.Error).message)
                    }
                }

                runScanState?.let { state ->
                    Spacer(modifier = Modifier.height(16.dp))
                    when (state) {
                        is NetworkResult.Success -> {
                            Text(state.data, color = MaterialTheme.colorScheme.primary)
                            LaunchedEffect(Unit) {
                                kotlinx.coroutines.delay(3000)
                                viewModel.clearRunScanState()
                            }
                        }
                        is NetworkResult.Error -> {
                            Text(state.message, color = MaterialTheme.colorScheme.error)
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

@Composable
fun HealthIndicator(healthState: NetworkResult<com.kaos.evcryptoscanner.domain.model.HealthResponse>?) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (healthState) {
                is NetworkResult.Success -> BuyGreen.copy(alpha = 0.2f)
                is NetworkResult.Error -> NoTradeRed.copy(alpha = 0.2f)
                else -> UnknownGray.copy(alpha = 0.2f)
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (healthState is NetworkResult.Success) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                tint = if (healthState is NetworkResult.Success) BuyGreen else NoTradeRed
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (healthState is NetworkResult.Success) context.getString(R.string.backend_healthy)
                else context.getString(R.string.backend_unreachable),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun ScanResultCard(scanResult: ScanResult, context: Context) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = getStateColor(scanResult.state).copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = scanResult.state.displayName(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = getStateColor(scanResult.state)
            )

            scanResult.coin?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            scanResult.readiness?.let {
                Text(
                    text = context.getString(R.string.readiness_score, it),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            Text(
                text = scanResult.formattedOutput,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = context.getString(
                    R.string.last_updated,
                    SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(scanResult.timestamp))
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ActionButtons(
    onRefresh: () -> Unit,
    onRunScan: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(onClick = onRefresh, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text(context.getString(R.string.refresh))
        }
        Button(onClick = onRunScan, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text(context.getString(R.string.run_scan))
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(onClick = onCopy, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.ContentCopy, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text(context.getString(R.string.copy_plan))
        }
        OutlinedButton(onClick = onShare, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.Share, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text(context.getString(R.string.share_plan))
        }
    }
}

@Composable
fun ErrorMessage(message: String) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NoTradeRed.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Error, contentDescription = null, tint = NoTradeRed)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = context.getString(R.string.error_network),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

fun getStateColor(state: TradeState) = when (state) {
    TradeState.BUY -> BuyGreen
    TradeState.WAIT -> WaitOrange
    TradeState.NO_TRADE -> NoTradeRed
    TradeState.UNKNOWN -> UnknownGray
}

fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Scan Result", text))
}

fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share Scan"))
}
