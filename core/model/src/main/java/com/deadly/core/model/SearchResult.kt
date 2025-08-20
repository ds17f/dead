package com.deadly.core.model

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
    val recordings: List<Recording>,
    
    @SerialName("numFound")
    val totalResults: Int,
    
    @SerialName("start")
    val startIndex: Int
) {
    val hasMoreResults: Boolean
        get() = startIndex + recordings.size < totalResults
    
    val currentPage: Int
        get() = (startIndex / recordings.size) + 1
    
    val totalPages: Int
        get() = kotlin.math.ceil(totalResults.toDouble() / recordings.size).toInt()
}