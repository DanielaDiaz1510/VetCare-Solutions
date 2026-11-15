package com.ucompensar.vetcare.api

import com.ucompensar.vetcare.preferences.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Construye Retrofit una sola vez y lo reutiliza.
 * Si el usuario cambia la dirección del servidor, se reconstruye con la nueva.
 */
object ApiClient {

    private var servicioActual: ApiService? = null
    private var urlActual: String? = null

    @Synchronized
    fun servicio(session: SessionManager): ApiService {
        val url = session.baseUrl
        val existente = servicioActual
        if (existente != null && url == urlActual) return existente

        val http = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(session))
            // BASIC muestra en el Logcat método, URL y código; no imprime contraseñas ni el token
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .build()

        val nuevo = Retrofit.Builder()
            .baseUrl(url)
            .client(http)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)

        servicioActual = nuevo
        urlActual = url
        return nuevo
    }
}