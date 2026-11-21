package com.rcsolutions.rcosmobile.data

import com.rcsolutions.rcosmobile.model.WeatherSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.roundToInt

class WeatherService {
    suspend fun fetchCurrentWeather(cityQuery: String): WeatherSnapshot? = withContext(Dispatchers.IO) {
        val cleanQuery = cityQuery.trim()
        if (cleanQuery.isBlank()) return@withContext null
        try {
            val encoded = URLEncoder.encode(cleanQuery, "UTF-8")
            val geoUrl = URL("https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=1&language=en&format=json")
            val geoJson = fetchJson(geoUrl) ?: return@withContext null
            val results = geoJson.optJSONArray("results") ?: return@withContext null
            if (results.length() == 0) return@withContext null

            val first = results.getJSONObject(0)
            val lat = first.getDouble("latitude")
            val lon = first.getDouble("longitude")
            val cityName = first.optString("name", cleanQuery)
            val admin = first.optString("admin1", "")
            val locationLabel = if (admin.isNotBlank()) "$cityName, $admin" else cityName

            val weatherUrl = URL(
                "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code&temperature_unit=fahrenheit"
            )
            val weatherJson = fetchJson(weatherUrl) ?: return@withContext null
            val current = weatherJson.optJSONObject("current") ?: return@withContext null
            val temp = current.optDouble("temperature_2m", 72.0).roundToInt()
            val code = current.optInt("weather_code", 0)
            val (label, clear) = mapWeatherCode(code)

            WeatherSnapshot(
                temperatureFahrenheit = temp,
                conditionLabel = label,
                locationLabel = locationLabel,
                isClear = clear
            )
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchJson(url: URL): JSONObject? {
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 6000
            readTimeout = 6000
            requestMethod = "GET"
        }
        return try {
            if (conn.responseCode in 200..299) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                JSONObject(text)
            } else null
        } finally {
            conn.disconnect()
        }
    }

    private fun mapWeatherCode(code: Int): Pair<String, Boolean> = when (code) {
        0 -> "Clear Skies" to true
        1, 2 -> "Mostly Clear" to true
        3 -> "Overcast" to false
        45, 48 -> "Foggy" to false
        51, 53, 55, 61, 63, 65 -> "Rain" to false
        71, 73, 75 -> "Snow" to false
        80, 81, 82 -> "Showers" to false
        95, 96, 99 -> "Storms" to false
        else -> "Fair Skies" to true
    }
}
