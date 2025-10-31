package com.example.hackathon

// Optional: Simple stub for weather suggestion. Replace with real OpenWeatherMap calls if needed.
object WeatherHelper {
    fun getSimpleTip(tempCelsius: Double, condition: String): String {
        return when {
            condition.contains("rain", true) -> "Rain expected. Carry an umbrella and keep medicines dry."
            tempCelsius > 32 -> "It’s hot. Stay hydrated and avoid peak sun."
            tempCelsius < 10 -> "It’s cold. Dress warmly and monitor elderly family members."
            else -> "Weather looks fine. Take a short walk today!"
        }
    }
}


