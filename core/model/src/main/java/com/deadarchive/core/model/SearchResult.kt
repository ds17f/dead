package com.deadarchive.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchResult(
    @SerialName("response")
    val response: SearchResponse
)

@Serializable
data class SearchResponse(
    @SerialName("docs")
    val concerts: List<Concert>,
    
    @SerialName("numFound")
    val totalResults: Int,
    
    @SerialName("start")
    val startIndex: Int
) {
    val hasMoreResults: Boolean
        get() = startIndex + concerts.size < totalResults
    
    val currentPage: Int
        get() = (startIndex / concerts.size) + 1
    
    val totalPages: Int
        get() = kotlin.math.ceil(totalResults.toDouble() / concerts.size).toInt()
}