package com.ucompensar.vetcare.models

data class Propietario(
    val id: Long? = null,              // null al crear; lo asigna el servidor
    val tipoDocumento: String,
    val numeroDocumento: String,
    val nombres: String,
    val apellidos: String,
    val telefono: String? = null,
    val email: String? = null,
    val direccion: String? = null,
    val estado: String? = null          // "ACTIVO" / "INACTIVO"; null al crear (el servidor pone ACTIVO)
) {
    val nombreCompleto: String get() = "$nombres $apellidos"
}

data class Mascota(
    val id: Long? = null,
    val nombre: String,
    val especie: String,
    val raza: String? = null,
    val fechaNacimiento: String? = null,   // "2020-03-15"
    val sexo: String? = null,              // "M" o "H"
    val color: String? = null,
    val pesoActual: Double? = null,
    val estado: String? = null,            // "ACTIVO" / "INACTIVO"
    val idPropietario: Long,
    val nombrePropietario: String? = null
)

data class Veterinario(
    val id: Long? = null,
    val tipoDocumento: String,
    val numeroDocumento: String,
    val nombres: String,
    val apellidos: String,
    val especialidad: String? = null,
    val telefono: String? = null,
    val email: String? = null,
    val estado: String? = null,
    val idUsuario: Long? = null
) {
    val nombreCompleto: String get() = "$nombres $apellidos"
}