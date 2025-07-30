package com.deadarchive.core.network

import com.deadarchive.core.network.model.GitHubRelease
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * GitHub API service interface for accessing release information.
 * 
 * Base URL: https://api.github.com/
 * Repository: ds17f/dead
 * 
 * Uses GitHub's public REST API which doesn't require authentication
 * for accessing public repository release information.
 */
interface GitHubApiService {
    
    /**
     * Get the latest release for the Dead Archive repository.
     * 
     * @param owner Repository owner (ds17f)
     * @param repo Repository name (dead)
     * @return Latest release information including assets and metadata
     */
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String = "ds17f",
        @Path("repo") repo: String = "dead"
    ): Response<GitHubRelease>
    
    /**
     * Get all releases for the repository (for future use).
     * 
     * @param owner Repository owner (ds17f)
     * @param repo Repository name (dead)
     * @return List of all releases
     */
    @GET("repos/{owner}/{repo}/releases")
    suspend fun getAllReleases(
        @Path("owner") owner: String = "ds17f",
        @Path("repo") repo: String = "dead"
    ): Response<List<GitHubRelease>>
    
    /**
     * Get a specific release by tag name (for future use).
     * 
     * @param owner Repository owner (ds17f)
     * @param repo Repository name (dead)
     * @param tag Release tag (e.g., "v1.0.0")
     * @return Specific release information
     */
    @GET("repos/{owner}/{repo}/releases/tags/{tag}")
    suspend fun getReleaseByTag(
        @Path("owner") owner: String = "ds17f",
        @Path("repo") repo: String = "dead",
        @Path("tag") tag: String
    ): Response<GitHubRelease>
}