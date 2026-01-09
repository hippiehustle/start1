package com.kaos.evcryptoscanner.data.worker

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kaos.evcryptoscanner.data.api.NetworkResult
import com.kaos.evcryptoscanner.data.repository.ScanRepository
import com.kaos.evcryptoscanner.ui.widget.CompactCardWidget
import com.kaos.evcryptoscanner.ui.widget.FullPlanWidget
import com.kaos.evcryptoscanner.ui.widget.MatrixWidget
import com.kaos.evcryptoscanner.ui.widget.MinimalBadgeWidget
import com.kaos.evcryptoscanner.ui.widget.TargetsStopsWidget
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull
import timber.log.Timber

@HiltWorker
class ScanWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val scanRepository: ScanRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Timber.d("ScanWorker: Fetching latest scan")
            val result = scanRepository.getLatestScan(useCache = false).firstOrNull()

            when (result) {
                is NetworkResult.Success -> {
                    Timber.d("ScanWorker: Scan fetched successfully, updating widgets")
                    updateAllWidgets()
                    Result.success()
                }
                is NetworkResult.Error -> {
                    Timber.w("ScanWorker: Failed to fetch scan: ${result.message}")
                    Result.retry()
                }
                else -> {
                    Timber.w("ScanWorker: Unexpected result")
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "ScanWorker: Error during work")
            Result.retry()
        }
    }

    private suspend fun updateAllWidgets() {
        try {
            MinimalBadgeWidget().updateAll(applicationContext)
            CompactCardWidget().updateAll(applicationContext)
            FullPlanWidget().updateAll(applicationContext)
            TargetsStopsWidget().updateAll(applicationContext)
            MatrixWidget().updateAll(applicationContext)
        } catch (e: Exception) {
            Timber.e(e, "Error updating widgets")
        }
    }
}
