package com.ucompensar.vetcare.models

data class Cita(
    val idCita: Long? = null,
    val idMascota: Long,
    val nombreMascota: String? = null,
    val idVeterinario: Long,
    val nombreVeterinario: String? = null,
    val fechaHora: String,                 // "2026-12-20T10:00:00"
    val fechaFin: String? = null,
    val duracionMinutos: Int? = null,
    val motivo: String? = null,
    val estado: String? = null,            // PROGRAMADA / ATENDIDA / CANCELADA
    val observaciones: String? = null,
    val idHistorial: Long? = null
)

data class CancelarCitaRequest(val motivo: String? = null)

data class AtenderCitaRequest(
    val diagnostico: String? = null,
    val observaciones: String? = null,
    val peso: Double? = null,
    val temperatura: Double? = null
)

data class HistorialClinico(
    val idHistorial: Long? = null,
    val fechaAtencion: String? = null,
    val motivoConsulta: String,
    val diagnostico: String? = null,
    val observaciones: String? = null,
    val peso: Double? = null,
    val temperatura: Double? = null,
    val idMascota: Long,
    val nombreMascota: String? = null,
    val idVeterinario: Long? = null,
    val nombreCompletoVeterinario: String? = null
)

data class Tratamiento(
    val idTratamiento: Long? = null,
    val dosis: String? = null,
    val frecuencia: String? = null,
    val duracion: Int? = null,
    val indicaciones: String? = null,
    val idHistorial: Long,
    val idMedicamento: Long,
    val nombreMedicamento: String? = null
)

data class Vacunacion(
    val idVacunacion: Long? = null,
    val fechaAplicacion: String,           // "2026-11-10"
    val fechaProxima: String? = null,
    val observaciones: String? = null,
    val idMascota: Long,
    val nombreMascota: String? = null,
    val idVacuna: Long,
    val nombreVacuna: String? = null
)

data class MascotaAlergia(
    val id: Long? = null,
    val observaciones: String? = null,
    val idMascota: Long,
    val nombreMascota: String? = null,
    val idAlergia: Long,
    val nombreAlergia: String? = null
)