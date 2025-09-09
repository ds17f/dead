package com.deadarchive.kmm.shared

class Greeting {
    private val platform = getPlatform()

    fun greet(): String {
        return "Hello KMM from ${platform.name}!"
    }
}