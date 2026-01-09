package com.kaos.evcryptoscanner.domain.usecase

import com.kaos.evcryptoscanner.data.api.NetworkResult
import com.kaos.evcryptoscanner.data.repository.ScanRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ManageDeviceRegistrationUseCase @Inject constructor(
    private val repository: ScanRepository
) {
    suspend fun register(token: String): Flow<NetworkResult<Unit>> {
        return repository.registerDevice(token)
    }

    suspend fun unregister(token: String): Flow<NetworkResult<Unit>> {
        return repository.unregisterDevice(token)
    }
}
