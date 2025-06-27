package com.deadarchive.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.HttpURLConnection

/**
 * Interceptor for handling API error responses and network issues
 */
class ErrorHandlingInterceptor : Interceptor {
    
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        
        return try {
            val response = chain.proceed(request)
            
            // Handle HTTP error codes
            when (response.code) {
                HttpURLConnection.HTTP_OK -> response
                
                HttpURLConnection.HTTP_BAD_REQUEST -> {
                    throw ArchiveApiException(
                        code = response.code,
                        message = "Bad request - check search parameters",
                        url = request.url.toString()
                    )
                }
                
                HttpURLConnection.HTTP_FORBIDDEN -> {
                    throw ArchiveApiException(
                        code = response.code,
                        message = "Access forbidden - rate limit exceeded or blocked",
                        url = request.url.toString()
                    )
                }
                
                HttpURLConnection.HTTP_NOT_FOUND -> {
                    throw ArchiveApiException(
                        code = response.code,
                        message = "Resource not found - invalid identifier or endpoint",
                        url = request.url.toString()
                    )
                }
                
                HttpURLConnection.HTTP_INTERNAL_ERROR -> {
                    throw ArchiveApiException(
                        code = response.code,
                        message = "Archive.org server error - try again later",
                        url = request.url.toString()
                    )
                }
                
                HttpURLConnection.HTTP_BAD_GATEWAY,
                HttpURLConnection.HTTP_UNAVAILABLE,
                HttpURLConnection.HTTP_GATEWAY_TIMEOUT -> {
                    throw ArchiveApiException(
                        code = response.code,
                        message = "Archive.org service temporarily unavailable",
                        url = request.url.toString()
                    )
                }
                
                else -> {
                    if (response.code >= 400) {
                        throw ArchiveApiException(
                            code = response.code,
                            message = "API request failed with code ${response.code}",
                            url = request.url.toString()
                        )
                    }
                    response
                }
            }
        } catch (e: IOException) {
            // Network connectivity issues
            throw NetworkException(
                message = "Network error: ${e.message}",
                cause = e,
                url = request.url.toString()
            )
        }
    }
}

/**
 * Exception for Archive.org API specific errors
 */
class ArchiveApiException(
    val code: Int,
    message: String,
    val url: String,
    cause: Throwable? = null
) : IOException("Archive API Error [$code]: $message (URL: $url)", cause)

/**
 * Exception for network connectivity issues
 */
class NetworkException(
    message: String,
    cause: Throwable? = null,
    val url: String
) : IOException("Network Error: $message (URL: $url)", cause)