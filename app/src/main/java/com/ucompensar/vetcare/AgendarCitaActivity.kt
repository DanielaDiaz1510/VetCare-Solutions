package com.ucompensar.vetcare

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.ucompensar.vetcare.api.ApiResult
import com.ucompensar.vetcare.databinding.ActivityAgendarCitaBinding
import com.ucompensar.vetcare.models.Cita
import com.ucompensar.vetcare.models.Mascota
import com.ucompensar.vetcare.models.Veterinario
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Agenda una cita (POST) o la reprograma (PUT, cuando llega EXTRA_ID).
 * Al reprogramar no se cambia la mascota. Un VETERINARIO solo puede agendar a su nombre (R7),
 * por eso su selector queda fijo; las demás reglas (R1 a R6) las valida el servidor.
 */
class AgendarCitaActivity : BaseActivity() {

    private lateinit var binding: ActivityAgendarCitaBinding
    private var mascotas: List<Mascota> = emptyList()
    private var veterinarios: List<Veterinario> = emptyList()
    private var citaActual: Cita? = null          // null = cita nueva
    private var fechaIso: String? = null          // "2026-12-20T10:00:00"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAgendarCitaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ajustarBarras(binding.raiz)

        binding.etFecha.setOnClickListener { elegirFechaHora() }
        binding.btnGuardar.setOnClickListener { guardar() }
        binding.btnCancelar.setOnClickListener { finish() }

