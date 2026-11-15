package com.ucompensar.vetcare

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ucompensar.vetcare.api.ApiResult
import com.ucompensar.vetcare.databinding.ActivityPropietariosBinding
import com.ucompensar.vetcare.models.Propietario
import kotlinx.coroutines.launch

/**
 * Lista de propietarios con búsqueda por dos criterios (RF-09): nombre/apellido y documento.
 * Se filtra también por estado (activos por defecto). Al tocar uno: ver detalle (RF-10), editar,
 * desactivar o reactivar, o eliminar (solo ADMIN, y solo si nunca tuvo mascotas).
 * Desactivar conserva al propietario y sus mascotas; eliminar lo borra del todo.
 */
class PropietariosActivity : BaseActivity() {

    private lateinit var binding: ActivityPropietariosBinding
    private lateinit var adaptador: PropietarioAdapter

    private val estados = listOf("Activos", "Inactivos", "Todos")
    private val valoresEstado = listOf(null, "INACTIVO", "TODOS")    // null = por defecto, ACTIVOS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPropietariosBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ajustarBarras(binding.raiz)

        adaptador = PropietarioAdapter { propietario -> mostrarOpciones(propietario) }
        binding.rvPropietarios.layoutManager = LinearLayoutManager(this)
        binding.rvPropietarios.adapter = adaptador

        binding.spEstado.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, estados)
        // Al elegir un estado, la lista se recarga sola (sin tener que pulsar Buscar).
        alElegir(binding.spEstado) { cargar() }

        binding.btnBuscar.setOnClickListener { cargar() }
        val alBuscar = { _: android.widget.TextView, accion: Int, _: android.view.KeyEvent? ->
            if (accion == EditorInfo.IME_ACTION_SEARCH) {
                cargar()
                true
            } else {
                false
            }
        }
        binding.etBuscar.setOnEditorActionListener(alBuscar)
        binding.etDocumento.setOnEditorActionListener(alBuscar)

        binding.btnNuevo.setOnClickListener {
            startActivity(Intent(this, RegistroPropietarioActivity::class.java))
        }
    }

    /** Ejecuta [accion] solo cuando cambia la opción elegida; ignora el aviso inicial de Android. */
    private fun alElegir(spinner: Spinner, accion: () -> Unit) {
        var ultima = spinner.selectedItemPosition
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                if (pos == ultima) return
                ultima = pos
                accion()
            }

            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    /** Se recarga al volver de crear o editar, para ver los cambios. */
    override fun onResume() {
        super.onResume()
        cargar()
    }

    // --------------------------------------------------------------- consulta

    private fun cargar() {
        binding.progreso.visibility = View.VISIBLE
        lifecycleScope.launch {
            val resultado = repo.propietarios(
                texto = binding.etBuscar.text.toString(),
                documento = binding.etDocumento.text.toString(),
                estado = valoresEstado[binding.spEstado.selectedItemPosition]
            )
            binding.progreso.visibility = View.GONE
            when (resultado) {
                is ApiResult.Exito -> {
                    val lista = resultado.datos
                    adaptador.actualizar(lista)
                    binding.tvContador.text = "${lista.size} propietario(s)"
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

    private fun mostrarOpciones(p: Propietario) {
        val inactivo = p.estado == "INACTIVO"
        val opciones = arrayOf(
            "Ver detalle",
            "Editar",
            if (inactivo) "Reactivar" else "Desactivar",
            "Eliminar"
        )
        AlertDialog.Builder(this)
            .setTitle(p.nombreCompleto)
            .setItems(opciones) { _, i ->
                when (i) {
                    0 -> verDetalle(p)
                    1 -> editar(p)
                    2 -> if (inactivo) cambiarEstado(p, "ACTIVO") else confirmarDesactivar(p)
                    3 -> confirmarEliminar(p)
                }
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun verDetalle(p: Propietario) {
        val texto = "Documento: ${p.tipoDocumento} ${p.numeroDocumento}\n" +
                "Teléfono: ${p.telefono ?: "—"}\n" +
                "Correo: ${p.email ?: "—"}\n" +
                "Dirección: ${p.direccion ?: "—"}\n" +
                "Estado: ${p.estado ?: "ACTIVO"}"
        AlertDialog.Builder(this)
            .setTitle(p.nombreCompleto)
            .setMessage(texto)
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun editar(p: Propietario) {
        val id = p.id ?: return
        val intent = Intent(this, RegistroPropietarioActivity::class.java)
        intent.putExtra(RegistroPropietarioActivity.EXTRA_ID, id)
        startActivity(intent)
    }

    private fun confirmarDesactivar(p: Propietario) {
        AlertDialog.Builder(this)
            .setTitle("Desactivar propietario")
            .setMessage("¿Desactivar a ${p.nombreCompleto}? Deja de aparecer en la lista de activos, pero no se borra y se puede reactivar. Para desactivarlo no debe tener mascotas activas.")
            .setPositiveButton("Desactivar") { _, _ -> cambiarEstado(p, "INACTIVO") }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /** Desactivar o reactivar = guardar de nuevo con otro estado (PUT); los demás datos no cambian. */
    private fun cambiarEstado(p: Propietario, nuevo: String) {
        lifecycleScope.launch {
            when (val r = repo.guardarPropietario(p.copy(estado = nuevo))) {
                is ApiResult.Exito -> {
                    Toast.makeText(
                        this@PropietariosActivity,
                        if (nuevo == "ACTIVO") "Propietario reactivado" else "Propietario desactivado",
                        Toast.LENGTH_SHORT
                    ).show()
                    cargar()
                }
                // 409 si todavía tiene mascotas activas; 403 si el rol no puede
                is ApiResult.Falla -> mostrarError(r)
            }
        }
    }

    /** 401, 403 y falta de conexión los maneja BaseActivity; las reglas de negocio se muestran en un diálogo. */
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

    private fun confirmarEliminar(p: Propietario) {
        val id = p.id ?: return
        AlertDialog.Builder(this)
            .setTitle("Eliminar propietario")
            .setMessage("¿Eliminar a ${p.nombreCompleto}? Solo es posible si nunca tuvo mascotas y no se puede deshacer. Si solo quiere retirarlo de la lista, use Desactivar.")
            .setPositiveButton("Eliminar") { _, _ -> eliminar(id) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminar(id: Long) {
        lifecycleScope.launch {
            when (val r = repo.eliminarPropietario(id)) {
                is ApiResult.Exito -> {
                    Toast.makeText(this@PropietariosActivity, "Propietario eliminado", Toast.LENGTH_SHORT).show()
                    cargar()
                }
                // 403 si es VETERINARIO; 409 si todavía tiene mascotas
                is ApiResult.Falla -> mostrarFalla(r)
            }
        }
    }
}