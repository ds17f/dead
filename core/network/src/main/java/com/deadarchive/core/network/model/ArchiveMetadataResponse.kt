package com.deadarchive.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ArchiveMetadataResponse(
    @SerialName("files")
    val files: List<ArchiveFile> = emptyList(),
    
    @SerialName("metadata")
    val metadata: ArchiveMetadata,
    
    @SerialName("reviews")
    val reviews: List<ArchiveReview>? = null,
    
    @SerialName("server")
    val server: String? = null,
    
    @SerialName("dir")
    val directory: String? = null,
    
    @SerialName("workable_servers")
    val workableServers: List<String>? = null
) {
    @Serializable
    data class ArchiveFile(
        @SerialName("name")
        val name: String,
        
        @SerialName("format")
        val format: String,
        
        @SerialName("size")
        val size: String? = null,
        
        @SerialName("length")
        val length: String? = null,
        
        @SerialName("title")
        val title: String? = null,
        
        @SerialName("track")
        val track: String? = null,
        
        @SerialName("artist")
        val artist: String? = null,
        
        @SerialName("album")
        val album: String? = null,
        
        @SerialName("bitrate")
        val bitrate: String? = null,
        
        @SerialName("sample_rate")
        val sampleRate: String? = null,
        
        @SerialName("md5")
        val md5: String? = null,
        
        @SerialName("crc32")
        val crc32: String? = null,
        
        @SerialName("sha1")
        val sha1: String? = null,
        
        @SerialName("mtime")
        val modifiedTime: String? = null
    )
    
    @Serializable
    data class ArchiveMetadata(
        @SerialName("identifier")
        val identifier: String,
        
        @SerialName("title")
        val title: String,
        
        @SerialName("date")
        val date: String? = null,
        
        @SerialName("venue")
        val venue: List<String>? = null,
        
        @SerialName("coverage")
        val coverage: String? = null,
        
        @SerialName("creator")
        val creator: List<String>? = null,
        
        @SerialName("description")
        val description: List<String>? = null,
        
        @SerialName("setlist")
        val setlist: List<String>? = null,
        
        @SerialName("source")
        val source: List<String>? = null,
        
        @SerialName("taper")
        val taper: List<String>? = null,
        
        @SerialName("transferer")
        val transferer: List<String>? = null,
        
        @SerialName("lineage")
        val lineage: List<String>? = null,
        
        @SerialName("notes")
        val notes: List<String>? = null,
        
        @SerialName("uploader")
        val uploader: String? = null,
        
        @SerialName("addeddate")
        val addedDate: String? = null,
        
        @SerialName("publicdate")
        val publicDate: String? = null,
        
        @SerialName("collection")
        val collection: List<String>? = null,
        
        @SerialName("subject")
        val subject: List<String>? = null,
        
        @SerialName("licenseurl")
        val licenseUrl: String? = null
    )
    
    @Serializable
    data class ArchiveReview(
        @SerialName("reviewtitle")
        val title: String? = null,
        
        @SerialName("reviewbody")
        val body: String? = null,
        
        @SerialName("reviewer")
        val reviewer: String? = null,
        
        @SerialName("reviewdate")
        val reviewDate: String? = null,
        
        @SerialName("stars")
        val stars: Int? = null
    )
}