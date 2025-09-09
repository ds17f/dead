package com.deadarchive.kmm.shared

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform