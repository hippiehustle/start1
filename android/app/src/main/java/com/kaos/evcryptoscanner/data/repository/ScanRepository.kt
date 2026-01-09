package com.kaos.evcryptoscanner.data.repository

import com.google.gson.Gson
import com.kaos.evcryptoscanner.data.api.KaosApiService
import com.kaos.evcryptoscanner.data.api.NetworkResult
import com.kaos.evcryptoscanner.data.api.dto.DeviceTokenDto
import com.kaos.evcryptoscanner.data.local.PreferencesManager
import com.kaos.evcryptoscanner.domain.model.HealthResponse
import com.kaos.evcryptoscanner.domain.model.ScanResult
import com.kaos.evcryptoscanner.domain.model.TradeState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanRepository @Inject constructor(
    private val apiService: KaosApiService,
    private val preferencesManager: PreferencesManager,
    private val gson: Gson
) {

    suspend fun getLatestScan(useCache: Boolean = false): Flow<NetworkResult<ScanResult>> = flow {
        emit(NetworkResult.Loading)

        if (useCache) {
            val cachedData = preferencesManager.cachedScanData.first()
            if (!cachedData.isNullOrBlank()) {
                try {
                    val scanResult = gson.fromJson(cachedData, ScanResult::class.java)
                    emit(NetworkResult.Success(scanResult))
                    return@flow
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse cached scan data")
                }
            }
        }

        try {
            val response = apiService.getLatestScan()
            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!
                val scanResult = ScanResult(
                    state = TradeState.fromString(dto.state),
                    coin = dto.coin,
                    readiness = dto.readiness,
                    formattedOutput = dto.formattedOutput ?: "No data available",
                    entry = dto.entry,
                    stop = dto.stop,
                    tp1 = dto.tp1,
                    tp2 = dto.tp2,
                    btcRegime = dto.btcRegime,
                    timestamp = dto.timestamp ?: System.currentTimeMillis()
                )

                preferencesManager.setCachedScanData(gson.toJson(scanResult))
                emit(NetworkResult.Success(scanResult))
            } else {
                val errorMsg = "Error ${response.code()}: ${response.message()}"
                Timber.w("API error: $errorMsg")

                val cachedData = preferencesManager.cachedScanData.first()
                if (!cachedData.isNullOrBlank()) {
                    try {
                        val scanResult = gson.fromJson(cachedData, ScanResult::class.java)
                        emit(NetworkResult.Success(scanResult))
                    } catch (e: Exception) {
                        emit(NetworkResult.Error(errorMsg, response.code()))
                    }
                } else {
                    emit(NetworkResult.Error(errorMsg, response.code()))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Network error fetching scan")

            val cachedData = preferencesManager.cachedScanData.first()
            if (!cachedData.isNullOrBlank()) {
                try {
                    val scanResult = gson.fromJson(cachedData, ScanResult::class.java)
                    emit(NetworkResult.Success(scanResult))
                } catch (ex: Exception) {
                    emit(NetworkResult.Error(e.message ?: "Network error"))
                }
            } else {
                emit(NetworkResult.Error(e.message ?: "Network error"))
            }
        }
    }

    suspend fun checkHealth(): Flow<NetworkResult<HealthResponse>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = apiService.getHealth()
            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!
                emit(NetworkResult.Success(
                    HealthResponse(
                        status = dto.status ?: "unknown",
                        timestamp = dto.timestamp ?: System.currentTimeMillis()
                    )
                ))
            } else {
                emit(NetworkResult.Error("Health check failed", response.code()))
            }
        } catch (e: Exception) {
            Timber.e(e, "Health check error")
            emit(NetworkResult.Error(e.message ?: "Connection failed"))
        }
    }

    suspend fun runScan(): Flow<NetworkResult<String>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = apiService.runScan()
            if (response.isSuccessful) {
                val message = response.body()?.message ?: "Scan triggered successfully"
                emit(NetworkResult.Success(message))
            } else {
                emit(NetworkResult.Error("Failed to trigger scan", response.code()))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error triggering scan")
            emit(NetworkResult.Error(e.message ?: "Failed to trigger scan"))
        }
    }

    suspend fun registerDevice(token: String): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = apiService.registerDevice(DeviceTokenDto(token))
            if (response.isSuccessful) {
                emit(NetworkResult.Success(Unit))
            } else {
                emit(NetworkResult.Error("Failed to register device", response.code()))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error registering device")
            emit(NetworkResult.Error(e.message ?: "Failed to register device"))
        }
    }

    suspend fun unregisterDevice(token: String): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = apiService.unregisterDevice(DeviceTokenDto(token))
            if (response.isSuccessful) {
                emit(NetworkResult.Success(Unit))
            } else {
                emit(NetworkResult.Error("Failed to unregister device", response.code()))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error unregistering device")
            emit(NetworkResult.Error(e.message ?: "Failed to unregister device"))
        }
    }
}
