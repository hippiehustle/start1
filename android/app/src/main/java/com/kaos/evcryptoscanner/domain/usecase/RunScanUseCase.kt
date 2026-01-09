package com.kaos.evcryptoscanner.domain.usecase

import com.kaos.evcryptoscanner.data.api.NetworkResult
import com.kaos.evcryptoscanner.data.repository.ScanRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RunScanUseCase @Inject constructor(
    private val repository: ScanRepository
) {
    suspend operator fun invoke(): Flow<NetworkResult<String>> {
        return repository.runScan()
    }
}
