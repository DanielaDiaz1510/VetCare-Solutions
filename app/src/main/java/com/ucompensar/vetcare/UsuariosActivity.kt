package com.ucompensar.vetcare

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ucompensar.vetcare.api.ApiResult
import com.ucompensar.vetcare.databinding.ActivityUsuariosBinding
import com.ucompensar.vetcare.models.Usuario
import com.ucompensar.vetcare.models.Veterinario
import kotlinx.coroutines.launch

/**
 * Administración de cuentas (solo ADMIN): ver quién se registró, aprobar (activar) o desactivar su acceso.
 * Si la persona se registró como veterinario, su perfil clínico ya viene creado: al aprobar la cuenta
 * el servidor lo activa y queda disponible para agendar citas. Si una cuenta de veterinario no tiene perfil
 * (por ejemplo, la creó otro admin), aquí mismo se puede registrar.
 */
class UsuariosActivity : BaseActivity() {

    private lateinit var binding: ActivityUsuariosBinding
    private lateinit var adaptador: UsuarioAdapter

    private val estados = listOf("Todos", "Pendientes o desactivados", "Activos")
    private var todos: List<Usuario> = emptyList()
    private var perfiles: Map<Long, Veterinario> = emptyMap()   // idUsuario -> su perfil de veterinario

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsuariosBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ajustarBarras(binding.raiz)

