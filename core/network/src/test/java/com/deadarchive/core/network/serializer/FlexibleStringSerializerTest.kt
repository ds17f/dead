package com.deadarchive.core.network.serializer

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.Test

class FlexibleStringSerializerTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    data class TestModel(
        @Serializable(with = FlexibleStringSerializer::class)
        val creator: String? = null,
        @Serializable(with = FlexibleStringListSerializer::class)
        val description: String? = null
    )

    @Test
    fun `FlexibleStringSerializer handles string values`() {
        val jsonString = """{"creator":"Grateful Dead"}"""
        val result = json.decodeFromString<TestModel>(jsonString)
        
        assertThat(result.creator).isEqualTo("Grateful Dead")
    }

    @Test
    fun `FlexibleStringSerializer handles single-element array`() {
        val jsonString = """{"creator":["Grateful Dead"]}"""
        val result = json.decodeFromString<TestModel>(jsonString)
        
        assertThat(result.creator).isEqualTo("Grateful Dead")
    }

    @Test
    fun `FlexibleStringSerializer handles multi-element array takes first`() {
        val jsonString = """{"creator":["Grateful Dead", "Jerry Garcia"]}"""
        val result = json.decodeFromString<TestModel>(jsonString)
        
        assertThat(result.creator).isEqualTo("Grateful Dead")
    }

    @Test
    fun `FlexibleStringSerializer handles empty array`() {
        val jsonString = """{"creator":[]}"""
        val result = json.decodeFromString<TestModel>(jsonString)
        
        assertThat(result.creator).isNull()
    }

    @Test
    fun `FlexibleStringSerializer handles null values`() {
        val jsonString = """{"creator":null}"""
        val result = json.decodeFromString<TestModel>(jsonString)
        
        assertThat(result.creator).isNull()
    }

    @Test
    fun `FlexibleStringSerializer handles number as string`() {
        val jsonString = """{"creator":1995}"""
        val result = json.decodeFromString<TestModel>(jsonString)
        
        assertThat(result.creator).isEqualTo("1995")
    }

    @Test
    fun `FlexibleStringListSerializer handles string values`() {
        val jsonString = """{"description":"This is a description"}"""
        val result = json.decodeFromString<TestModel>(jsonString)
        
        assertThat(result.description).isEqualTo("This is a description")
    }

    @Test
    fun `FlexibleStringListSerializer handles array values joined with newlines`() {
        val jsonString = """{"description":["Line 1", "Line 2", "Line 3"]}"""
        val result = json.decodeFromString<TestModel>(jsonString)
        
        assertThat(result.description).isEqualTo("Line 1\nLine 2\nLine 3")
    }

    @Test
    fun `FlexibleStringListSerializer filters blank entries`() {
        val jsonString = """{"description":["Line 1", "", "Line 3", " "]}"""
        val result = json.decodeFromString<TestModel>(jsonString)
        
        assertThat(result.description).isEqualTo("Line 1\nLine 3")
    }

    @Test
    fun `FlexibleStringListSerializer handles empty array`() {
        val jsonString = """{"description":[]}"""
        val result = json.decodeFromString<TestModel>(jsonString)
        
        assertThat(result.description).isNull()
    }

    @Test
    fun `FlexibleStringListSerializer handles array of blank strings`() {
        val jsonString = """{"description":["", " ", ""]}"""
        val result = json.decodeFromString<TestModel>(jsonString)
        
        assertThat(result.description).isNull()
    }
}