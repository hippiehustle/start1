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
import javax.inject.Inject

class MinimalBadgeWidget : GlanceAppWidget() {

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
                MinimalBadgeContent(scanResult)
            }
        }
    }

    @Composable
    private fun MinimalBadgeContent(scanResult: ScanResult?) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(getStateColor(scanResult?.state ?: TradeState.UNKNOWN))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = scanResult?.state?.displayName() ?: "NO DATA",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                scanResult?.coin?.let {
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = it,
                        style = TextStyle(fontSize = 12.sp)
                    )
                }
            }
        }
    }

    private fun getStateColor(state: TradeState) = when (state) {
        TradeState.BUY -> androidx.glance.unit.ColorProvider(androidx.compose.ui.graphics.Color(0xFF4CAF50))
        TradeState.WAIT -> androidx.glance.unit.ColorProvider(androidx.compose.ui.graphics.Color(0xFFFF9800))
        TradeState.NO_TRADE -> androidx.glance.unit.ColorProvider(androidx.compose.ui.graphics.Color(0xFFF44336))
        TradeState.UNKNOWN -> androidx.glance.unit.ColorProvider(androidx.compose.ui.graphics.Color(0xFF9E9E9E))
    }
}

@AndroidEntryPoint
class MinimalBadgeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MinimalBadgeWidget()
}
