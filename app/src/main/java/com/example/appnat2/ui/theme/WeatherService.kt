package com.example.appnat2.ui.theme

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class WeatherResponse(
    val main: MainData,
    val weather: List<WeatherInfo>,
    val name: String
)

data class MainData(
    val temp: Double,
    val humidity: Int
)

data class WeatherInfo(
    val description: String,
    val icon: String
)

interface WeatherService {
    @GET("weather")
    suspend fun getCurrentWeather(
        @Query("q") city: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "es"
    ): WeatherResponse

    companion object {
        private const val BASE_URL = "https://api.openweathermap.org/data/2.5/"

        fun create(): WeatherService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(WeatherService::class.java)
        }
    }
}
