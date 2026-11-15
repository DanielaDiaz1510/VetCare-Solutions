package com.ucompensar.vetcare.api

import com.ucompensar.vetcare.preferences.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Se ejecuta ANTES de enviar cada petición. Si hay token guardado, le agrega
 *      Authorization: Bearer <token>
 * que es lo que el filtro del servidor (ApiSecurityFilter) espera.
 * Login y registro son públicos, así que no llevan token.
 */
class AuthInterceptor(private val session: SessionManager) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val ruta = original.url.encodedPath
        val esPublica = ruta.endsWith("/login") || ruta.endsWith("/registro")
        val token = session.obtenerToken()

        val peticion = if (token.isNullOrEmpty() || esPublica) {
            original
        } else {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
        return chain.proceed(peticion)
    }
}