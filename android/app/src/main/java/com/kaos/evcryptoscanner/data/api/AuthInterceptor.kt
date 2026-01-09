package com.kaos.evcryptoscanner.data.api

import com.kaos.evcryptoscanner.data.local.PreferencesManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val preferencesManager: PreferencesManager
) : Interceptor {

    private val protectedEndpoints = setOf(
        "/scan/run",
        "/device/register",
        "/device/unregister"
    )

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath

        val requiresAuth = protectedEndpoints.any { path.contains(it) }

        return if (requiresAuth) {
            val apiKey = runBlocking { preferencesManager.apiKey.first() }
            if (apiKey.isNotBlank()) {
                val authenticatedRequest = request.newBuilder()
                    .addHeader("X-API-KEY", apiKey)
                    .build()
                chain.proceed(authenticatedRequest)
            } else {
                chain.proceed(request)
            }
        } else {
            chain.proceed(request)
        }
    }
}
