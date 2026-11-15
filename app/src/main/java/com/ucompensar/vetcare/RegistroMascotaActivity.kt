package com.ucompensar.vetcare

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.ucompensar.vetcare.api.ApiResult
import com.ucompensar.vetcare.databinding.ActivityRegistroMascotaBinding
import com.ucompensar.vetcare.models.Mascota
import com.ucompensar.vetcare.models.Propietario
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

/**
 * Crea una mascota (POST) o edita una existente (PUT, cuando llega EXTRA_ID).
 * Las reglas coinciden con las del servidor: nombre, especie y propietario son obligatorios,
 * el peso debe ser mayor que 0 y la fecha no puede ser futura.
 */
class RegistroMascotaActivity : BaseActivity() {

    private lateinit var binding: ActivityRegistroMascotaBinding
    private var propietarios: List<Propietario> = emptyList()
    private var mascotaActual: Mascota? = null            // null = mascota nueva
    private val sexos = listOf("Sin especificar", "Macho", "Hembra")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroMascotaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ajustarBarras(binding.raiz)

        binding.spSexo.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sexos)

        binding.etFecha.setOnClickListener { elegirFecha() }
        binding.btnGuardar.setOnClickListener { guardar() }
        binding.btnCancelar.setOnClickListener { finish() }

        val id = intent.getLongExtra(EXTRA_ID, -1L).takeIf { it > 0 }
        binding.tvTitulo.text = if (id == null) "Nueva mascota" else "Editar mascota"
        cargarInicial(id)
    }

    // ------------------------------------------------------- carga de datos

    /**
     * Trae los propietarios (para el selector) y, si se edita, la mascota.
     * Solo se ofrecen propietarios activos, más el dueño actual de la mascota que se edita
     * (aunque esté inactivo, para que no se le cambie el dueño sin querer).
     */
    private fun cargarInicial(id: Long?) {
        ocupado(true)
        lifecycleScope.launch {
            val todos = when (val p = repo.propietarios(estado = "TODOS")) {
                is ApiResult.Exito -> p.datos
                is ApiResult.Falla -> {
                    ocupado(false)
                    mostrarFalla(p)
                    return@launch
                }
            }

            var existente: Mascota? = null
            if (id != null) {
                when (val m = repo.mascota(id)) {
                    is ApiResult.Exito -> existente = m.datos
                    is ApiResult.Falla -> {
                        ocupado(false)
                        mostrarFalla(m)
                        return@launch
                    }
                }
            }

            propietarios = todos.filter { it.estado != "INACTIVO" || it.id == existente?.idPropietario }
            binding.spPropietario.adapter = ArrayAdapter(
                this@RegistroMascotaActivity,
                android.R.layout.simple_spinner_dropdown_item,
                propietarios.map {
                    val nota = if (it.estado == "INACTIVO") " – inactivo" else ""
                    "${it.nombreCompleto} (${it.numeroDocumento})$nota"
                }
            )

            if (propietarios.isEmpty()) {
                binding.tvError.text = "No hay propietarios activos. Registre o reactive uno primero."
                binding.tvError.visibility = View.VISIBLE
                ocupado(false)
                binding.btnGuardar.isEnabled = false
                return@launch
            }

            if (existente != null) {
                mascotaActual = existente
                llenarFormulario(existente)
            }
            ocupado(false)
        }
    }

    private fun llenarFormulario(m: Mascota) {
        binding.etNombre.setText(m.nombre)
        binding.etEspecie.setText(m.especie)
        binding.etRaza.setText(m.raza ?: "")
        binding.etFecha.setText(m.fechaNacimiento ?: "")
        binding.etPeso.setText(m.pesoActual?.toString() ?: "")
        binding.etColor.setText(m.color ?: "")
        binding.spSexo.setSelection(
            when (m.sexo) {
                "M" -> 1
                "H" -> 2
                else -> 0
            }
        )
        val pos = propietarios.indexOfFirst { it.id == m.idPropietario }
        binding.spPropietario.setSelection(if (pos >= 0) pos else 0)
    }

    // ---------------------------------------------------------------- fecha

    private fun elegirFecha() {
        val cal = Calendar.getInstance()
        var anio = cal.get(Calendar.YEAR)
        var mes = cal.get(Calendar.MONTH)
        var dia = cal.get(Calendar.DAY_OF_MONTH)

        // Si ya hay una fecha escrita (AAAA-MM-DD), el selector abre en ella
        val partes = binding.etFecha.text.toString().split("-")
        if (partes.size == 3) {
            anio = partes[0].toIntOrNull() ?: anio
            mes = (partes[1].toIntOrNull() ?: (mes + 1)) - 1
            dia = partes[2].toIntOrNull() ?: dia
        }

        val dialogo = DatePickerDialog(this, { _, a, m, d ->
            binding.etFecha.setText(String.format(Locale.US, "%04d-%02d-%02d", a, m + 1, d))
        }, anio, mes, dia)
        dialogo.datePicker.maxDate = System.currentTimeMillis()   // no se permite fecha futura
        dialogo.show()
    }

    // --------------------------------------------------------------- guardar

    private fun guardar() {
        limpiarErrores()

        val nombre = binding.etNombre.text.toString().trim()
        val especie = binding.etEspecie.text.toString().trim()
        val raza = binding.etRaza.text.toString().trim()
        val color = binding.etColor.text.toString().trim()
        val fecha = binding.etFecha.text.toString().trim()
        val pesoTexto = binding.etPeso.text.toString().trim().replace(',', '.')
        val peso = pesoTexto.toDoubleOrNull()
        val idPropietario = propietarios.getOrNull(binding.spPropietario.selectedItemPosition)?.id

        var valido = true
        if (idPropietario == null) {
            binding.tvError.text = "Seleccione un propietario"
            binding.tvError.visibility = View.VISIBLE
            valido = false
        }
        if (nombre.isEmpty()) {
            binding.tilNombre.error = "Ingrese el nombre"
            valido = false
        }
        if (especie.isEmpty()) {
            binding.tilEspecie.error = "Ingrese la especie"
            valido = false
        }
        if (pesoTexto.isNotEmpty() && (peso == null || peso <= 0.0)) {
            binding.tilPeso.error = "El peso debe ser un número mayor que 0"
            valido = false
        }
        if (!valido || idPropietario == null) return

        val mascota = Mascota(
            id = mascotaActual?.id,
            nombre = nombre,
            especie = especie,
            raza = raza.ifEmpty { null },
            fechaNacimiento = fecha.ifEmpty { null },
            sexo = when (binding.spSexo.selectedItemPosition) {
                1 -> "M"
                2 -> "H"
                else -> null
            },
            color = color.ifEmpty { null },
            pesoActual = peso,
            estado = mascotaActual?.estado,
            idPropietario = idPropietario
        )

        ocupado(true)
        lifecycleScope.launch {
            when (val r = repo.guardarMascota(mascota)) {
                is ApiResult.Exito -> {
                    Toast.makeText(this@RegistroMascotaActivity, "Mascota guardada", Toast.LENGTH_SHORT).show()
                    finish()
                }
                is ApiResult.Falla -> {
                    ocupado(false)
                    if (r.sesionInvalida || r.sinPermiso || r.sinConexion) {
                        mostrarFalla(r)
                    } else {
                        // 400 validación del servidor, 404 propietario inexistente, etc.
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
        binding.tilNombre.error = null
        binding.tilEspecie.error = null
        binding.tilPeso.error = null
        binding.tvError.visibility = View.GONE
    }

    companion object {
        const val EXTRA_ID = "id_mascota"
    }
}