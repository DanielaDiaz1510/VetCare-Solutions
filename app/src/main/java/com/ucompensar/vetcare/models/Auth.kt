package com.ucompensar.vetcare.models

data class LoginRequest(val username: String, val password: String)

data class LoginResponse(
    val token: String,
    val tipo: String,
    val expiraEnSegundos: Long
)

data class RegistroRequest(
    val username: String,
    val password: String,
    val nombre: String,
    val email: String,
    // Rol que se pide ("VETERINARIO" o "ADMIN"). El servidor deja la cuenta inactiva hasta que un admin la apruebe.
    val rolSolicitado: String? = null,
    // Datos del perfil de veterinario (solo cuando se pide ese rol)
    val tipoDocumento: String? = null,
    val numeroDocumento: String? = null,
    val nombres: String? = null,
    val apellidos: String? = null,
    val especialidad: String? = null,
    val telefono: String? = null
)

data class MensajeResponse(val mensaje: String?)

/** Respuesta de GET /api/me: el perfil que se muestra en el Home. */
data class Perfil(
    val idUsuario: Long,
    val username: String,
    val nombre: String,
    val email: String?,
    val estado: Boolean,
    val roles: List<String>,
    val idVeterinario: Long?,
    val nombreProfesional: String?
) {
    val esAdmin: Boolean get() = roles.contains("ADMIN")
}

data class Estadisticas(
    val mascotas: Int,
    val propietarios: Int,
    val veterinarios: Int,
    val citasProgramadas: Int,
    val citasHoy: Int,
    val vacunasPorVencer30Dias: Int
)

/** Formato único de error de la API: {status, codigo, error, ruta}. */
data class ApiError(
    val status: Int?,
    val codigo: String?,
    val error: String?,
    val ruta: String?
)