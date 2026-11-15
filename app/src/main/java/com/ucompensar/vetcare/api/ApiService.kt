package com.ucompensar.vetcare.api

import com.ucompensar.vetcare.models.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Cada función es una llamada a la API. La URL base ya termina en ".../veterinaria/api/",
 * así que aquí solo va el resto de la ruta.
 * "suspend" = la función espera la respuesta sin congelar la pantalla.
 */
interface ApiService {

    // ------------------------------------------------------------ autenticación
    @POST("login")
    suspend fun login(@Body datos: LoginRequest): Response<LoginResponse>

    @POST("registro")
    suspend fun registro(@Body datos: RegistroRequest): Response<MensajeResponse>

    @POST("logout")
    suspend fun logout(): Response<MensajeResponse>

    @GET("me")
    suspend fun perfil(): Response<Perfil>

    @GET("estadisticas")
    suspend fun estadisticas(): Response<Estadisticas>

    // ------------------------------------------------------------ propietarios
    @GET("propietarios")
    suspend fun propietarios(
        @Query("q") texto: String? = null,
        @Query("documento") documento: String? = null,
        @Query("estado") estado: String? = null           // ACTIVO (por defecto), INACTIVO o TODOS
    ): Response<List<Propietario>>

    @GET("propietarios")
    suspend fun propietario(@Query("id") id: Long): Response<Propietario>

    @POST("propietarios")
    suspend fun crearPropietario(@Body datos: Propietario): Response<Propietario>

    @PUT("propietarios")
    suspend fun editarPropietario(@Body datos: Propietario): Response<Propietario>

    @DELETE("propietarios")
    suspend fun eliminarPropietario(@Query("id") id: Long): Response<Unit>

    // ---------------------------------------------------------------- mascotas
    @GET("mascotas")
    suspend fun mascotas(
        @Query("q") texto: String? = null,
        @Query("especie") especie: String? = null,
        @Query("estado") estado: String? = null,          // ACTIVO (por defecto), INACTIVO o TODOS
        @Query("idPropietario") idPropietario: Long? = null
    ): Response<List<Mascota>>

    @GET("mascotas")
    suspend fun mascota(@Query("id") id: Long): Response<Mascota>

    @POST("mascotas")
    suspend fun crearMascota(@Body datos: Mascota): Response<Mascota>

    @PUT("mascotas")
    suspend fun editarMascota(@Body datos: Mascota): Response<Mascota>

    @DELETE("mascotas")
    suspend fun desactivarMascota(@Query("id") id: Long): Response<Unit>

    // ------------------------------------------------------------ veterinarios
    @GET("veterinarios")
    suspend fun veterinarios(
        @Query("q") texto: String? = null,
        @Query("estado") estado: String? = null
    ): Response<List<Veterinario>>

    @GET("veterinarios")
    suspend fun veterinario(@Query("id") id: Long): Response<Veterinario>

    @POST("veterinarios")
    suspend fun crearVeterinario(@Body datos: Veterinario): Response<Veterinario>

    @PUT("veterinarios")
    suspend fun editarVeterinario(@Body datos: Veterinario): Response<Veterinario>

    /** El servidor no borra: marca al veterinario como INACTIVO (204). */
    @DELETE("veterinarios")
    suspend fun desactivarVeterinario(@Query("id") id: Long): Response<Unit>

    // ---------------------------------------------------------------- usuarios (solo ADMIN)
    @GET("usuarios")
    suspend fun usuarios(): Response<List<Usuario>>

    @PUT("usuarios")
    suspend fun cambiarUsuario(@Body datos: UsuarioCambio): Response<Unit>

    // ------------------------------------------------------------------- citas
    @GET("citas")
    suspend fun citas(
        @Query("estado") estado: String? = null,
        @Query("idVeterinario") idVeterinario: Long? = null,
        @Query("idMascota") idMascota: Long? = null,
        @Query("fecha") fecha: String? = null              // AAAA-MM-DD
    ): Response<List<Cita>>

    @GET("citas/{id}")
    suspend fun cita(@Path("id") id: Long): Response<Cita>

    @POST("citas")
    suspend fun agendarCita(@Body datos: Cita): Response<Cita>

    @PUT("citas")
    suspend fun editarCita(@Body datos: Cita): Response<Cita>

    @POST("citas/{id}/cancelar")
    suspend fun cancelarCita(
        @Path("id") id: Long,
        @Body datos: CancelarCitaRequest
    ): Response<Cita>

    @POST("citas/{id}/atender")
    suspend fun atenderCita(
        @Path("id") id: Long,
        @Body datos: AtenderCitaRequest
    ): Response<Cita>

    // ------------------------------------------------------- historia clínica
    @GET("historiales")
    suspend fun historiales(
        @Query("idMascota") idMascota: Long? = null,
        @Query("idVeterinario") idVeterinario: Long? = null
    ): Response<List<HistorialClinico>>

    @POST("historiales")
    suspend fun crearHistorial(@Body datos: HistorialClinico): Response<HistorialClinico>

    @PUT("historiales")
    suspend fun editarHistorial(@Body datos: HistorialClinico): Response<HistorialClinico>

    @GET("tratamientos")
    suspend fun tratamientos(@Query("idHistorial") idHistorial: Long): Response<List<Tratamiento>>

    @POST("tratamientos")
    suspend fun crearTratamiento(@Body datos: Tratamiento): Response<Tratamiento>

    // ---------------------------------------------------------------- vacunas
    @GET("vacunaciones")
    suspend fun vacunaciones(
        @Query("idMascota") idMascota: Long? = null,
        @Query("proximasDias") proximasDias: Int? = null
    ): Response<List<Vacunacion>>

    @POST("vacunaciones")
    suspend fun registrarVacunacion(@Body datos: Vacunacion): Response<Vacunacion>

    @GET("mascota-alergias")
    suspend fun alergiasDeMascota(@Query("idMascota") idMascota: Long): Response<List<MascotaAlergia>>

    @POST("mascota-alergias")
    suspend fun registrarAlergia(@Body datos: MascotaAlergia): Response<MascotaAlergia>

    // -------------------------------------------------------------- catálogos
    @GET("vacunas")
    suspend fun vacunas(@Query("q") texto: String? = null): Response<List<Vacuna>>

    @GET("medicamentos")
    suspend fun medicamentos(@Query("q") texto: String? = null): Response<List<Medicamento>>

    @GET("alergias")
    suspend fun alergias(@Query("q") texto: String? = null): Response<List<Alergia>>
}