package com.ucompensar.vetcare

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.ucompensar.vetcare.api.ApiResult
import com.ucompensar.vetcare.databinding.ActivityRegistroHistoriaBinding
import com.ucompensar.vetcare.models.HistorialClinico
import com.ucompensar.vetcare.models.Mascota
import com.ucompensar.vetcare.models.Veterinario
import kotlinx.coroutines.launch

/**
 * Registra una atención clínica (POST) o edita una existente (PUT, cuando llega EXTRA_JSON).
 * Al crear: el ADMIN elige el veterinario; el VETERINARIO queda registrado él mismo.
 * Al editar no cambian la mascota ni el veterinario, y se conserva la fecha original.
 */
class RegistroHistoriaActivity : BaseActivity() {

    private lateinit var binding: ActivityRegistroHistoriaBinding
    private var mascotas: List<Mascota> = emptyList()
    private var veterinarios: List<Veterinario> = emptyList()
    private var actual: HistorialClinico? = null          // null = atención nueva

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroHistoriaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ajustarBarras(binding.raiz)

        binding.btnGuardar.setOnClickListener { guardar() }
        binding.btnCancelar.setOnClickListener { finish() }

        val json = intent.getStringExtra(EXTRA_JSON)
        actual = json?.let { Gson().fromJson(it, HistorialClinico::class.java) }

        val editando = actual
        if (editando != null) {
            binding.tvTitulo.text = "Editar atención"
            binding.bloqueMascota.visibility = View.GONE
            binding.bloqueVeterinario.visibility = View.GONE
            binding.tvMascotaFija.visibility = View.VISIBLE
            binding.tvMascotaFija.text = "Mascota: ${editando.nombreMascota ?: "—"}"
            llenarFormulario(editando)
        } else {
            binding.tvTitulo.text = "Nueva atención"
            if (!repo.session.esAdmin()) {
                // El VETERINARIO queda registrado él mismo: no elige veterinario
                binding.bloqueVeterinario.visibility = View.GONE
            }
            cargarListas()
        }
    }

    private fun llenarFormulario(h: HistorialClinico) {
        binding.etMotivo.setText(h.motivoConsulta)
        binding.etDiagnostico.setText(h.diagnostico ?: "")
        binding.etObservaciones.setText(h.observaciones ?: "")
        binding.etPeso.setText(h.peso?.toString() ?: "")
        binding.etTemperatura.setText(h.temperatura?.toString() ?: "")
    }

    /** Solo al crear: trae las mascotas activas y, si es ADMIN, los veterinarios activos. */
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
            binding.spMascota.adapter = ArrayAdapter(
                this@RegistroHistoriaActivity,
                android.R.layout.simple_spinner_dropdown_item,
                mascotas.map { "${it.nombre} (${it.especie})" }
            )

            if (repo.session.esAdmin()) {
                when (val v = repo.veterinarios(estado = "ACTIVO")) {
                    is ApiResult.Exito -> veterinarios = v.datos
                    is ApiResult.Falla -> {
                        ocupado(false)
                        mostrarFalla(v)
                        return@launch
                    }
                }
                binding.spVeterinario.adapter = ArrayAdapter(
                    this@RegistroHistoriaActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    veterinarios.map { it.nombreCompleto }
                )
            }

            ocupado(false)
            if (mascotas.isEmpty() || (repo.session.esAdmin() && veterinarios.isEmpty())) {
                binding.tvError.text = "Se necesita al menos una mascota activa" +
                        if (repo.session.esAdmin()) " y un veterinario activo." else "."
                binding.tvError.visibility = View.VISIBLE
                binding.btnGuardar.isEnabled = false
            }
        }
    }

    // --------------------------------------------------------------- guardar

    private fun guardar() {
        limpiarErrores()
        val esAdmin = repo.session.esAdmin()

        val motivo = binding.etMotivo.text.toString().trim()
        val diagnostico = binding.etDiagnostico.text.toString().trim()
        val observaciones = binding.etObservaciones.text.toString().trim()
        val pesoTexto = binding.etPeso.text.toString().trim().replace(',', '.')
        val tempTexto = binding.etTemperatura.text.toString().trim().replace(',', '.')
        val peso = pesoTexto.toDoubleOrNull()
        val temperatura = tempTexto.toDoubleOrNull()

        val idMascota = actual?.idMascota
            ?: mascotas.getOrNull(binding.spMascota.selectedItemPosition)?.id
        val idVeterinario: Long? = when {
            actual != null -> actual?.idVeterinario
            esAdmin -> veterinarios.getOrNull(binding.spVeterinario.selectedItemPosition)?.id
            else -> null                                   // lo asigna el servidor
        }

        var valido = true
        if (idMascota == null) {
            binding.tvError.text = "Seleccione la mascota"
            binding.tvError.visibility = View.VISIBLE
            valido = false
        }
        if (actual == null && esAdmin && idVeterinario == null) {
            binding.tvError.text = "Seleccione el veterinario"
            binding.tvError.visibility = View.VISIBLE
            valido = false
        }
        if (motivo.isEmpty()) {
            binding.tilMotivo.error = "Ingrese el motivo de la consulta"
            valido = false
        }
        if (pesoTexto.isNotEmpty() && (peso == null || peso <= 0.0 || peso > 500.0)) {
            binding.tilPeso.error = "El peso debe estar entre 0 y 500 kg"
            valido = false
        }
        if (tempTexto.isNotEmpty() && (temperatura == null || temperatura < 30.0 || temperatura > 45.0)) {
            binding.tilTemperatura.error = "La temperatura debe estar entre 30 y 45 °C"
            valido = false
        }
        if (!valido || idMascota == null) return

        val historia = HistorialClinico(
            idHistorial = actual?.idHistorial,
            fechaAtencion = actual?.fechaAtencion,         // al editar se conserva la fecha original
            motivoConsulta = motivo,
            diagnostico = diagnostico.ifEmpty { null },
            observaciones = observaciones.ifEmpty { null },
            peso = peso,
            temperatura = temperatura,
            idMascota = idMascota,
            idVeterinario = idVeterinario
        )

        ocupado(true)
        lifecycleScope.launch {
            when (val r = repo.guardarHistorial(historia)) {
                is ApiResult.Exito -> {
                    Toast.makeText(this@RegistroHistoriaActivity, "Atención guardada", Toast.LENGTH_SHORT).show()
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
        binding.tilMotivo.error = null
        binding.tilPeso.error = null
        binding.tilTemperatura.error = null
        binding.tvError.visibility = View.GONE
    }

    companion object {
        const val EXTRA_JSON = "historia_json"
    }
}