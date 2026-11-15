package com.ucompensar.vetcare

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.ucompensar.vetcare.api.ApiResult
import com.ucompensar.vetcare.databinding.ActivityRegistroPropietarioBinding
import com.ucompensar.vetcare.models.Propietario
import kotlinx.coroutines.launch

/**
 * Crea un propietario (POST) o edita uno existente (PUT, cuando llega EXTRA_ID).
 * Las reglas coinciden con las del servidor: tipo y número de documento, nombres y apellidos
 * obligatorios; teléfono y correo opcionales pero con formato válido.
 */
class RegistroPropietarioActivity : BaseActivity() {

    private lateinit var binding: ActivityRegistroPropietarioBinding
    private val tipos = listOf("CC", "TI", "CE", "PAS", "NIT")
    private var actual: Propietario? = null              // null = propietario nuevo

    private val regexDocumento = Regex("^[A-Za-z0-9-]{4,30}$")
    private val regexTelefono = Regex("^[0-9+()\\-\\s]{7,20}$")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroPropietarioBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ajustarBarras(binding.raiz)

        binding.spTipoDocumento.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, tipos)

        binding.btnGuardar.setOnClickListener { guardar() }
        binding.btnCancelar.setOnClickListener { finish() }

        val id = intent.getLongExtra(EXTRA_ID, -1L).takeIf { it > 0 }
        binding.tvTitulo.text = if (id == null) "Nuevo propietario" else "Editar propietario"
        if (id != null) cargar(id)
    }

    private fun cargar(id: Long) {
        ocupado(true)
        lifecycleScope.launch {
            when (val r = repo.propietario(id)) {
                is ApiResult.Exito -> {
                    actual = r.datos
                    llenar(r.datos)
                    ocupado(false)
                }
                is ApiResult.Falla -> {
                    ocupado(false)
                    mostrarFalla(r)
                }
            }
        }
    }

    private fun llenar(p: Propietario) {
        val pos = tipos.indexOf(p.tipoDocumento)
        binding.spTipoDocumento.setSelection(if (pos >= 0) pos else 0)
        binding.etDocumento.setText(p.numeroDocumento)
        binding.etNombres.setText(p.nombres)
        binding.etApellidos.setText(p.apellidos)
        binding.etTelefono.setText(p.telefono ?: "")
        binding.etEmail.setText(p.email ?: "")
        binding.etDireccion.setText(p.direccion ?: "")
    }

    private fun guardar() {
        limpiarErrores()

        val documento = binding.etDocumento.text.toString().trim()
        val nombres = binding.etNombres.text.toString().trim()
        val apellidos = binding.etApellidos.text.toString().trim()
        val telefono = binding.etTelefono.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val direccion = binding.etDireccion.text.toString().trim()

        var valido = true
        if (!regexDocumento.matches(documento)) {
            binding.tilDocumento.error = "De 4 a 30 letras, números o guiones"
            valido = false
        }
        if (nombres.isEmpty()) {
            binding.tilNombres.error = "Ingrese los nombres"
            valido = false
        }
        if (apellidos.isEmpty()) {
            binding.tilApellidos.error = "Ingrese los apellidos"
            valido = false
        }
        if (telefono.isNotEmpty() && !regexTelefono.matches(telefono)) {
            binding.tilTelefono.error = "De 7 a 20 caracteres: números, espacios, + - ( )"
            valido = false
        }
        if (email.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Correo no válido"
            valido = false
        }
        if (!valido) return

        val propietario = Propietario(
            id = actual?.id,
            tipoDocumento = tipos[binding.spTipoDocumento.selectedItemPosition],
            numeroDocumento = documento,
            nombres = nombres,
            apellidos = apellidos,
            telefono = telefono.ifEmpty { null },
            email = email.ifEmpty { null },
            direccion = direccion.ifEmpty { null },
            estado = actual?.estado            // al editar se conserva; el estado se cambia desde la lista
        )

        ocupado(true)
        lifecycleScope.launch {
            when (val r = repo.guardarPropietario(propietario)) {
                is ApiResult.Exito -> {
                    Toast.makeText(this@RegistroPropietarioActivity, "Propietario guardado", Toast.LENGTH_SHORT).show()
                    finish()
                }
                is ApiResult.Falla -> {
                    ocupado(false)
                    if (r.sesionInvalida || r.sinPermiso || r.sinConexion) {
                        mostrarFalla(r)
                    } else {
                        // 400 validación del servidor, 409 documento repetido, etc.
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
        binding.tilDocumento.error = null
        binding.tilNombres.error = null
        binding.tilApellidos.error = null
        binding.tilTelefono.error = null
        binding.tilEmail.error = null
        binding.tvError.visibility = View.GONE
    }

    companion object {
        const val EXTRA_ID = "id_propietario"
    }
}