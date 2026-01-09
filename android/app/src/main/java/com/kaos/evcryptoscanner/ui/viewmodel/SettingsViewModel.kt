package com.kaos.evcryptoscanner.ui.viewmodel

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import com.kaos.evcryptoscanner.data.api.NetworkResult
import com.kaos.evcryptoscanner.data.local.PreferencesManager
import com.kaos.evcryptoscanner.data.worker.WorkManagerHelper
import com.kaos.evcryptoscanner.domain.usecase.CheckHealthUseCase
import com.kaos.evcryptoscanner.domain.usecase.GetLatestScanUseCase
import com.kaos.evcryptoscanner.domain.usecase.ManageDeviceRegistrationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val checkHealthUseCase: CheckHealthUseCase,
    private val getLatestScanUseCase: GetLatestScanUseCase,
    private val manageDeviceRegistrationUseCase: ManageDeviceRegistrationUseCase,
    private val firebaseMessaging: FirebaseMessaging
) : ViewModel() {

    val baseUrl = preferencesManager.baseUrl
    val apiKey = preferencesManager.apiKey
    val autoRefreshInterval = preferencesManager.autoRefreshInterval
    val notificationsEnabled = preferencesManager.notificationsEnabled
    val smartNotifications = preferencesManager.smartNotifications

    private val _connectionTestState = MutableStateFlow<NetworkResult<String>?>(null)
    val connectionTestState: StateFlow<NetworkResult<String>?> = _connectionTestState.asStateFlow()

    fun updateBaseUrl(url: String) {
        viewModelScope.launch {
            preferencesManager.setBaseUrl(url)
        }
    }

    fun updateApiKey(key: String) {
        viewModelScope.launch {
            preferencesManager.setApiKey(key)
        }
    }

    fun updateAutoRefreshInterval(context: android.content.Context, minutes: Int) {
        viewModelScope.launch {
            preferencesManager.setAutoRefreshInterval(minutes)
            WorkManagerHelper.schedulePeriodicScan(context, minutes)
        }
    }

    fun updateNotificationsEnabled(context: android.content.Context, enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setNotificationsEnabled(enabled)

            if (enabled) {
                val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }

                if (hasPermission) {
                    registerFCMToken()
                }
            } else {
                unregisterFCMToken()
            }
        }
    }

    fun updateSmartNotifications(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setSmartNotifications(enabled)
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            _connectionTestState.value = NetworkResult.Loading

            try {
                var healthOk = false
                var scanOk = false

                checkHealthUseCase().collect { result ->
                    healthOk = result is NetworkResult.Success
                }

                getLatestScanUseCase().collect { result ->
                    scanOk = result is NetworkResult.Success
                }

                _connectionTestState.value = if (healthOk && scanOk) {
                    NetworkResult.Success("Connection successful!")
                } else {
                    NetworkResult.Error("Connection failed. Check URL and API key.")
                }
            } catch (e: Exception) {
                _connectionTestState.value = NetworkResult.Error(e.message ?: "Connection failed")
            }
        }
    }

    fun clearConnectionTestState() {
        _connectionTestState.value = null
    }

    private fun registerFCMToken() {
        viewModelScope.launch {
            try {
                val token = firebaseMessaging.token.await()
                Timber.d("FCM Token: $token")
                preferencesManager.setFcmToken(token)
                manageDeviceRegistrationUseCase.register(token).collect { result ->
                    when (result) {
                        is NetworkResult.Success -> Timber.d("Device registered successfully")
                        is NetworkResult.Error -> Timber.e("Failed to register device: ${result.message}")
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to get FCM token")
            }
        }
    }

    private fun unregisterFCMToken() {
        viewModelScope.launch {
            try {
                val token = preferencesManager.fcmToken.first()
                if (token.isNotBlank()) {
                    manageDeviceRegistrationUseCase.unregister(token).collect { result ->
                        when (result) {
                            is NetworkResult.Success -> {
                                Timber.d("Device unregistered successfully")
                                preferencesManager.setFcmToken("")
                            }
                            is NetworkResult.Error -> Timber.e("Failed to unregister device: ${result.message}")
                            else -> {}
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to unregister device")
            }
        }
    }
}
