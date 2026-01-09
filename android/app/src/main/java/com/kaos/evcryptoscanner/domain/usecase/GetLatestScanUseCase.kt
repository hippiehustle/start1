package com.kaos.evcryptoscanner.domain.usecase

import com.kaos.evcryptoscanner.data.api.NetworkResult
import com.kaos.evcryptoscanner.data.repository.ScanRepository
import com.kaos.evcryptoscanner.domain.model.ScanResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetLatestScanUseCase @Inject constructor(
    private val repository: ScanRepository
) {
    suspend operator fun invoke(useCache: Boolean = false): Flow<NetworkResult<ScanResult>> {
        return repository.getLatestScan(useCache)
    }
}
