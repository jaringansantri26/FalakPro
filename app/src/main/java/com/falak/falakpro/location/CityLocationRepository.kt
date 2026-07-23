package com.falak.falakpro.location

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.util.Locale

data class CityLocation(
    val name: String,
    val group: String,
    val latitude: Double,
    val longitude: Double,
    val elevation: Double,
    val timezone: Double,
    val pressure: Double,
    val temperature: Double,
    val humidity: Double,
    val lapseRate: Double
) {
    val displayName: String
        get() = "$name, $group"
}

object CityLocationRepository {
    private const val ASSET_NAME = "cities_falakpro.bin"
    private val magic = byteArrayOf('F'.code.toByte(), 'P'.code.toByte(), 'K'.code.toByte(), 'O'.code.toByte(), 'T'.code.toByte(), 'A'.code.toByte(), '1'.code.toByte(), 0)

    @Volatile
    private var cachedCities: List<CityLocation>? = null

    suspend fun load(context: Context): List<CityLocation> {
        cachedCities?.let { return it }
        return withContext(Dispatchers.IO) {
            cachedCities ?: readCities(context.applicationContext).also { cachedCities = it }
        }
    }

    suspend fun search(context: Context, query: String, limit: Int = 80): List<CityLocation> {
        val cities = load(context)
        val normalized = query.trim().lowercase(Locale.ROOT)
        if (normalized.isBlank()) return cities.take(limit)
        return cities.asSequence()
            .filter {
                it.name.lowercase(Locale.ROOT).contains(normalized) ||
                    it.group.lowercase(Locale.ROOT).contains(normalized)
            }
            .take(limit)
            .toList()
    }

    private fun readCities(context: Context): List<CityLocation> {
        DataInputStream(BufferedInputStream(context.assets.open(ASSET_NAME))).use { input ->
            val actualMagic = ByteArray(magic.size)
            input.readFully(actualMagic)
            require(actualMagic.contentEquals(magic)) { "Invalid city database asset" }

            val count = input.readInt()
            return List(count) {
                CityLocation(
                    name = input.readLengthPrefixedString(),
                    group = input.readLengthPrefixedString(),
                    latitude = input.readDouble(),
                    longitude = input.readDouble(),
                    elevation = input.readDouble(),
                    timezone = input.readDouble(),
                    pressure = input.readDouble(),
                    temperature = input.readDouble(),
                    humidity = input.readDouble(),
                    lapseRate = input.readDouble()
                )
            }
        }
    }

    private fun DataInputStream.readLengthPrefixedString(): String {
        val length = readUnsignedShort()
        val bytes = ByteArray(length)
        readFully(bytes)
        return bytes.toString(Charsets.UTF_8)
    }
}
