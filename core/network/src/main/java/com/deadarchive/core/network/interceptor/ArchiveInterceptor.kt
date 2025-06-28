package com.deadarchive.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Interceptor for Archive.org API requests
 * Handles rate limiting, user agent, and request headers
 */
class ArchiveInterceptor : Interceptor {
    
    companion object {
        private const val USER_AGENT = "DeadArchive/1.0 (Android; Grateful Dead Concert Archive App)"
        private const val ACCEPT_HEADER = "application/json"
        
        // Simple rate limiting - minimum time between requests
        private const val MIN_REQUEST_INTERVAL_MS = 100L
        private var lastRequestTime = 0L
    }
    
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        // Rate limiting - ensure minimum interval between requests
        val currentTime = System.currentTimeMillis()
        val timeSinceLastRequest = currentTime - lastRequestTime
        
        if (timeSinceLastRequest < MIN_REQUEST_INTERVAL_MS) {
            val sleepTime = MIN_REQUEST_INTERVAL_MS - timeSinceLastRequest
            try {
                Thread.sleep(sleepTime)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw IOException("Request interrupted during rate limiting", e)
            }
        }
        
        // Build request with appropriate headers
        // Note: Don't manually set Accept-Encoding - let OkHttp handle gzip automatically
        val originalRequest = chain.request()
        val modifiedRequest = originalRequest.newBuilder()
            .addHeader("User-Agent", USER_AGENT)
            .addHeader("Accept", ACCEPT_HEADER)
            .build()
        
        lastRequestTime = System.currentTimeMillis()
        
        return chain.proceed(modifiedRequest)
    }
}