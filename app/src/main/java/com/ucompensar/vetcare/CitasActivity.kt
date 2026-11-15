package com.ucompensar.vetcare

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ucompensar.vetcare.api.ApiResult
import com.ucompensar.vetcare.databinding.ActivityCitasBinding
import com.ucompensar.vetcare.databinding.DialogAtenderBinding
import com.ucompensar.vetcare.models.AtenderCitaRequest
import com.ucompensar.vetcare.models.Cita
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

/**
 * Lista de citas con filtros por estado y por fecha (RF-09).
 * Al tocar una cita: ver detalle (RF-10) y, si está PROGRAMADA, reprogramar, cancelar o atender.
 */
class CitasActivity : BaseActivity() {

    private lateinit var binding: ActivityCitasBinding
    private lateinit var adaptador: CitaAdapter

    private val estados = listOf("Todas", "Programadas", "Atendidas", "Canceladas")
    private val valoresEstado = listOf(null, "PROGRAMADA", "ATENDIDA", "CANCELADA")
    private var fechaFiltro: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCitasBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ajustarBarras(binding.raiz)

        binding.spEstado.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, estados)

        // Al elegir un estado, la lista se recarga sola (sin tener que pulsar Buscar).
        binding.spEstado.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                cargar()
            }

            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        adaptador = CitaAdapter { cita -> mostrarOpciones(cita) }
        binding.rvCitas.layoutManager = LinearLayoutManager(this)
        binding.rvCitas.adapter = adaptador

        binding.etFecha.setOnClickListener { elegirFechaFiltro() }
        binding.btnBuscar.setOnClickListener { cargar() }
        binding.btnQuitarFecha.setOnClickListener {
            fechaFiltro = null
            binding.etFecha.setText("")
            cargar()
        }
        binding.btnNueva.setOnClickListener {
            startActivity(Intent(this, AgendarCitaActivity::class.java))
        }
    }

    /** Se recarga al volver de agendar o reprogramar, para ver los cambios. */
    override fun onResume() {
        super.onResume()
        cargar()
    }

    // --------------------------------------------------------------- consulta

    private fun elegirFechaFiltro() {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, a, m, d ->
            fechaFiltro = String.format(Locale.US, "%04d-%02d-%02d", a, m + 1, d)
            binding.etFecha.setText(fechaFiltro)
            cargar()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun cargar() {
        binding.progreso.visibility = View.VISIBLE
        lifecycleScope.launch {
            val resultado = repo.citas(
                estado = valoresEstado[binding.spEstado.selectedItemPosition],
                idVeterinario = null,
                idMascota = null,
                fecha = fechaFiltro
            )
            binding.progreso.visibility = View.GONE
            when (resultado) {
                is ApiResult.Exito -> {
                    val lista = resultado.datos
                    adaptador.actualizar(lista)
                    binding.tvContador.text = "${lista.size} cita(s)"
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

    private fun mostrarOpciones(c: Cita) {
        val opciones = mutableListOf("Ver detalle")
        if (c.estado == "PROGRAMADA") {
            opciones.add("Reprogramar")
            opciones.add("Cancelar cita")
            opciones.add("Atender")
        }
        AlertDialog.Builder(this)
            .setTitle("Cita de ${c.nombreMascota ?: "—"}")
            .setItems(opciones.toTypedArray()) { _, i ->
                when (opciones[i]) {
                    "Ver detalle" -> verDetalle(c)
                    "Reprogramar" -> reprogramar(c)
                    "Cancelar cita" -> pedirMotivoCancelacion(c)
                    "Atender" -> dialogoAtender(c)
                }
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun verDetalle(c: Cita) {
        val texto = "Mascota: ${c.nombreMascota ?: "—"}\n" +
                "Veterinario: ${c.nombreVeterinario ?: "—"}\n" +
                "Inicio: ${formatoFecha(c.fechaHora)}\n" +
                "Fin: ${formatoFecha(c.fechaFin)}\n" +
                "Duración: ${c.duracionMinutos ?: "—"} min\n" +
                "Motivo: ${c.motivo ?: "—"}\n" +
                "Estado: ${c.estado ?: "—"}\n" +
                "Observaciones: ${c.observaciones ?: "—"}\n" +
                "Historia clínica: ${c.idHistorial?.let { "n.º $it" } ?: "no registrada"}"
        AlertDialog.Builder(this)
            .setTitle("Detalle de la cita")
            .setMessage(texto)
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun reprogramar(c: Cita) {
        val id = c.idCita ?: return
        val intent = Intent(this, AgendarCitaActivity::class.java)
        intent.putExtra(AgendarCitaActivity.EXTRA_ID, id)
        startActivity(intent)
    }

    // ---- cancelar

    private fun pedirMotivoCancelacion(c: Cita) {
        val id = c.idCita ?: return
        val campo = EditText(this)
        campo.hint = "Motivo (opcional)"
        val margen = (20 * resources.displayMetrics.density).toInt()
        val contenedor = FrameLayout(this)
        contenedor.setPadding(margen, 8, margen, 0)
        contenedor.addView(campo)

        AlertDialog.Builder(this)
            .setTitle("Cancelar cita")
            .setMessage("¿Cancelar la cita de ${c.nombreMascota ?: "la mascota"}?")
            .setView(contenedor)
            .setPositiveButton("Cancelar cita") { _, _ ->
                cancelar(id, campo.text.toString().trim())
            }
            .setNegativeButton("Volver", null)
            .show()
    }

    private fun cancelar(id: Long, motivo: String) {
        lifecycleScope.launch {
            when (val r = repo.cancelarCita(id, motivo)) {
                is ApiResult.Exito -> {
                    Toast.makeText(this@CitasActivity, "Cita cancelada", Toast.LENGTH_SHORT).show()
                    cargar()
                }
                is ApiResult.Falla -> mostrarError(r)
            }
        }
    }

    // ---- atender

    private fun dialogoAtender(c: Cita) {
        val id = c.idCita ?: return
        val d = DialogAtenderBinding.inflate(layoutInflater)

        val dialogo = AlertDialog.Builder(this)
            .setTitle("Atender cita")
            .setView(d.root)
            .setPositiveButton("Atender", null)      // se define abajo para poder validar sin cerrar
            .setNegativeButton("Cancelar", null)
            .create()

        dialogo.setOnShowListener {
            dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                d.tilPeso.error = null
                d.tilTemperatura.error = null

                val pesoTexto = d.etPeso.text.toString().trim().replace(',', '.')
                val tempTexto = d.etTemperatura.text.toString().trim().replace(',', '.')
                val peso = pesoTexto.toDoubleOrNull()
                val temperatura = tempTexto.toDoubleOrNull()

                var valido = true
                if (pesoTexto.isNotEmpty() && (peso == null || peso <= 0.0)) {
                    d.tilPeso.error = "Número mayor que 0"
                    valido = false
                }
                if (tempTexto.isNotEmpty() && (temperatura == null || temperatura <= 0.0)) {
                    d.tilTemperatura.error = "Número mayor que 0"
                    valido = false
                }
                if (!valido) return@setOnClickListener

                val pedido = AtenderCitaRequest(
                    diagnostico = d.etDiagnostico.text.toString().trim().ifEmpty { null },
                    observaciones = d.etObservaciones.text.toString().trim().ifEmpty { null },
                    peso = peso,
                    temperatura = temperatura
                )
                dialogo.dismiss()
                atender(id, pedido)
            }
        }
        dialogo.show()
    }

    private fun atender(id: Long, pedido: AtenderCitaRequest) {
        lifecycleScope.launch {
            when (val r = repo.atenderCita(id, pedido)) {
                is ApiResult.Exito -> {
                    val conHistoria = pedido.diagnostico != null
                    Toast.makeText(
                        this@CitasActivity,
                        if (conHistoria) "Cita atendida y historia clínica creada" else "Cita atendida",
                        Toast.LENGTH_LONG
                    ).show()
                    cargar()
                }
                is ApiResult.Falla -> mostrarError(r)
            }
        }
    }

    /**
     * 401, 403 y falta de conexión los maneja BaseActivity. Los errores de reglas (409 por
     * "atender por adelantado", cita ya cancelada, etc.) se muestran en un diálogo para poder leerlos.
     */
    private fun mostrarError(r: ApiResult.Falla) {
        if (r.sesionInvalida || r.sinPermiso || r.sinConexion) {
            mostrarFalla(r)
        } else {
            AlertDialog.Builder(this)
                .setTitle("No se pudo completar")
                .setMessage(r.mensaje)
                .setPositiveButton("Entendido", null)
                .show()
            cargar()
        }
    }
}