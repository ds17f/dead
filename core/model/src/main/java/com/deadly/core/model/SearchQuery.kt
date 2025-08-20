package com.deadly.core.model

import kotlinx.serialization.Serializable

@Serializable
data class SearchQuery(
    val query: String = "",
    val year: String? = null,
    val venue: String? = null,
    val location: String? = null,
    val source: String? = null,
    val sortBy: SortOption = SortOption.DATE_DESC,
    val pageSize: Int = 20,
    val page: Int = 0
) {
    fun toArchiveQuery(): String {
        val conditions = mutableListOf<String>()
        
        // Always search within Grateful Dead collection
        conditions.add("collection:GratefulDead")
        
        // Add search terms if provided
        if (query.isNotBlank()) {
            conditions.add("(title:$query OR description:$query)")
        }
        
        // Add filters
        year?.takeIf { it.isNotBlank() }?.let { conditions.add("date:$it*") }
        venue?.takeIf { it.isNotBlank() }?.let { conditions.add("venue:$it") }
        location?.takeIf { it.isNotBlank() }?.let { conditions.add("coverage:$it") }
        source?.takeIf { it.isNotBlank() }?.let { conditions.add("source:$it") }
        
        return conditions.joinToString(" AND ")
    }
    
    val sortParameter: String
        get() = when (sortBy) {
            SortOption.DATE_ASC -> "date asc"
            SortOption.DATE_DESC -> "date desc"
            SortOption.TITLE_ASC -> "title asc"
            SortOption.TITLE_DESC -> "title desc"
            SortOption.ADDEDDATE_ASC -> "addeddate asc"
            SortOption.ADDEDDATE_DESC -> "addeddate desc"
            SortOption.RELEVANCE -> "" // Default Archive.org relevance
        }
    
    val isEmpty: Boolean
        get() = query.isBlank() && year.isNullOrBlank() && venue.isNullOrBlank() && 
                location.isNullOrBlank() && source.isNullOrBlank()
}

enum class SortOption(val displayName: String) {
    RELEVANCE("Relevance"),
    DATE_DESC("Date (Newest)"),
    DATE_ASC("Date (Oldest)"),
    TITLE_ASC("Title (A-Z)"),
    TITLE_DESC("Title (Z-A)"),
    ADDEDDATE_DESC("Recently Added"),
    ADDEDDATE_ASC("Oldest Added")
}