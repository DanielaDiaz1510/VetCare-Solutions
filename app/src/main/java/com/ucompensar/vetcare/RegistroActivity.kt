package com.ucompensar.vetcare

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.lifecycle.lifecycleScope
import com.ucompensar.vetcare.api.ApiResult
import com.ucompensar.vetcare.databinding.ActivityRegistroBinding
import com.ucompensar.vetcare.models.RegistroRequest
import kotlinx.coroutines.launch

/**
 * Registro de una cuenta nueva (RF-01). La persona elige si quiere ser Veterinario o Administrador.
 *  - Veterinario: además de la cuenta se piden los datos de su perfil clínico (documento, nombres...).
 *  - Administrador: solo los datos de la cuenta.
 * En ambos casos la solicitud queda PENDIENTE: un administrador la aprueba desde la app y, si era de
 * veterinario, su perfil queda listo para que se le agenden citas.
 */
class RegistroActivity : BaseActivity() {

    private lateinit var binding: ActivityRegistroBinding

    private val roles = listOf("Veterinario", "Administrador")
    private val valoresRol = listOf("VETERINARIO", "ADMIN")
    private val tipos = listOf("CC", "CE", "PAS", "TI")

    private val esVeterinario: Boolean get() = binding.spRol.selectedItemPosition == 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ajustarBarras(binding.raiz)

        binding.spRol.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, roles)
        binding.spTipoDocumento.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, tipos)

        // Según el rol se muestran unos campos u otros
        binding.spRol.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                mostrarCamposDelRol()
            }

            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        mostrarCamposDelRol()

        binding.btnRegistrar.setOnClickListener { intentarRegistro() }
        binding.tvVolver.setOnClickListener { finish() }
    }

    private fun mostrarCamposDelRol() {
        binding.bloqueVet.visibility = if (esVeterinario) View.VISIBLE else View.GONE
        binding.tilNombre.visibility = if (esVeterinario) View.GONE else View.VISIBLE
    }

    /** Mismas reglas que el backend (Validador.java), para avisar antes de gastar una llamada. */
    private fun intentarRegistro() {
        limpiarErrores()

        val usuario = binding.etUsuario.text.toString().trim()
        val correo = binding.etCorreo.text.toString().trim()
        val clave = binding.etClave.text.toString()
        val clave2 = binding.etClave2.text.toString()

        val documento = binding.etDocumento.text.toString().trim()
        val nombres = binding.etNombres.text.toString().trim()
        val apellidos = binding.etApellidos.text.toString().trim()
        val especialidad = binding.etEspecialidad.text.toString().trim()
        val telefono = binding.etTelefono.text.toString().trim()
        val nombreAdmin = binding.etNombre.text.toString().trim()

        var valido = true
        if (esVeterinario) {
            if (!Regex("^[A-Za-z0-9-]{4,30}$").matches(documento)) {
                binding.tilDocumento.error = "De 4 a 30 letras, números o guiones"
                valido = false
            }
            if (nombres.isEmpty()) {
                binding.tilNombres.error = "Ingrese sus nombres"
                valido = false
            }
            if (apellidos.isEmpty()) {
                binding.tilApellidos.error = "Ingrese sus apellidos"
                valido = false
            }
            if (telefono.isNotEmpty() && !Regex("^[0-9+()\\-\\s]{7,20}$").matches(telefono)) {
                binding.tilTelefono.error = "De 7 a 20 caracteres: números, espacios, + - ( )"
                valido = false
            }
        } else if (nombreAdmin.isEmpty()) {
            binding.tilNombre.error = "Ingrese su nombre"
            valido = false
        }
        if (!Regex("^[A-Za-z0-9._-]{3,50}$").matches(usuario)) {
            binding.tilUsuario.error = "3 a 50 caracteres: letras, números, punto, guion o guion bajo"
            valido = false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            binding.tilCorreo.error = "Correo no válido"
            valido = false
        }
        if (clave.length !in 8..72 ||
            clave.none { it.isLetter() } || clave.none { it.isDigit() }
        ) {
            binding.tilClave.error = "Mínimo 8 caracteres, con al menos una letra y un número"
            valido = false
        }
        if (clave != clave2) {
            binding.tilClave2.error = "Las contraseñas no coinciden"
            valido = false
        }
        if (!valido) return

        val pedido = if (esVeterinario) {
            RegistroRequest(
                username = usuario,
                password = clave,
                nombre = "$nombres $apellidos",
                email = correo,
                rolSolicitado = valoresRol[0],
                tipoDocumento = tipos[binding.spTipoDocumento.selectedItemPosition],
                numeroDocumento = documento,
                nombres = nombres,
                apellidos = apellidos,
                especialidad = especialidad.ifEmpty { null },
                telefono = telefono.ifEmpty { null }
            )
        } else {
            RegistroRequest(
                username = usuario,
                password = clave,
                nombre = nombreAdmin,
                email = correo,
                rolSolicitado = valoresRol[1]
            )
        }
        enviar(pedido)
    }

    private fun enviar(pedido: RegistroRequest) {
        ocupado(true)
        lifecycleScope.launch {
            val resultado = repo.registrar(pedido)
            ocupado(false)
            when (resultado) {
                is ApiResult.Exito -> {
                    irALogin("Solicitud recibida. Un administrador debe aprobar su cuenta antes de ingresar.")
                }
                is ApiResult.Falla -> {
                    binding.tvError.text = resultado.mensaje
                    binding.tvError.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun ocupado(si: Boolean) {
        binding.progreso.visibility = if (si) View.VISIBLE else View.GONE
        binding.btnRegistrar.isEnabled = !si
    }

    private fun limpiarErrores() {
        binding.tilNombre.error = null
        binding.tilDocumento.error = null
        binding.tilNombres.error = null
        binding.tilApellidos.error = null
        binding.tilTelefono.error = null
        binding.tilUsuario.error = null
        binding.tilCorreo.error = null
        binding.tilClave.error = null
        binding.tilClave2.error = null
        binding.tvError.visibility = View.GONE
    }
}
