package com.ucompensar.vetcare.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.ucompensar.vetcare.models.Perfil

/**
 * Guarda la sesión y las preferencias de la app.
 *
 *  - Sesión (token, rol, nombre): archivo CIFRADO con AES-256 (EncryptedSharedPreferences).
 *  - Preferencias de interfaz (tema, último usuario, URL del servidor): archivo normal.
 */
class SessionManager(context: Context) {

    private val appContext = context.applicationContext
    private val segura: SharedPreferences = crearPrefsSeguras()
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(ARCHIVO_PREFS, Context.MODE_PRIVATE)

    // ---------------------------------------------------------------- sesión (cifrada)

    fun guardarToken(token: String) {
        segura.edit().putString(K_TOKEN, token).apply()
    }

    fun obtenerToken(): String? = segura.getString(K_TOKEN, null)

    fun haySesion(): Boolean = !obtenerToken().isNullOrEmpty()

    fun guardarPerfil(perfil: Perfil) {
        segura.edit()
            .putString(K_USUARIO, perfil.username)
            .putString(K_NOMBRE, perfil.nombre)
            .putString(K_ROLES, perfil.roles.joinToString(","))
            .putLong(K_ID_VET, perfil.idVeterinario ?: -1L)
            .apply()
    }

    fun nombre(): String = segura.getString(K_NOMBRE, "") ?: ""

    fun roles(): List<String> =
        (segura.getString(K_ROLES, "") ?: "").split(",").filter { it.isNotBlank() }

    fun esAdmin(): Boolean = roles().contains("ADMIN")

    /** Id del perfil de veterinario ligado al usuario, o null si no tiene. */
    fun idVeterinario(): Long? = segura.getLong(K_ID_VET, -1L).takeIf { it > 0 }

    /** Cierra la sesión: borra token y datos del usuario del teléfono. */
    fun cerrarSesion() {
        segura.edit().clear().apply()
    }

    // ------------------------------------------------- preferencias de interfaz (RF-11)

    var baseUrl: String
        get() = prefs.getString(K_URL, URL_POR_DEFECTO) ?: URL_POR_DEFECTO
        set(valor) { prefs.edit().putString(K_URL, normalizarUrl(valor)).apply() }

    var ultimoUsuario: String
        get() = prefs.getString(K_ULTIMO_USUARIO, "") ?: ""
        set(valor) { prefs.edit().putString(K_ULTIMO_USUARIO, valor).apply() }

    var modoOscuro: Boolean
        get() = prefs.getBoolean(K_MODO_OSCURO, false)
        set(valor) { prefs.edit().putBoolean(K_MODO_OSCURO, valor).apply() }

    /** Aplica el tema guardado (claro u oscuro). Se llama al abrir la app. */
    fun aplicarTema() {
        AppCompatDelegate.setDefaultNightMode(
            if (modoOscuro) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    // ------------------------------------------------------------------- internos

    private fun crearPrefsSeguras(): SharedPreferences {
        fun crear(): SharedPreferences {
            val clave = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            return EncryptedSharedPreferences.create(
                appContext,
                ARCHIVO_SEGURO,
                clave,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
        return try {
            crear()
        } catch (e: Exception) {
            // Si la clave del teléfono ya no coincide con el archivo, se empieza de cero
            appContext.deleteSharedPreferences(ARCHIVO_SEGURO)
            crear()
        }
    }

    /** Retrofit exige que la URL base termine en "/". */
    private fun normalizarUrl(texto: String): String {
        var url = texto.trim()
        if (url.isEmpty()) return URL_POR_DEFECTO
        if (!url.startsWith("http://") && !url.startsWith("https://")) url = "http://$url"
        if (!url.endsWith("/")) url += "/"
        return url
    }

    companion object {
        // Por USB (adb reverse). Para Wi-Fi se cambia desde la pantalla de login.
        const val URL_POR_DEFECTO = "http://localhost:8080/veterinaria/api/"

        const val ARCHIVO_SEGURO = "vetcare_sesion_segura"
        private const val ARCHIVO_PREFS = "vetcare_preferencias"

        private const val K_TOKEN = "token"
        private const val K_USUARIO = "usuario"
        private const val K_NOMBRE = "nombre"
        private const val K_ROLES = "roles"
        private const val K_ID_VET = "id_veterinario"
        private const val K_URL = "base_url"
        private const val K_ULTIMO_USUARIO = "ultimo_usuario"
        private const val K_MODO_OSCURO = "modo_oscuro"
    }
}