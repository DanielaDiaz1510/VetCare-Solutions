package com.ucompensar.vetcare

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.ucompensar.vetcare.api.ApiResult
import com.ucompensar.vetcare.databinding.ActivityRegistroVacunacionBinding
import com.ucompensar.vetcare.models.Mascota
import com.ucompensar.vetcare.models.Vacuna
import com.ucompensar.vetcare.models.Vacunacion
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

/**
 * Registra una vacunación en el carnet de una mascota (POST).
 * Reglas (las mismas del servidor): la aplicación no puede ser futura y el refuerzo
 * no puede ser anterior a la aplicación. Las fechas viajan como AAAA-MM-DD.
 */
class RegistroVacunacionActivity : BaseActivity() {

    private lateinit var binding: ActivityRegistroVacunacionBinding
    private var mascotas: List<Mascota> = emptyList()
    private var vacunas: List<Vacuna> = emptyList()
    private var fechaAplicacion: String? = null
    private var fechaProxima: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroVacunacionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ajustarBarras(binding.raiz)

        binding.etAplicacion.setOnClickListener {
            elegirFecha(fechaAplicacion, soloPasado = true) { fecha ->
                fechaAplicacion = fecha
                binding.etAplicacion.setText(fecha)
            }
        }
        binding.etProxima.setOnClickListener {
            elegirFecha(fechaProxima ?: fechaAplicacion, soloPasado = false) { fecha ->
                fechaProxima = fecha
                binding.etProxima.setText(fecha)
            }
        }
        binding.btnQuitarProxima.setOnClickListener {
            fechaProxima = null
            binding.etProxima.setText("")
        }
        binding.btnGuardar.setOnClickListener { guardar() }
        binding.btnCancelar.setOnClickListener { finish() }

        cargarListas()
    }

    /** Trae las mascotas activas y el catálogo de vacunas para los selectores. */
    private fun cargarListas() {
        ocupado(true)
        lifecycleScope.launch {
            when (val m = repo.mascotas()) {
                is ApiResult.Exito -> mascotas = m.datos
                is ApiResult.Falla -> {
                    ocupado(false)
                    mostrarFalla(m)
                    return@launch
                }
            }
            when (val v = repo.vacunas()) {
                is ApiResult.Exito -> vacunas = v.datos
                is ApiResult.Falla -> {
                    ocupado(false)
                    mostrarFalla(v)
                    return@launch
                }
            }

            binding.spMascota.adapter = ArrayAdapter(
                this@RegistroVacunacionActivity,
                android.R.layout.simple_spinner_dropdown_item,
                mascotas.map { "${it.nombre} (${it.especie})" }
            )
            binding.spVacuna.adapter = ArrayAdapter(
                this@RegistroVacunacionActivity,
                android.R.layout.simple_spinner_dropdown_item,
                vacunas.map { v ->
                    if (v.especieAplicable.isNullOrEmpty()) v.nombre else "${v.nombre} (${v.especieAplicable})"
                }
            )

            ocupado(false)
            if (mascotas.isEmpty() || vacunas.isEmpty()) {
                binding.tvError.text = "Se necesita al menos una mascota activa y una vacuna en el catálogo."
                binding.tvError.visibility = View.VISIBLE
                binding.btnGuardar.isEnabled = false
            }
        }
    }

    // ---------------------------------------------------------------- fechas

    private fun hoy(): String {
        val c = Calendar.getInstance()
        return String.format(
            Locale.US, "%04d-%02d-%02d",
            c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH)
        )
    }

    private fun elegirFecha(inicial: String?, soloPasado: Boolean, alElegir: (String) -> Unit) {
        val cal = Calendar.getInstance()
        var anio = cal.get(Calendar.YEAR)
        var mes = cal.get(Calendar.MONTH)
        var dia = cal.get(Calendar.DAY_OF_MONTH)

        // Si ya hay una fecha escrita (AAAA-MM-DD), el selector abre en ella
        val partes = inicial?.split("-")
        if (partes != null && partes.size == 3) {
            anio = partes[0].toIntOrNull() ?: anio
            mes = (partes[1].toIntOrNull() ?: (mes + 1)) - 1
            dia = partes[2].toIntOrNull() ?: dia
        }

        val dialogo = DatePickerDialog(this, { _, a, m, d ->
            alElegir(String.format(Locale.US, "%04d-%02d-%02d", a, m + 1, d))
        }, anio, mes, dia)
        if (soloPasado) dialogo.datePicker.maxDate = System.currentTimeMillis()   // la aplicación no puede ser futura
        dialogo.show()
    }

    // --------------------------------------------------------------- guardar

    private fun guardar() {
        limpiarErrores()

        val aplicacion = fechaAplicacion
        val proxima = fechaProxima
        val observaciones = binding.etObservaciones.text.toString().trim()
        val idMascota = mascotas.getOrNull(binding.spMascota.selectedItemPosition)?.id
        val idVacuna = vacunas.getOrNull(binding.spVacuna.selectedItemPosition)?.idVacuna

        var valido = true
        if (idMascota == null || idVacuna == null) {
            binding.tvError.text = "Seleccione la mascota y la vacuna"
            binding.tvError.visibility = View.VISIBLE
            valido = false
        }
        if (aplicacion == null) {
            binding.tilAplicacion.error = "Elija la fecha de aplicación"
            valido = false
        } else if (aplicacion > hoy()) {
            binding.tilAplicacion.error = "La aplicación no puede estar en el futuro"
            valido = false
        }
        // Las fechas AAAA-MM-DD se pueden comparar como texto
        if (aplicacion != null && proxima != null && proxima < aplicacion) {
            binding.tilProxima.error = "El refuerzo no puede ser anterior a la aplicación"
            valido = false
        }
        if (observaciones.length > 255) {
            binding.tilObservaciones.error = "Máximo 255 caracteres"
            valido = false
        }
        if (!valido || idMascota == null || idVacuna == null || aplicacion == null) return

        val vacunacion = Vacunacion(
            fechaAplicacion = aplicacion,
            fechaProxima = proxima,
            observaciones = observaciones.ifEmpty { null },
            idMascota = idMascota,
            idVacuna = idVacuna
        )

        ocupado(true)
        lifecycleScope.launch {
            when (val r = repo.registrarVacunacion(vacunacion)) {
                is ApiResult.Exito -> {
                    Toast.makeText(this@RegistroVacunacionActivity, "Vacunación registrada", Toast.LENGTH_SHORT).show()
                    finish()
                }
                is ApiResult.Falla -> {
                    ocupado(false)
                    if (r.sesionInvalida || r.sinPermiso || r.sinConexion) {
                        mostrarFalla(r)
                    } else {
                        // 400 validación, 404, 409 mascota inactiva, etc.
                        binding.tvError.text = r.mensaje
                        binding.tvError.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun ocupado(si: Boolean) {
        binding.progreso.visibility = if (si) View.VISIBLE else View.GONE
        binding.btnGuardar.isEnabled = !si
    }

    private fun limpiarErrores() {
        binding.tilAplicacion.error = null
        binding.tilProxima.error = null
        binding.tilObservaciones.error = null
        binding.tvError.visibility = View.GONE
    }
}