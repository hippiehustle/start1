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

class CompactCardWidget : GlanceAppWidget() {

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
                CompactCardContent(scanResult)
            }
        }
    }

    @Composable
    private fun CompactCardContent(scanResult: ScanResult?) {
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
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = androidx.glance.unit.ColorProvider(getStateColorValue(scanResult?.state ?: TradeState.UNKNOWN))
                    )
                )

                scanResult?.coin?.let {
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = it,
                        style = TextStyle(fontSize = 14.sp)
                    )
                }

                scanResult?.readiness?.let {
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = "Readiness: $it%",
                        style = TextStyle(fontSize = 12.sp)
                    )
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                Text(
                    text = "Updated: ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(scanResult?.timestamp ?: 0))}",
                    style = TextStyle(fontSize = 10.sp)
                )
            }
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
class CompactCardWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CompactCardWidget()
}
