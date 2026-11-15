package com.ucompensar.vetcare.repository

import android.content.Context
import com.ucompensar.vetcare.api.ApiClient
import com.ucompensar.vetcare.api.ApiResult
import com.ucompensar.vetcare.api.llamar
import com.ucompensar.vetcare.api.llamarSinCuerpo
import com.ucompensar.vetcare.models.*
import com.ucompensar.vetcare.preferences.SessionManager
import com.ucompensar.vetcare.api.ApiService

/**
 * Único punto por donde las pantallas hablan con la API.
 * Cada función devuelve un ApiResult: Exito(datos) o Falla(código, mensaje).
 */
class VetRepository(context: Context) {

    val session = SessionManager(context)

    // Siempre se pide el servicio a ApiClient: si cambió la URL, se reconstruye solo
    private val api: ApiService get() = ApiClient.servicio(session)

    // ---------------------------------------------------------------- sesión

    /**
     * Flujo de login (RF-02):
     *  1. POST /login con usuario y clave -> llega el token y se guarda cifrado.
     *  2. GET /me (el interceptor ya adjunta el token) -> se guarda el perfil y los roles.
     * Si el paso 2 falla se borra el token, para no dejar una sesión a medias.
     */
    suspend fun iniciarSesion(usuario: String, clave: String): ApiResult<Perfil> {
        when (val r = llamar { api.login(LoginRequest(usuario.trim(), clave)) }) {
            is ApiResult.Falla -> return r
            is ApiResult.Exito -> session.guardarToken(r.datos.token)
        }
        return when (val p = llamar { api.perfil() }) {
            is ApiResult.Exito -> {
                session.guardarPerfil(p.datos)
                session.ultimoUsuario = usuario.trim()
                p
            }
            is ApiResult.Falla -> {
                session.cerrarSesion()
                p
            }
        }
    }

    suspend fun registrar(datos: RegistroRequest) = llamar { api.registro(datos) }

    /** RF-12: avisa al servidor (revoca el token) y SIEMPRE borra la sesión local. */
    suspend fun cerrarSesion() {
        llamar { api.logout() }
        session.cerrarSesion()
    }

    suspend fun perfil() = llamar { api.perfil() }

    suspend fun estadisticas() = llamar { api.estadisticas() }

    // ----------------------------------------------------------- propietarios

    suspend fun propietarios(texto: String? = null, documento: String? = null, estado: String? = null) =
        llamar { api.propietarios(texto.limpio(), documento.limpio(), estado.limpio()) }

    suspend fun propietario(id: Long) = llamar { api.propietario(id) }

    suspend fun guardarPropietario(p: Propietario) =
        llamar { if (p.id == null) api.crearPropietario(p) else api.editarPropietario(p) }

    suspend fun eliminarPropietario(id: Long) = llamarSinCuerpo { api.eliminarPropietario(id) }

    // --------------------------------------------------------------- mascotas

    suspend fun mascotas(
        texto: String? = null,
        especie: String? = null,
        estado: String? = null,
        idPropietario: Long? = null
    ) = llamar { api.mascotas(texto.limpio(), especie.limpio(), estado.limpio(), idPropietario) }

    suspend fun mascota(id: Long) = llamar { api.mascota(id) }

    suspend fun guardarMascota(m: Mascota) =
        llamar { if (m.id == null) api.crearMascota(m) else api.editarMascota(m) }

    suspend fun desactivarMascota(id: Long) = llamarSinCuerpo { api.desactivarMascota(id) }

    // ----------------------------------------------------------- veterinarios

    suspend fun veterinarios(texto: String? = null, estado: String? = null) =
        llamar { api.veterinarios(texto.limpio(), estado.limpio()) }

    suspend fun veterinario(id: Long) = llamar { api.veterinario(id) }

    suspend fun guardarVeterinario(v: Veterinario) =
        llamar { if (v.id == null) api.crearVeterinario(v) else api.editarVeterinario(v) }

    suspend fun desactivarVeterinario(id: Long) = llamarSinCuerpo { api.desactivarVeterinario(id) }

    // ------------------------------------------------------- usuarios (solo ADMIN)

    suspend fun usuarios() = llamar { api.usuarios() }

    /** Activa o desactiva una cuenta conservando sus demás datos. */
    suspend fun cambiarEstadoUsuario(u: Usuario, activo: Boolean) =
        llamarSinCuerpo {
            api.cambiarUsuario(UsuarioCambio(u.idUsuario, u.username, u.nombre, u.email, activo))
        }

    // ------------------------------------------------------------------ citas

    suspend fun citas(
        estado: String? = null,
        idVeterinario: Long? = null,
        idMascota: Long? = null,
        fecha: String? = null
    ) = llamar { api.citas(estado.limpio(), idVeterinario, idMascota, fecha.limpio()) }

    suspend fun cita(id: Long) = llamar { api.cita(id) }

    suspend fun guardarCita(c: Cita) =
        llamar { if (c.idCita == null) api.agendarCita(c) else api.editarCita(c) }

    suspend fun cancelarCita(id: Long, motivo: String? = null) =
        llamar { api.cancelarCita(id, CancelarCitaRequest(motivo.limpio())) }

    suspend fun atenderCita(id: Long, datos: AtenderCitaRequest) =
        llamar { api.atenderCita(id, datos) }

    // ------------------------------------------------------- historia clínica

    suspend fun historiales(idMascota: Long? = null, idVeterinario: Long? = null) =
        llamar { api.historiales(idMascota, idVeterinario) }

    suspend fun guardarHistorial(h: HistorialClinico) =
        llamar { if (h.idHistorial == null) api.crearHistorial(h) else api.editarHistorial(h) }

    suspend fun tratamientos(idHistorial: Long) = llamar { api.tratamientos(idHistorial) }

    suspend fun crearTratamiento(t: Tratamiento) = llamar { api.crearTratamiento(t) }

    // ---------------------------------------------------------------- vacunas

    suspend fun vacunaciones(idMascota: Long? = null, proximasDias: Int? = null) =
        llamar { api.vacunaciones(idMascota, proximasDias) }

    suspend fun registrarVacunacion(v: Vacunacion) = llamar { api.registrarVacunacion(v) }

    suspend fun alergiasDeMascota(idMascota: Long) = llamar { api.alergiasDeMascota(idMascota) }

    suspend fun registrarAlergia(a: MascotaAlergia) = llamar { api.registrarAlergia(a) }

    // -------------------------------------------------------------- catálogos

    suspend fun vacunas(texto: String? = null) = llamar { api.vacunas(texto.limpio()) }

    suspend fun medicamentos(texto: String? = null) = llamar { api.medicamentos(texto.limpio()) }

    suspend fun alergias(texto: String? = null) = llamar { api.alergias(texto.limpio()) }

    // ---------------------------------------------------------------- interno

    /** Un filtro vacío ("") se envía como "sin filtro" (null). */
    private fun String?.limpio(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
}