package com.deadarchive.core.data.repository

import com.deadarchive.core.model.Concert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface ConcertRepository {
    fun searchConcerts(query: String): Flow<List<Concert>>
    suspend fun getConcertById(id: String): Concert?
    suspend fun getFavoriteConcerts(): Flow<List<Concert>>
}

class ConcertRepositoryImpl : ConcertRepository {
    override fun searchConcerts(query: String): Flow<List<Concert>> = flowOf(emptyList())
    override suspend fun getConcertById(id: String): Concert? = null
    override suspend fun getFavoriteConcerts(): Flow<List<Concert>> = flowOf(emptyList())
}