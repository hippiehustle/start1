package com.kaos.evcryptoscanner.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.google.gson.Gson
import com.kaos.evcryptoscanner.data.local.PreferencesManager
import com.kaos.evcryptoscanner.domain.model.ScanResult
import com.kaos.evcryptoscanner.domain.model.TradeState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

class MatrixWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val preferencesManager = PreferencesManager(context)
        val cachedData = preferencesManager.cachedScanData.first()
        val scanResult = try {
            if (!cachedData.isNullOrBlank()) {
                Gson().fromJson(cachedData, ScanResult::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }

        provideContent {
            GlanceTheme {
                MatrixContent(scanResult)
            }
        }
    }

    @Composable
    private fun MatrixContent(scanResult: ScanResult?) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(androidx.glance.unit.ColorProvider(androidx.compose.ui.graphics.Color.White))
                .padding(12.dp)
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    MatrixCell("State", scanResult?.state?.displayName() ?: "NO DATA", Modifier.weight(1f))
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    MatrixCell("Coin", scanResult?.coin ?: "N/A", Modifier.weight(1f))
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    MatrixCell("Readiness", "${scanResult?.readiness ?: 0}%", Modifier.weight(1f))
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    MatrixCell("Regime", scanResult?.btcRegime ?: "N/A", Modifier.weight(1f))
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    MatrixCell(
                        "Updated",
                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(scanResult?.timestamp ?: 0)),
                        Modifier.weight(1f)
                    )
                }
            }
        }
    }

    @Composable
    private fun MatrixCell(label: String, value: String, modifier: GlanceModifier = GlanceModifier) {
        Column(modifier = modifier) {
            Text(
                text = label,
                style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = value,
                style = TextStyle(fontSize = 11.sp)
            )
        }
    }

    private fun getStateColorValue(state: TradeState) = when (state) {
        TradeState.BUY -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
        TradeState.WAIT -> androidx.compose.ui.graphics.Color(0xFFFF9800)
        TradeState.NO_TRADE -> androidx.compose.ui.graphics.Color(0xFFF44336)
        TradeState.UNKNOWN -> androidx.compose.ui.graphics.Color(0xFF9E9E9E)
    }
}

@AndroidEntryPoint
class MatrixWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MatrixWidget()
}
