package com.deadarchive.core.network

import retrofit2.http.GET
import retrofit2.http.Query

interface ArchiveApiService {
    @GET("advancedsearch.php")
    suspend fun searchConcerts(
        @Query("q") query: String,
        @Query("fl") fields: String = "identifier,title,date,creator",
        @Query("output") output: String = "json"
    ): ArchiveSearchResponse
}

data class ArchiveSearchResponse(
    val response: Response
) {
    data class Response(
        val docs: List<Doc>
    ) {
        data class Doc(
            val identifier: String,
            val title: String,
            val date: String?,
            val creator: String?
        )
    }
}