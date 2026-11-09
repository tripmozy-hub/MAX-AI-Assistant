package com.max.assistant.automation

import android.util.Log
import com.max.assistant.tts.VoiceLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class WeatherInfo(
    val location: String,
    val temperatureC: Int,
    val condition: String,
    val humidityPercent: Int
)

interface IWeatherService {
    val isConfigured: Boolean
    suspend fun getWeather(location: String): WeatherInfo?
    fun formatWeatherReport(info: WeatherInfo?, language: VoiceLanguage): String
}

class LiveOpenMeteoWeatherService : IWeatherService {
    companion object {
        private const val TAG = "LiveWeatherService"
    }

    override val isConfigured: Boolean = true

    override suspend fun getWeather(location: String): WeatherInfo? = withContext(Dispatchers.IO) {
        val queryCity = if (location.isBlank()) "Siliguri" else location.trim()
        try {
            // 1. Geocode City Name
            val geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name=" + URLEncoder.encode(queryCity, "UTF-8") + "&count=1&language=en&format=json"
            val geoConn = (URL(geoUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
            }
            if (geoConn.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
            val geoJsonStr = geoConn.inputStream.bufferedReader().use { it.readText() }
            val geoJson = JSONObject(geoJsonStr)
            val results = geoJson.optJSONArray("results") ?: return@withContext null
            if (results.length() == 0) return@withContext null

            val topResult = results.getJSONObject(0)
            val cityName = topResult.optString("name", queryCity)
            val lat = topResult.optDouble("latitude")
            val lon = topResult.optDouble("longitude")

            // 2. Fetch Live Weather Forecast
            val weatherUrl = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,relative_humidity_2m,weather_code"
            val weatherConn = (URL(weatherUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
            }
            if (weatherConn.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
            val weatherJsonStr = weatherConn.inputStream.bufferedReader().use { it.readText() }
            val weatherJson = JSONObject(weatherJsonStr)
            val current = weatherJson.optJSONObject("current") ?: return@withContext null

            val temp = current.optDouble("temperature_2m", 25.0).toInt()
            val humidity = current.optInt("relative_humidity_2m", 60)
            val code = current.optInt("weather_code", 0)

            val condition = mapWeatherCode(code)

            WeatherInfo(
                location = cityName,
                temperatureC = temp,
                condition = condition,
                humidityPercent = humidity
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching live weather for $queryCity: ${e.message}")
            null
        }
    }

    private fun mapWeatherCode(code: Int): String {
        return when (code) {
            0 -> "Clear skies"
            1, 2, 3 -> "Partly cloudy"
            45, 48 -> "Foggy"
            51, 53, 55 -> "Light drizzle"
            61, 63, 65 -> "Rainy"
            71, 73, 75 -> "Snowy"
            80, 81, 82 -> "Rain showers"
            95, 96, 99 -> "Thunderstorm"
            else -> "Pleasant weather"
        }
    }

    override fun formatWeatherReport(info: WeatherInfo?, language: VoiceLanguage): String {
        if (info == null) {
            return when (language) {
                VoiceLanguage.HINGLISH -> "Sir, abhi live weather info nahi mil paya. Internet connection check karein."
                VoiceLanguage.HINDI -> "सर, मौसम की लाइव जानकारी प्राप्त नहीं हो सकी। कृपया इंटरनेट कनेक्शन जांचें।"
                VoiceLanguage.BENGALI -> "স্যার, লাইভ আবহাওয়ার তথ্য আনা যায়নি। অনুগ্রহ করে ইন্টারনেট সংযোগ পরীক্ষা করুন।"
                VoiceLanguage.ENGLISH, VoiceLanguage.AUTO -> "Sir, unable to retrieve live weather data at this moment. Please check internet connectivity."
            }
        }
        val condHindi = when (info.condition) {
            "Clear skies" -> "आसमान साफ"
            "Partly cloudy" -> "हल्के बादल"
            "Foggy" -> "कोहरा"
            "Rainy", "Light drizzle", "Rain showers" -> "बारिश"
            "Thunderstorm" -> "आंधी-तूफान"
            else -> "सुहावना"
        }
        val condBengali = when (info.condition) {
            "Clear skies" -> "পরিষ্কার আকাশ"
            "Partly cloudy" -> "আংশিক মেঘলা"
            "Foggy" -> "কুয়াশা"
            "Rainy", "Light drizzle", "Rain showers" -> "বৃষ্টি"
            "Thunderstorm" -> "বজ্রপাত"
            else -> "মনোরম"
        }

        return when (language) {
            VoiceLanguage.HINGLISH -> "Sir, ${info.location} mein aaj weather ${info.condition.lowercase()} hai, temperature ${info.temperatureC} degree Celsius hai aur humidity ${info.humidityPercent}% hai."
            VoiceLanguage.HINDI -> "सर, ${info.location} में मौसम $condHindi है और तापमान ${info.temperatureC} डिग्री सेल्सियस है। आर्द्रता ${info.humidityPercent}% है।"
            VoiceLanguage.BENGALI -> "স্যার, ${info.location}-এ আবহাওয়া $condBengali এবং তাপমাত্রা ${info.temperatureC} ডিগ্রি সেলসিয়াস। আর্দ্রতা ${info.humidityPercent}%।"
            VoiceLanguage.ENGLISH, VoiceLanguage.AUTO -> "Sir, in ${info.location}, it is currently ${info.condition.lowercase()} at ${info.temperatureC} degrees Celsius, with ${info.humidityPercent}% humidity."
        }
    }
}
