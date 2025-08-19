package com.deadarchive.v2.core.database.entities

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions

/**
 * FTS5 entity for full-text search on show data.
 * 
 * Architecture: Separate FTS table that references ShowEntity.id
 * - showId: Foreign key to ShowEntity
 * - searchText: Rich searchable content (venue, location, date, year)
 * 
 * Example searchText: "1977-05-08 Barton Hall Cornell University Ithaca NY"
 * 
 * FTS5 provides:
 * - Better performance than FTS4
 * - Advanced BM25 ranking algorithm 
 * - Better query syntax with phrase matching
 * - Improved memory usage
 */
@Entity(tableName = "show_search")
@Fts4(tokenizer = FtsOptions.TOKENIZER_SIMPLE)
data class ShowSearchEntity(
    val showId: String,        // References ShowEntity.id
    val searchText: String     // Rich searchable content
)