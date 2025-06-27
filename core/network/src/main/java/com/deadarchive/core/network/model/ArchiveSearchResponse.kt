package com.deadarchive.core.network.model

import com.deadarchive.core.network.serializer.FlexibleStringSerializer
import com.deadarchive.core.network.serializer.FlexibleStringOrIntSerializer
import com.deadarchive.core.network.serializer.FlexibleStringListSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ArchiveSearchResponse(
    @SerialName("response")
    val response: ArchiveResponse,
    
    @SerialName("responseHeader")
    val responseHeader: ResponseHeader? = null
) {
    @Serializable
    data class ArchiveResponse(
        @SerialName("docs")
        val docs: List<ArchiveDoc>,
        
        @SerialName("numFound")
        val numFound: Int,
        
        @SerialName("start")
        val start: Int
    )
    
    @Serializable
    data class ArchiveDoc(
        @SerialName("identifier")
        val identifier: String,
        
        @SerialName("title")
        val title: String,
        
        @SerialName("date")
        val date: String? = null,
        
        @SerialName("venue")
        @Serializable(with = FlexibleStringSerializer::class)
        val venue: String? = null,
        
        @SerialName("coverage")
        val coverage: String? = null,
        
        @SerialName("creator")
        @Serializable(with = FlexibleStringSerializer::class)
        val creator: String? = null,
        
        @SerialName("year")
        @Serializable(with = FlexibleStringOrIntSerializer::class)
        val year: String? = null,
        
        @SerialName("source")
        @Serializable(with = FlexibleStringSerializer::class)
        val source: String? = null,
        
        @SerialName("taper")
        @Serializable(with = FlexibleStringSerializer::class)
        val taper: String? = null,
        
        @SerialName("transferer")
        @Serializable(with = FlexibleStringSerializer::class)
        val transferer: String? = null,
        
        @SerialName("lineage")
        @Serializable(with = FlexibleStringListSerializer::class)
        val lineage: String? = null,
        
        @SerialName("description")
        @Serializable(with = FlexibleStringListSerializer::class)
        val description: String? = null,
        
        @SerialName("setlist")
        @Serializable(with = FlexibleStringListSerializer::class)
        val setlist: String? = null,
        
        @SerialName("uploader")
        val uploader: String? = null,
        
        @SerialName("addeddate")
        val addedDate: String? = null,
        
        @SerialName("publicdate")
        val publicDate: String? = null,
        
        @SerialName("collection")
        val collection: List<String>? = null,
        
        @SerialName("mediatype")
        val mediaType: String? = null,
        
        @SerialName("avg_rating")
        val avgRating: Double? = null,
        
        @SerialName("num_reviews")
        val numReviews: Int? = null,
        
        @SerialName("downloads")
        val downloads: Int? = null,
        
        @SerialName("item_size")
        val itemSize: Long? = null
    )
    
    @Serializable
    data class ResponseHeader(
        @SerialName("status")
        val status: Int,
        
        @SerialName("QTime")
        val queryTime: Int,
        
        @SerialName("params")
        val params: Map<String, String>? = null
    )
}