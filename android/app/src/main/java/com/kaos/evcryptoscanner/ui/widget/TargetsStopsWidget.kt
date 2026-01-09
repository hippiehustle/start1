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

class TargetsStopsWidget : GlanceAppWidget() {

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
                TargetsStopsContent(scanResult)
            }
        }
    }

    @Composable
    private fun TargetsStopsContent(scanResult: ScanResult?) {
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
                Text(
                    text = scanResult?.state?.displayName() ?: "NO DATA",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = androidx.glance.unit.ColorProvider(getStateColorValue(scanResult?.state ?: TradeState.UNKNOWN))
                    )
                )

                Spacer(modifier = GlanceModifier.height(8.dp))

                scanResult?.entry?.let {
                    PriceRow("Entry:", it)
                }

                scanResult?.stop?.let {
                    PriceRow("Stop:", it)
                }

                scanResult?.tp1?.let {
                    PriceRow("TP1:", it)
                }

                scanResult?.tp2?.let {
                    PriceRow("TP2:", it)
                }

                if (scanResult?.entry == null && scanResult?.stop == null) {
                    Text(
                        text = "No targets available",
                        style = TextStyle(fontSize = 11.sp)
                    )
                }
            }
        }
    }

    @Composable
    private fun PriceRow(label: String, value: Double) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "$label ",
                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold)
            )
            Text(
                text = String.format("$%.2f", value),
                style = TextStyle(fontSize = 12.sp)
            )
        }
        Spacer(modifier = GlanceModifier.height(4.dp))
    }

    private fun getStateColorValue(state: TradeState) = when (state) {
        TradeState.BUY -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
        TradeState.WAIT -> androidx.compose.ui.graphics.Color(0xFFFF9800)
        TradeState.NO_TRADE -> androidx.compose.ui.graphics.Color(0xFFF44336)
        TradeState.UNKNOWN -> androidx.compose.ui.graphics.Color(0xFF9E9E9E)
    }
}

@AndroidEntryPoint
class TargetsStopsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TargetsStopsWidget()
}
