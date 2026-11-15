package com.ucompensar.vetcare.models

data class Vacuna(
    val idVacuna: Long? = null,
    val nombre: String,
    val descripcion: String? = null,
    val especieAplicable: String? = null
)

data class Medicamento(
    val idMedicamento: Long? = null,
    val nombre: String,
    val tipo: String? = null,
    val presentacion: String? = null,
    val descripcion: String? = null
)

data class Alergia(
    val idAlergia: Long? = null,
    val nombre: String,
    val descripcion: String? = null
)