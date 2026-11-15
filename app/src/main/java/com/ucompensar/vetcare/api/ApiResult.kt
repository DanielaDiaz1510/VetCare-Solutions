package com.ucompensar.vetcare.api

import com.google.gson.Gson
import com.ucompensar.vetcare.models.ApiError
import retrofit2.Response
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/** Resultado de llamar a la API: o salió bien (Exito) o falló (Falla). */
sealed class ApiResult<out T> {

    data class Exito<T>(val datos: T) : ApiResult<T>()

    /**
     * codigoHttp: 401, 403, 404, 409, 429, 500...  (0 = no hubo conexión, -1 = respuesta ilegible)
     * mensaje: texto listo para mostrar al usuario.
     */
    data class Falla(
        val codigoHttp: Int,
        val codigo: String?,
        val mensaje: String
    ) : ApiResult<Nothing>() {
        val sesionInvalida: Boolean get() = codigoHttp == 401
        val sinPermiso: Boolean get() = codigoHttp == 403
        val sinConexion: Boolean get() = codigoHttp == 0
    }
}

/** Ejecuta una llamada y convierte la respuesta (o el fallo de red) en un ApiResult. */
suspend fun <T> llamar(bloque: suspend () -> Response<T>): ApiResult<T> {
    return try {
        val resp = bloque()
        if (resp.isSuccessful) {
            val cuerpo = resp.body()
            if (cuerpo != null) ApiResult.Exito(cuerpo)
            else ApiResult.Falla(-1, "SIN_CUERPO", "El servidor respondió sin datos")
        } else {
            convertirError(resp.code(), resp.errorBody()?.string())
        }
    } catch (e: CancellationException) {
        throw e                                   // la pantalla se cerró: no es un error
    } catch (e: IOException) {
        sinRed()
    } catch (e: IllegalArgumentException) {
        ApiResult.Falla(0, "URL_INVALIDA", "La dirección del servidor no es válida")
    } catch (e: Exception) {
        ApiResult.Falla(-1, "RESPUESTA_INVALIDA", "Respuesta inesperada del servidor")
    }
}

/** Para llamadas que responden 204 sin cuerpo (por ejemplo DELETE). */
suspend fun llamarSinCuerpo(bloque: suspend () -> Response<Unit>): ApiResult<Unit> {
    return try {
        val resp = bloque()
        if (resp.isSuccessful) ApiResult.Exito(Unit)
        else convertirError(resp.code(), resp.errorBody()?.string())
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        sinRed()
    } catch (e: IllegalArgumentException) {
        ApiResult.Falla(0, "URL_INVALIDA", "La dirección del servidor no es válida")
    } catch (e: Exception) {
        ApiResult.Falla(-1, "RESPUESTA_INVALIDA", "Respuesta inesperada del servidor")
    }
}

private fun sinRed() = ApiResult.Falla(
    0, "SIN_CONEXION",
    "No se pudo conectar con el servidor. Revise la red y la dirección configurada."
)

/** Lee el JSON de error de la API {status, codigo, error, ruta} y arma el mensaje. */
private fun convertirError(codigoHttp: Int, cuerpo: String?): ApiResult.Falla {
    val api: ApiError? = try {
        if (cuerpo.isNullOrBlank()) null else Gson().fromJson(cuerpo, ApiError::class.java)
    } catch (e: Exception) {
        null
    }
    val mensaje = api?.error?.takeIf { it.isNotBlank() } ?: mensajePorDefecto(codigoHttp)
    return ApiResult.Falla(codigoHttp, api?.codigo, mensaje)
}

private fun mensajePorDefecto(codigoHttp: Int) = when (codigoHttp) {
    400 -> "Los datos enviados no son válidos"
    401 -> "Su sesión no es válida o expiró. Inicie sesión de nuevo"
    403 -> "No tiene permisos para realizar esta acción"
    404 -> "No se encontró el registro"
    409 -> "La operación no se pudo completar por una regla del sistema"
    429 -> "Demasiados intentos. Espere unos minutos"
    in 500..599 -> "Error del servidor. Intente más tarde"
    else -> "Error inesperado ($codigoHttp)"
}