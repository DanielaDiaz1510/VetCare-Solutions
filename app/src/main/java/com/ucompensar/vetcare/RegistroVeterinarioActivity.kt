package com.ucompensar.vetcare

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.ucompensar.vetcare.api.ApiResult
import com.ucompensar.vetcare.databinding.ActivityRegistroVeterinarioBinding
import com.ucompensar.vetcare.models.Usuario
import com.ucompensar.vetcare.models.Veterinario
import kotlinx.coroutines.launch

/**
 * Crea un veterinario (POST) o edita uno existente (PUT, cuando llega EXTRA_ID). Solo ADMIN.
 *
 * La "cuenta de acceso" es el usuario con el que el veterinario inicia sesión. Solo se ofrecen
 * cuentas con rol VETERINARIO que todavía no estén ligadas a otro veterinario (el servidor
 * también lo valida). Al editar se envía siempre la cuenta elegida, así no se desvincula por accidente.
 *
 * Desde la pantalla de Usuarios llega con la cuenta, el nombre y el correo ya precargados.
 */
class RegistroVeterinarioActivity : BaseActivity() {

    private lateinit var binding: ActivityRegistroVeterinarioBinding
    private val tipos = listOf("CC", "CE", "PAS", "TI")
    private val estados = listOf("ACTIVO", "INACTIVO")
    private var actual: Veterinario? = null              // null = veterinario nuevo
    private var cuentas: List<Usuario> = emptyList()     // posición 0 del spinner = "Sin cuenta"

    private val regexDocumento = Regex("^[A-Za-z0-9-]{4,30}$")
    private val regexTelefono = Regex("^[0-9+()\\-\\s]{7,20}$")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroVeterinarioBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ajustarBarras(binding.raiz)

        if (!repo.session.esAdmin()) {
            Toast.makeText(this, "Solo el administrador puede gestionar veterinarios", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        binding.spTipoDocumento.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, tipos)
        binding.spEstado.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, estados)

        binding.btnGuardar.setOnClickListener { guardar() }
        binding.btnCancelar.setOnClickListener { finish() }

        val id = intent.getLongExtra(EXTRA_ID, -1L).takeIf { it > 0 }
        binding.tvTitulo.text = if (id == null) "Nuevo veterinario" else "Editar veterinario"
        binding.bloqueEstado.visibility = if (id == null) View.GONE else View.VISIBLE

