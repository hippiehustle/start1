package com.kaos.evcryptoscanner.domain.usecase

import com.kaos.evcryptoscanner.data.api.NetworkResult
import com.kaos.evcryptoscanner.data.repository.ScanRepository
import com.kaos.evcryptoscanner.domain.model.HealthResponse
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CheckHealthUseCase @Inject constructor(
    private val repository: ScanRepository
) {
    suspend operator fun invoke(): Flow<NetworkResult<HealthResponse>> {
        return repository.checkHealth()
    }
}