        val id = intent.getLongExtra(EXTRA_ID, -1L).takeIf { it > 0 }
        binding.tvTitulo.text = if (id == null) "Nueva cita" else "Reprogramar cita"
        cargarInicial(id)
    }

    // ------------------------------------------------------- carga de datos

    private fun cargarInicial(id: Long?) {
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
            when (val v = repo.veterinarios(texto = null, estado = "ACTIVO")) {
                is ApiResult.Exito -> veterinarios = v.datos
                is ApiResult.Falla -> {
                    ocupado(false)
                    mostrarFalla(v)
                    return@launch
                }
            }

            binding.spMascota.adapter = ArrayAdapter(
                this@AgendarCitaActivity,
                android.R.layout.simple_spinner_dropdown_item,
                mascotas.map { "${it.nombre} (${it.especie})" }
            )
            binding.spVeterinario.adapter = ArrayAdapter(
                this@AgendarCitaActivity,
                android.R.layout.simple_spinner_dropdown_item,
                veterinarios.map { it.nombreCompleto }
            )

            if (mascotas.isEmpty() || veterinarios.isEmpty()) {
                binding.tvError.text = "Se necesita al menos una mascota activa y un veterinario activo."
                binding.tvError.visibility = View.VISIBLE
                ocupado(false)
                binding.btnGuardar.isEnabled = false
                return@launch
            }

            if (id != null) {
                when (val c = repo.cita(id)) {
                    is ApiResult.Exito -> {
                        citaActual = c.datos
                        llenarFormulario(c.datos)
                    }
                    is ApiResult.Falla -> {
                        ocupado(false)
                        mostrarFalla(c)
                        return@launch
                    }
                }
            }

            // Un VETERINARIO solo agenda a su nombre: se deja fijo en su propio perfil
            if (!repo.session.esAdmin()) {
                val miId = repo.session.idVeterinario()
                val idElegido = citaActual?.idVeterinario ?: miId
                val pos = veterinarios.indexOfFirst { it.id == idElegido }
                if (pos >= 0) binding.spVeterinario.setSelection(pos)
                binding.spVeterinario.isEnabled = false
            }
            ocupado(false)
        }
    }

    private fun llenarFormulario(c: Cita) {
        val posMascota = mascotas.indexOfFirst { it.id == c.idMascota }
        if (posMascota >= 0) binding.spMascota.setSelection(posMascota)
        binding.spMascota.isEnabled = false          // al reprogramar no se cambia la mascota

        val posVet = veterinarios.indexOfFirst { it.id == c.idVeterinario }
        if (posVet >= 0) binding.spVeterinario.setSelection(posVet)

        fechaIso = c.fechaHora
        binding.etFecha.setText(formatoFecha(c.fechaHora))
        binding.etDuracion.setText((c.duracionMinutos ?: 30).toString())
        binding.etMotivo.setText(c.motivo ?: "")
        binding.etObservaciones.setText(c.observaciones ?: "")
    }

    // ----------------------------------------------------------- fecha y hora

    private fun elegirFechaHora() {
        val cal = Calendar.getInstance()
        var anio = cal.get(Calendar.YEAR)
        var mes = cal.get(Calendar.MONTH)
        var dia = cal.get(Calendar.DAY_OF_MONTH)

        // Si ya hay una fecha elegida, el selector abre en ella
        val partes = fechaIso?.take(10)?.split("-")
        if (partes != null && partes.size == 3) {
            anio = partes[0].toIntOrNull() ?: anio
            mes = (partes[1].toIntOrNull() ?: (mes + 1)) - 1
            dia = partes[2].toIntOrNull() ?: dia
        }

        val dialogo = DatePickerDialog(this, { _, a, m, d ->
            // Después de la fecha se pide la hora
            TimePickerDialog(this, { _, hora, minuto ->
                fechaIso = String.format(Locale.US, "%04d-%02d-%02dT%02d:%02d:00", a, m + 1, d, hora, minuto)
                binding.etFecha.setText(formatoFecha(fechaIso))
            }, 9, 0, true).show()
        }, anio, mes, dia)
        dialogo.datePicker.minDate = System.currentTimeMillis() - 1000   // no se permite el pasado
        dialogo.show()
    }

    /** ¿La fecha elegida es posterior al momento actual? (R1) */
    private fun esFutura(iso: String): Boolean {
        return try {
            val fecha = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(iso)
            fecha != null && fecha.time > System.currentTimeMillis()
        } catch (e: Exception) {
            false
        }
    }

    // --------------------------------------------------------------- guardar

    private fun guardar() {
        limpiarErrores()

        val motivo = binding.etMotivo.text.toString().trim()
        val observaciones = binding.etObservaciones.text.toString().trim()
        val duracionTexto = binding.etDuracion.text.toString().trim()
        val duracion = if (duracionTexto.isEmpty()) 30 else duracionTexto.toIntOrNull()
        val fecha = fechaIso

        val idMascota = citaActual?.idMascota
            ?: mascotas.getOrNull(binding.spMascota.selectedItemPosition)?.id
        val idVeterinario = veterinarios.getOrNull(binding.spVeterinario.selectedItemPosition)?.id

        var valido = true
        if (idMascota == null || idVeterinario == null) {
            binding.tvError.text = "Seleccione la mascota y el veterinario"
            binding.tvError.visibility = View.VISIBLE
            valido = false
        }
        if (fecha == null) {
            binding.tilFecha.error = "Elija la fecha y la hora"
            valido = false
        } else if (!esFutura(fecha)) {
            binding.tilFecha.error = "La cita debe ser en una fecha y hora futuras"
            valido = false
        }
        if (duracion == null || duracion < 10 || duracion > 240) {
            binding.tilDuracion.error = "Entre 10 y 240 minutos"
            valido = false
        }
        if (motivo.isEmpty()) {
            binding.tilMotivo.error = "Ingrese el motivo"
            valido = false
        }
        if (!valido || idMascota == null || idVeterinario == null || fecha == null || duracion == null) return

        val cita = Cita(
            idCita = citaActual?.idCita,
            idMascota = idMascota,
            idVeterinario = idVeterinario,
            fechaHora = fecha,
            duracionMinutos = duracion,
            motivo = motivo,
            observaciones = observaciones.ifEmpty { null }
        )

        ocupado(true)
        lifecycleScope.launch {
            when (val r = repo.guardarCita(cita)) {
                is ApiResult.Exito -> {
                    Toast.makeText(this@AgendarCitaActivity, "Cita guardada", Toast.LENGTH_SHORT).show()
                    finish()
                }
                is ApiResult.Falla -> {
                    ocupado(false)
                    if (r.sesionInvalida || r.sinPermiso || r.sinConexion) {
                        mostrarFalla(r)
                    } else {
                        // 400 validación, 404, 409 cruce de horario o mascota/veterinario inactivo
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
        binding.tilFecha.error = null
        binding.tilDuracion.error = null
        binding.tilMotivo.error = null
        binding.tvError.visibility = View.GONE
    }

    companion object {
        const val EXTRA_ID = "id_cita"
    }
}