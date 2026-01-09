package com.kaos.evcryptoscanner.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaos.evcryptoscanner.data.api.NetworkResult
import com.kaos.evcryptoscanner.domain.model.HealthResponse
import com.kaos.evcryptoscanner.domain.model.ScanResult
import com.kaos.evcryptoscanner.domain.usecase.CheckHealthUseCase
import com.kaos.evcryptoscanner.domain.usecase.GetLatestScanUseCase
import com.kaos.evcryptoscanner.domain.usecase.RunScanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getLatestScanUseCase: GetLatestScanUseCase,
    private val runScanUseCase: RunScanUseCase,
    private val checkHealthUseCase: CheckHealthUseCase
) : ViewModel() {

    private val _scanState = MutableStateFlow<NetworkResult<ScanResult>>(NetworkResult.Loading)
    val scanState: StateFlow<NetworkResult<ScanResult>> = _scanState.asStateFlow()

    private val _healthState = MutableStateFlow<NetworkResult<HealthResponse>?>(null)
    val healthState: StateFlow<NetworkResult<HealthResponse>?> = _healthState.asStateFlow()

    private val _runScanState = MutableStateFlow<NetworkResult<String>?>(null)
    val runScanState: StateFlow<NetworkResult<String>?> = _runScanState.asStateFlow()

    init {
        loadLatestScan()
    }

    fun loadLatestScan(useCache: Boolean = false) {
        viewModelScope.launch {
            getLatestScanUseCase(useCache).collect { result ->
                _scanState.value = result
            }
        }
    }

    fun checkHealth() {
        viewModelScope.launch {
            checkHealthUseCase().collect { result ->
                _healthState.value = result
            }
        }
    }

    fun runScan() {
        viewModelScope.launch {
            runScanUseCase().collect { result ->
                _runScanState.value = result
                if (result is NetworkResult.Success) {
                    loadLatestScan()
                }
            }
        }
    }

    fun clearRunScanState() {
        _runScanState.value = null
    }
}
