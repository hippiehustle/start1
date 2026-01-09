package com.kaos.evcryptoscanner.data.api

import com.kaos.evcryptoscanner.data.api.dto.DeviceTokenDto
import com.kaos.evcryptoscanner.data.api.dto.HealthResponseDto
import com.kaos.evcryptoscanner.data.api.dto.ScanResponseDto
import com.kaos.evcryptoscanner.data.api.dto.ScanTriggerResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface KaosApiService {

    @GET("health")
    suspend fun getHealth(): Response<HealthResponseDto>

    @GET("scan/latest")
    suspend fun getLatestScan(): Response<ScanResponseDto>

    @POST("scan/run")
    suspend fun runScan(): Response<ScanTriggerResponseDto>

    @POST("device/register")
    suspend fun registerDevice(@Body token: DeviceTokenDto): Response<Unit>

    @POST("device/unregister")
    suspend fun unregisterDevice(@Body token: DeviceTokenDto): Response<Unit>
}
