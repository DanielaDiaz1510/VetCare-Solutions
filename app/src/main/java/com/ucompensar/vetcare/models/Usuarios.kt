package com.ucompensar.vetcare.models

/** Cuenta de acceso tal como la devuelve GET /api/usuarios (solo ADMIN). */
data class Usuario(
    val idUsuario: Long,
    val username: String,
    val nombre: String,
    val email: String,
    val estado: Boolean? = null,           // true = activo; false = pendiente o desactivado
    val roles: List<String>? = null
) {
    val activo: Boolean get() = estado == true
    val esVeterinario: Boolean get() = roles?.contains("VETERINARIO") == true
    val esAdmin: Boolean get() = roles?.contains("ADMIN") == true
}

/**
 * Cuerpo del PUT /api/usuarios para activar o desactivar.
 * No lleva "roles" ni "password": el servidor deja esos datos como están.
 */
data class UsuarioCambio(
    val idUsuario: Long,
    val username: String,
    val nombre: String,
    val email: String,
    val estado: Boolean
)
