package com.ucompensar.vetcare

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.ucompensar.vetcare.api.ApiResult
import com.ucompensar.vetcare.databinding.ActivityHistoriasBinding
import com.ucompensar.vetcare.databinding.DialogTratamientoBinding
import com.ucompensar.vetcare.models.HistorialClinico
import com.ucompensar.vetcare.models.Mascota
import com.ucompensar.vetcare.models.Medicamento
import com.ucompensar.vetcare.models.Tratamiento
import kotlinx.coroutines.launch

/**
 * Historia clínica: lista de atenciones, con filtro por mascota (RF-09).
 * Al tocar una: ver detalle con sus tratamientos (RF-10), editarla o agregarle un tratamiento.
 */
class HistoriasActivity : BaseActivity() {

    private lateinit var binding: ActivityHistoriasBinding
    private lateinit var adaptador: HistoriaAdapter
    private var mascotas: List<Mascota> = emptyList()
    private var mascotasListas = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoriasBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ajustarBarras(binding.raiz)

        adaptador = HistoriaAdapter { historia -> mostrarOpciones(historia) }
        binding.rvHistorias.layoutManager = LinearLayoutManager(this)
        binding.rvHistorias.adapter = adaptador

        binding.btnBuscar.setOnClickListener { cargar() }

        // Al elegir una mascota, la lista se recarga sola (sin tener que pulsar Buscar).
        binding.spMascota.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                if (mascotasListas) cargar()
            }

            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        binding.btnNueva.setOnClickListener {
            startActivity(Intent(this, RegistroHistoriaActivity::class.java))
        }

        cargarMascotas()
    }

    /** Se recarga al volver de crear o editar, para ver los cambios. */
    override fun onResume() {
        super.onResume()
        if (mascotasListas) cargar()
    }

    // --------------------------------------------------------------- consulta

    /** Llena el filtro con todas las mascotas (también las inactivas: conservan su historia). */
    private fun cargarMascotas() {
        binding.progreso.visibility = View.VISIBLE
        lifecycleScope.launch {
            when (val r = repo.mascotas(estado = "TODOS")) {
                is ApiResult.Exito -> {
                    mascotas = r.datos
                    val nombres = listOf("Todas las mascotas") +
                            mascotas.map { "${it.nombre} (${it.especie})" }
                    binding.spMascota.adapter = ArrayAdapter(
                        this@HistoriasActivity,
                        android.R.layout.simple_spinner_dropdown_item,
                        nombres
                    )
                    mascotasListas = true
                    cargar()
                }
                is ApiResult.Falla -> {
                    binding.progreso.visibility = View.GONE
                    mostrarFalla(r)
                }
            }
        }
    }

    private fun cargar() {
        binding.progreso.visibility = View.VISIBLE
        val pos = binding.spMascota.selectedItemPosition
        val idMascota = if (pos <= 0) null else mascotas.getOrNull(pos - 1)?.id
        lifecycleScope.launch {
            val resultado = repo.historiales(idMascota = idMascota)
            binding.progreso.visibility = View.GONE
            when (resultado) {
                is ApiResult.Exito -> {
                    val lista = resultado.datos
                    adaptador.actualizar(lista)
                    binding.tvContador.text = "${lista.size} atención(es)"
                    binding.tvVacio.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
                }
                is ApiResult.Falla -> {
                    binding.tvVacio.visibility = View.GONE
                    mostrarFalla(resultado)
                }
            }
        }
    }

    // ---------------------------------------------------------------- acciones

    private fun mostrarOpciones(h: HistorialClinico) {
        val opciones = arrayOf("Ver detalle y tratamientos", "Editar atención", "Agregar tratamiento")
        AlertDialog.Builder(this)
            .setTitle("${h.nombreMascota ?: "Atención"} · ${formatoFecha(h.fechaAtencion)}")
            .setItems(opciones) { _, i ->
                when (i) {
                    0 -> verDetalle(h)
                    1 -> editar(h)
                    2 -> agregarTratamiento(h)
                }
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun verDetalle(h: HistorialClinico) {
        val id = h.idHistorial ?: return
        lifecycleScope.launch {
            val tratamientos = when (val r = repo.tratamientos(id)) {
                is ApiResult.Exito -> r.datos
                is ApiResult.Falla -> {
                    mostrarError(r)
                    return@launch
                }
            }
            val lineas = if (tratamientos.isEmpty()) {
                "Sin tratamientos registrados"
            } else {
                tratamientos.joinToString("\n") { t ->
                    var linea = "• ${t.nombreMedicamento ?: "—"}: ${t.dosis ?: "—"}, ${t.frecuencia ?: "—"}"
                    if (t.duracion != null) linea += ", ${t.duracion} día(s)"
                    if (!t.indicaciones.isNullOrEmpty()) linea += "\n   ${t.indicaciones}"
                    linea
                }
            }
            val texto = "Fecha: ${formatoFecha(h.fechaAtencion)}\n" +
                    "Mascota: ${h.nombreMascota ?: "—"}\n" +
                    "Atendió: ${h.nombreCompletoVeterinario ?: "—"}\n" +
                    "Motivo: ${h.motivoConsulta}\n" +
                    "Diagnóstico: ${h.diagnostico ?: "—"}\n" +
                    "Observaciones: ${h.observaciones ?: "—"}\n" +
                    "Peso: ${h.peso?.let { "$it kg" } ?: "—"}\n" +
                    "Temperatura: ${h.temperatura?.let { "$it °C" } ?: "—"}\n\n" +
                    "Tratamientos:\n$lineas"
            AlertDialog.Builder(this@HistoriasActivity)
                .setTitle("Detalle de la atención")
                .setMessage(texto)
                .setPositiveButton("Cerrar", null)
                .show()
        }
    }

    private fun editar(h: HistorialClinico) {
        val intent = Intent(this, RegistroHistoriaActivity::class.java)
        intent.putExtra(RegistroHistoriaActivity.EXTRA_JSON, Gson().toJson(h))
        startActivity(intent)
    }

    // ---- tratamientos

    private fun agregarTratamiento(h: HistorialClinico) {
        val idHistorial = h.idHistorial ?: return
        lifecycleScope.launch {
            val medicamentos = when (val r = repo.medicamentos()) {
                is ApiResult.Exito -> r.datos
                is ApiResult.Falla -> {
                    mostrarError(r)
                    return@launch
                }
            }
            if (medicamentos.isEmpty()) {
                Toast.makeText(this@HistoriasActivity, "No hay medicamentos en el catálogo", Toast.LENGTH_LONG).show()
                return@launch
            }
            dialogoTratamiento(idHistorial, medicamentos)
        }
    }

    private fun dialogoTratamiento(idHistorial: Long, medicamentos: List<Medicamento>) {
        val d = DialogTratamientoBinding.inflate(layoutInflater)
        d.spMedicamento.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            medicamentos.map { m ->
                if (m.presentacion.isNullOrEmpty()) m.nombre else "${m.nombre} (${m.presentacion})"
            }
        )

        val dialogo = AlertDialog.Builder(this)
            .setTitle("Agregar tratamiento")
            .setView(d.root)
            .setPositiveButton("Agregar", null)      // se define abajo para poder validar sin cerrar
            .setNegativeButton("Cancelar", null)
            .create()

        dialogo.setOnShowListener {
            dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                d.tilDosis.error = null
                d.tilFrecuencia.error = null
                d.tilDuracion.error = null

                val dosis = d.etDosis.text.toString().trim()
                val frecuencia = d.etFrecuencia.text.toString().trim()
                val duracionTexto = d.etDuracion.text.toString().trim()
                val duracion = duracionTexto.toIntOrNull()

                var valido = true
                if (dosis.isEmpty()) {
                    d.tilDosis.error = "Ingrese la dosis"
                    valido = false
                }
                if (frecuencia.isEmpty()) {
                    d.tilFrecuencia.error = "Ingrese la frecuencia"
                    valido = false
                }
                if (duracionTexto.isNotEmpty() && (duracion == null || duracion < 1 || duracion > 365)) {
                    d.tilDuracion.error = "Entre 1 y 365 días"
                    valido = false
                }
                val idMedicamento = medicamentos.getOrNull(d.spMedicamento.selectedItemPosition)?.idMedicamento
                if (!valido || idMedicamento == null) return@setOnClickListener

                val tratamiento = Tratamiento(
                    dosis = dosis,
                    frecuencia = frecuencia,
                    duracion = duracion,
                    indicaciones = d.etIndicaciones.text.toString().trim().ifEmpty { null },
                    idHistorial = idHistorial,
                    idMedicamento = idMedicamento
                )
                dialogo.dismiss()
                crearTratamiento(tratamiento)
            }
        }
        dialogo.show()
    }

    private fun crearTratamiento(t: Tratamiento) {
        lifecycleScope.launch {
            when (val r = repo.crearTratamiento(t)) {
                is ApiResult.Exito ->
                    Toast.makeText(this@HistoriasActivity, "Tratamiento agregado", Toast.LENGTH_SHORT).show()
                // 403 si la atención es de otro veterinario
                is ApiResult.Falla -> mostrarError(r)
            }
        }
    }

    /** 401, 403 y falta de conexión los maneja BaseActivity; el resto se muestra en un diálogo. */
    private fun mostrarError(r: ApiResult.Falla) {
        if (r.sesionInvalida || r.sinPermiso || r.sinConexion) {
            mostrarFalla(r)
        } else {
            AlertDialog.Builder(this)
                .setTitle("No se pudo completar")
                .setMessage(r.mensaje)
                .setPositiveButton("Entendido", null)
                .show()
        }
    }
}