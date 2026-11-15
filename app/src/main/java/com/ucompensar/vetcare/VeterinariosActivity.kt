package com.ucompensar.vetcare

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ucompensar.vetcare.api.ApiResult
import com.ucompensar.vetcare.databinding.ActivityVeterinariosBinding
import com.ucompensar.vetcare.models.Veterinario
import kotlinx.coroutines.launch

/**
 * Gestión de veterinarios (solo ADMIN): registrar, editar, desactivar y reactivar.
 * Cada veterinario puede quedar ligado a una cuenta de acceso; sin esa cuenta no puede iniciar sesión.
 */
class VeterinariosActivity : BaseActivity() {

    private lateinit var binding: ActivityVeterinariosBinding
    private lateinit var adaptador: VeterinarioAdapter

    private val estados = listOf("Todos", "Activos", "Inactivos")
    private val valoresEstado = listOf("TODOS", "ACTIVO", "INACTIVO")
    private var listo = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVeterinariosBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ajustarBarras(binding.raiz)

        if (!repo.session.esAdmin()) {
            Toast.makeText(this, "Solo el administrador puede gestionar veterinarios", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        binding.spEstado.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, estados)

        adaptador = VeterinarioAdapter { veterinario -> mostrarOpciones(veterinario) }
        binding.rvVeterinarios.layoutManager = LinearLayoutManager(this)
        binding.rvVeterinarios.adapter = adaptador

        // Al elegir un estado, la lista se recarga sola
        binding.spEstado.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                if (listo) cargar()
            }

            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        binding.etBuscar.setOnEditorActionListener { _, accion, _ ->
            if (accion == EditorInfo.IME_ACTION_SEARCH) {
                cargar()
                true
            } else {
                false
            }
        }
        binding.btnNuevo.setOnClickListener {
            startActivity(Intent(this, RegistroVeterinarioActivity::class.java))
        }
        listo = true
    }

    /** Se recarga al volver de crear o editar, para ver los cambios. */
    override fun onResume() {
        super.onResume()
        if (repo.session.esAdmin()) cargar()
    }

    // --------------------------------------------------------------- consulta

    private fun cargar() {
        binding.progreso.visibility = View.VISIBLE
        lifecycleScope.launch {
            val resultado = repo.veterinarios(
                texto = binding.etBuscar.text.toString(),
                estado = valoresEstado[binding.spEstado.selectedItemPosition]
            )
            when (resultado) {
                is ApiResult.Exito -> {
                    // Nombres de usuario para mostrar a qué cuenta está ligado cada veterinario
                    val cuentas = when (val u = repo.usuarios()) {
                        is ApiResult.Exito -> u.datos.associate { it.idUsuario to it.username }
                        is ApiResult.Falla -> emptyMap()
                    }
                    binding.progreso.visibility = View.GONE
                    val lista = resultado.datos
                    adaptador.actualizar(lista, cuentas)
                    binding.tvContador.text = "${lista.size} veterinario(s)"
                    binding.tvVacio.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
                }
                is ApiResult.Falla -> {
                    binding.progreso.visibility = View.GONE
                    binding.tvVacio.visibility = View.GONE
                    mostrarFalla(resultado)
                }
            }
        }
    }

    // ---------------------------------------------------------------- acciones

    private fun mostrarOpciones(v: Veterinario) {
        val activo = v.estado != "INACTIVO"
        val opciones = arrayOf("Ver detalle", "Editar", if (activo) "Desactivar" else "Reactivar")
        AlertDialog.Builder(this)
            .setTitle(v.nombreCompleto)
            .setItems(opciones) { _, i ->
                when (i) {
                    0 -> verDetalle(v)
                    1 -> editar(v)
                    2 -> if (activo) confirmarDesactivar(v) else reactivar(v)
                }
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun verDetalle(v: Veterinario) {
        val texto = "Documento: ${v.tipoDocumento} ${v.numeroDocumento}\n" +
                "Especialidad: ${v.especialidad ?: "—"}\n" +
                "Teléfono: ${v.telefono ?: "—"}\n" +
                "Correo: ${v.email ?: "—"}\n" +
                "Estado: ${v.estado ?: "ACTIVO"}\n" +
                "Cuenta de acceso: ${if (v.idUsuario == null) "ninguna" else "vinculada"}"
        AlertDialog.Builder(this)
            .setTitle(v.nombreCompleto)
            .setMessage(texto)
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun editar(v: Veterinario) {
        val id = v.id ?: return
        val intent = Intent(this, RegistroVeterinarioActivity::class.java)
        intent.putExtra(RegistroVeterinarioActivity.EXTRA_ID, id)
        startActivity(intent)
    }

    private fun confirmarDesactivar(v: Veterinario) {
        val id = v.id ?: return
        AlertDialog.Builder(this)
            .setTitle("Desactivar veterinario")
            .setMessage("¿Desactivar a ${v.nombreCompleto}? No se borra: se conserva su historial. Se puede reactivar después.")
            .setPositiveButton("Desactivar") { _, _ -> desactivar(id) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun desactivar(id: Long) {
        lifecycleScope.launch {
            when (val r = repo.desactivarVeterinario(id)) {
                is ApiResult.Exito -> {
                    Toast.makeText(this@VeterinariosActivity, "Veterinario desactivado", Toast.LENGTH_SHORT).show()
                    cargar()
                }
                // 409 si todavía tiene citas programadas
                is ApiResult.Falla -> mostrarError(r)
            }
        }
    }

    /** Reactivar = guardar de nuevo con estado ACTIVO (se conserva la cuenta ligada). */
    private fun reactivar(v: Veterinario) {
        lifecycleScope.launch {
            when (val r = repo.guardarVeterinario(v.copy(estado = "ACTIVO"))) {
                is ApiResult.Exito -> {
                    Toast.makeText(this@VeterinariosActivity, "Veterinario reactivado", Toast.LENGTH_SHORT).show()
                    cargar()
                }
                is ApiResult.Falla -> mostrarError(r)
            }
        }
    }

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