        if (id == null) {
            // Alta: si viene desde Usuarios, se precargan los datos de la cuenta
            binding.etNombres.setText(intent.getStringExtra(EXTRA_NOMBRE) ?: "")
            binding.etEmail.setText(intent.getStringExtra(EXTRA_EMAIL) ?: "")
            val idUsuario = intent.getLongExtra(EXTRA_ID_USUARIO, -1L).takeIf { it > 0 }
            cargarCuentas(idUsuario)
        } else {
            cargar(id)
        }
    }

    // --------------------------------------------------------------- carga

    private fun cargar(id: Long) {
        ocupado(true)
        lifecycleScope.launch {
            when (val r = repo.veterinario(id)) {
                is ApiResult.Exito -> {
                    actual = r.datos
                    llenar(r.datos)
                    cargarCuentas(r.datos.idUsuario)
                }
                is ApiResult.Falla -> {
                    ocupado(false)
                    mostrarFalla(r)
                }
            }
        }
    }

    private fun llenar(v: Veterinario) {
        val pos = tipos.indexOf(v.tipoDocumento)
        binding.spTipoDocumento.setSelection(if (pos >= 0) pos else 0)
        binding.etDocumento.setText(v.numeroDocumento)
        binding.etNombres.setText(v.nombres)
        binding.etApellidos.setText(v.apellidos)
        binding.etEspecialidad.setText(v.especialidad ?: "")
        binding.etTelefono.setText(v.telefono ?: "")
        binding.etEmail.setText(v.email ?: "")
        binding.spEstado.setSelection(if (v.estado == "INACTIVO") 1 else 0)
    }

    /**
     * Arma la lista de cuentas que se pueden elegir: usuarios con rol VETERINARIO que no tengan
     * ya otro veterinario (o la que ya tiene este mismo). [seleccionar] es la que queda marcada.
     */
    private fun cargarCuentas(seleccionar: Long?) {
        ocupado(true)
        lifecycleScope.launch {
            val usuarios = when (val r = repo.usuarios()) {
                is ApiResult.Exito -> r.datos
                is ApiResult.Falla -> {
                    ocupado(false)
                    mostrarFalla(r)
                    return@launch
                }
            }
            val ocupadas = when (val r = repo.veterinarios(estado = "TODOS")) {
                is ApiResult.Exito -> r.datos
                    .filter { it.id != actual?.id }          // la cuenta de este mismo veterinario sí vale
                    .mapNotNull { it.idUsuario }
                    .toSet()
                is ApiResult.Falla -> emptySet()
            }
            cuentas = usuarios.filter { it.esVeterinario && !ocupadas.contains(it.idUsuario) }

            val nombres = listOf("Sin cuenta de acceso") + cuentas.map {
                val nota = if (it.activo) "" else " – inactiva"
                "${it.nombre} (${it.username})$nota"
            }
            binding.spCuenta.adapter =
                ArrayAdapter(this@RegistroVeterinarioActivity, android.R.layout.simple_spinner_dropdown_item, nombres)
            val pos = cuentas.indexOfFirst { it.idUsuario == seleccionar }
            binding.spCuenta.setSelection(if (pos >= 0) pos + 1 else 0)
            ocupado(false)
        }
    }

    // --------------------------------------------------------------- guardar

    private fun guardar() {
        limpiarErrores()

        val documento = binding.etDocumento.text.toString().trim()
        val nombres = binding.etNombres.text.toString().trim()
        val apellidos = binding.etApellidos.text.toString().trim()
        val especialidad = binding.etEspecialidad.text.toString().trim()
        val telefono = binding.etTelefono.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()

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

        val posCuenta = binding.spCuenta.selectedItemPosition
        val cuenta: Usuario? = if (posCuenta <= 0) null else cuentas.getOrNull(posCuenta - 1)

        val veterinario = Veterinario(
            id = actual?.id,
            tipoDocumento = tipos[binding.spTipoDocumento.selectedItemPosition],
            numeroDocumento = documento,
            nombres = nombres,
            apellidos = apellidos,
            especialidad = especialidad.ifEmpty { null },
            telefono = telefono.ifEmpty { null },
            email = email.ifEmpty { null },
            estado = if (actual == null) "ACTIVO" else estados[binding.spEstado.selectedItemPosition],
            idUsuario = cuenta?.idUsuario
        )

        ocupado(true)
        lifecycleScope.launch {
            when (val r = repo.guardarVeterinario(veterinario)) {
                is ApiResult.Exito -> {
                    Toast.makeText(this@RegistroVeterinarioActivity, "Veterinario guardado", Toast.LENGTH_SHORT).show()
                    // Si la cuenta elegida aún no está activa, se ofrece activarla de una vez
                    if (cuenta != null && !cuenta.activo) ofrecerActivar(cuenta) else finish()
                }
                is ApiResult.Falla -> {
                    ocupado(false)
                    if (r.sesionInvalida || r.sinPermiso || r.sinConexion) {
                        mostrarFalla(r)
                    } else {
                        // 400 validación, 409 documento repetido o cuenta ya ligada, etc.
                        binding.tvError.text = r.mensaje
                        binding.tvError.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun ofrecerActivar(cuenta: Usuario) {
        AlertDialog.Builder(this)
            .setTitle("Cuenta inactiva")
            .setMessage("La cuenta ${cuenta.username} todavía no puede iniciar sesión. ¿Activarla ahora?")
            .setCancelable(false)
            .setPositiveButton("Activar") { _, _ ->
                lifecycleScope.launch {
                    when (val r = repo.cambiarEstadoUsuario(cuenta, true)) {
                        is ApiResult.Exito ->
                            Toast.makeText(this@RegistroVeterinarioActivity, "Cuenta activada", Toast.LENGTH_SHORT).show()
                        is ApiResult.Falla -> mostrarFalla(r)
                    }
                    finish()
                }
            }
            .setNegativeButton("Más tarde") { _, _ -> finish() }
            .show()
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
        const val EXTRA_ID = "id_veterinario"
        const val EXTRA_ID_USUARIO = "id_usuario"
        const val EXTRA_NOMBRE = "nombre_usuario"
        const val EXTRA_EMAIL = "email_usuario"
    }
}