        // Aunque el servidor ya lo bloquea (403), un veterinario ni siquiera ve esta pantalla.
        if (!repo.session.esAdmin()) {
            Toast.makeText(this, "Solo el administrador puede gestionar usuarios", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        binding.spEstado.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, estados)
        binding.spEstado.setSelection(1)         // al entrar se ven primero los que esperan aprobación

        adaptador = UsuarioAdapter { usuario -> mostrarOpciones(usuario) }
        binding.rvUsuarios.layoutManager = LinearLayoutManager(this)
        binding.rvUsuarios.adapter = adaptador

        // El filtro se aplica sobre la lista ya descargada: es inmediato.
        binding.spEstado.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                filtrar()
            }

            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        binding.etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filtrar()
            }
        })
    }

    /** Se recarga al volver del formulario de veterinario, para ver el perfil nuevo. */
    override fun onResume() {
        super.onResume()
        if (repo.session.esAdmin()) cargar()
    }

    // --------------------------------------------------------------- consulta

    private fun cargar() {
        binding.progreso.visibility = View.VISIBLE
        lifecycleScope.launch {
            when (val usuarios = repo.usuarios()) {
                is ApiResult.Exito -> todos = usuarios.datos
                is ApiResult.Falla -> {
                    binding.progreso.visibility = View.GONE
                    binding.tvVacio.visibility = View.GONE
                    mostrarFalla(usuarios)
                    return@launch
                }
            }

            // Para saber qué cuentas ya tienen su perfil de veterinario
            val vets = repo.veterinarios(estado = "TODOS")
            perfiles = if (vets is ApiResult.Exito) {
                vets.datos.filter { it.idUsuario != null }.associateBy { it.idUsuario!! }
            } else {
                emptyMap()
            }
            binding.progreso.visibility = View.GONE
            filtrar()
        }
    }

    private fun filtrar() {
        val texto = binding.etBuscar.text.toString().trim().lowercase()
        val lista = todos.filter { u ->
            val estadoOk = when (binding.spEstado.selectedItemPosition) {
                1 -> !u.activo
                2 -> u.activo
                else -> true
            }
            val textoOk = texto.isEmpty() ||
                    u.nombre.lowercase().contains(texto) ||
                    u.username.lowercase().contains(texto) ||
                    u.email.lowercase().contains(texto)
            estadoOk && textoOk
        }
        adaptador.actualizar(lista, perfiles.keys)
        binding.tvContador.text = "${lista.size} usuario(s)"
        binding.tvVacio.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
    }

    // ---------------------------------------------------------------- acciones

    private fun mostrarOpciones(u: Usuario) {
        val opciones = mutableListOf("Ver detalle")
        opciones.add(if (u.activo) "Desactivar acceso" else "Aprobar / activar acceso")
        val sinPerfil = u.esVeterinario && !perfiles.containsKey(u.idUsuario)
        if (sinPerfil) opciones.add("Registrar perfil de veterinario")

        AlertDialog.Builder(this)
            .setTitle(u.nombre)
            .setItems(opciones.toTypedArray()) { _, i ->
                when (opciones[i]) {
                    "Ver detalle" -> verDetalle(u)
                    "Desactivar acceso" -> confirmarDesactivar(u)
                    "Aprobar / activar acceso" -> cambiarEstado(u, true)
                    "Registrar perfil de veterinario" -> registrarPerfil(u)
                }
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun verDetalle(u: Usuario) {
        val perfil = perfiles[u.idUsuario]
        var texto = "Usuario: ${u.username}\n" +
                "Correo: ${u.email}\n" +
                "Rol solicitado: ${(u.roles ?: emptyList()).joinToString(", ").ifEmpty { "ninguno" }}\n" +
                "Estado: ${if (u.activo) "activo" else "pendiente o desactivado"}"
        if (perfil != null) {
            texto += "\n\nPerfil de veterinario\n" +
                    "Documento: ${perfil.tipoDocumento} ${perfil.numeroDocumento}\n" +
                    "Especialidad: ${perfil.especialidad ?: "—"}\n" +
                    "Teléfono: ${perfil.telefono ?: "—"}\n" +
                    "Estado del perfil: ${perfil.estado ?: "ACTIVO"}"
        } else if (u.esVeterinario) {
            texto += "\n\nPerfil de veterinario: aún no registrado"
        }
        AlertDialog.Builder(this)
            .setTitle(u.nombre)
            .setMessage(texto)
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun confirmarDesactivar(u: Usuario) {
        AlertDialog.Builder(this)
            .setTitle("Desactivar acceso")
            .setMessage("¿Desactivar a ${u.nombre}? No podrá iniciar sesión hasta que se vuelva a activar.")
            .setPositiveButton("Desactivar") { _, _ -> cambiarEstado(u, false) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun cambiarEstado(u: Usuario, activo: Boolean) {
        binding.progreso.visibility = View.VISIBLE
        lifecycleScope.launch {
            val r = repo.cambiarEstadoUsuario(u, activo)
            binding.progreso.visibility = View.GONE
            when (r) {
                is ApiResult.Exito -> {
                    Toast.makeText(
                        this@UsuariosActivity,
                        if (activo) "Cuenta aprobada" else "Acceso desactivado",
                        Toast.LENGTH_SHORT
                    ).show()
                    cargar()
                    // Recién aprobado y sin perfil: se ofrece crearlo en el mismo momento
                    if (activo && u.esVeterinario && !perfiles.containsKey(u.idUsuario)) {
                        ofrecerPerfil(u)
                    }
                }
                // 409: por ejemplo, un administrador no puede desactivarse a sí mismo
                is ApiResult.Falla -> mostrarError(r)
            }
        }
    }

    private fun ofrecerPerfil(u: Usuario) {
        AlertDialog.Builder(this)
            .setTitle("Perfil de veterinario")
            .setMessage("${u.nombre} ya puede iniciar sesión, pero todavía no tiene perfil de veterinario. ¿Registrarlo ahora?")
            .setPositiveButton("Registrar") { _, _ -> registrarPerfil(u) }
            .setNegativeButton("Más tarde", null)
            .show()
    }

    /** Abre el formulario de veterinario con la cuenta ya elegida y el nombre y correo precargados. */
    private fun registrarPerfil(u: Usuario) {
        val intent = Intent(this, RegistroVeterinarioActivity::class.java)
        intent.putExtra(RegistroVeterinarioActivity.EXTRA_ID_USUARIO, u.idUsuario)
        intent.putExtra(RegistroVeterinarioActivity.EXTRA_NOMBRE, u.nombre)
        intent.putExtra(RegistroVeterinarioActivity.EXTRA_EMAIL, u.email)
        startActivity(intent)
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
