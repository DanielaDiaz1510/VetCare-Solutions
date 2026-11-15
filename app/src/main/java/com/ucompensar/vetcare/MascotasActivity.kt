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
import com.ucompensar.vetcare.databinding.ActivityMascotasBinding
import com.ucompensar.vetcare.models.Mascota
import kotlinx.coroutines.launch

/**
 * Lista de mascotas con búsqueda por tres criterios (RF-09): nombre, especie y estado.
 * Al tocar una mascota: ver detalle (RF-10), editar (RF-07) o desactivar (RF-08).
 */
class MascotasActivity : BaseActivity() {

    private lateinit var binding: ActivityMascotasBinding
    private lateinit var adaptador: MascotaAdapter

    private val especies = listOf("Todas", "Perro", "Gato", "Ave", "Conejo")
    private val estados = listOf("Activas", "Inactivas", "Todas")
    // Lo que se envía al servidor por cada opción de "estados" (null = por defecto, ACTIVAS)
    private val valoresEstado = listOf(null, "INACTIVO", "TODOS")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMascotasBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ajustarBarras(binding.raiz)

        binding.spEspecie.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, especies)
        binding.spEstado.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, estados)

        adaptador = MascotaAdapter { mascota -> mostrarOpciones(mascota) }
        binding.rvMascotas.layoutManager = LinearLayoutManager(this)
        binding.rvMascotas.adapter = adaptador

        binding.btnBuscar.setOnClickListener { cargar() }
        // Al elegir una especie o un estado, la lista se recarga sola (sin tener que pulsar Buscar).
        alElegir(binding.spEspecie) { cargar() }
        alElegir(binding.spEstado) { cargar() }
        binding.etBuscar.setOnEditorActionListener { _, accion, _ ->
            if (accion == EditorInfo.IME_ACTION_SEARCH) {
                cargar()
                true
            } else {
                false
            }
        }
        binding.btnNueva.setOnClickListener {
            startActivity(Intent(this, RegistroMascotaActivity::class.java))
        }
    }

    /**
     * Ejecuta [accion] solo cuando la persona cambia la opción de un desplegable.
     * Android avisa también al dibujar la pantalla; ese aviso trae la misma posición y se ignora
     * (onResume ya carga la lista).
     */
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
        val posEspecie = binding.spEspecie.selectedItemPosition
        lifecycleScope.launch {
            val resultado = repo.mascotas(
                texto = binding.etBuscar.text.toString(),
                especie = if (posEspecie == 0) null else especies[posEspecie],
                estado = valoresEstado[binding.spEstado.selectedItemPosition]
            )
            binding.progreso.visibility = View.GONE
            when (resultado) {
                is ApiResult.Exito -> {
                    val lista = resultado.datos
                    adaptador.actualizar(lista)
                    binding.tvContador.text = "${lista.size} mascota(s)"
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

    private fun mostrarOpciones(m: Mascota) {
        val opciones = mutableListOf("Ver detalle", "Editar")
        opciones.add(if (m.estado == "INACTIVO") "Reactivar" else "Desactivar")

        AlertDialog.Builder(this)
            .setTitle(m.nombre)
            .setItems(opciones.toTypedArray()) { _, i ->
                when (opciones[i]) {
                    "Ver detalle" -> verDetalle(m)
                    "Editar" -> editar(m)
                    "Desactivar" -> confirmarDesactivar(m)
                    "Reactivar" -> reactivar(m)
                }
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun verDetalle(m: Mascota) {
        val sexo = when (m.sexo) {
            "M" -> "Macho"
            "H" -> "Hembra"
            else -> "—"
        }
        val texto = "Especie: ${m.especie}\n" +
                "Raza: ${m.raza ?: "—"}\n" +
                "Sexo: $sexo\n" +
                "Nacimiento: ${m.fechaNacimiento ?: "—"}\n" +
                "Color: ${m.color ?: "—"}\n" +
                "Peso: ${m.pesoActual?.let { "$it kg" } ?: "—"}\n" +
                "Propietario: ${m.nombrePropietario ?: "—"}\n" +
                "Estado: ${m.estado ?: "—"}"
        AlertDialog.Builder(this)
            .setTitle(m.nombre)
            .setMessage(texto)
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun editar(m: Mascota) {
        val id = m.id ?: return
        val intent = Intent(this, RegistroMascotaActivity::class.java)
        intent.putExtra(RegistroMascotaActivity.EXTRA_ID, id)
        startActivity(intent)
    }

    private fun confirmarDesactivar(m: Mascota) {
        val id = m.id ?: return
        AlertDialog.Builder(this)
            .setTitle("Desactivar mascota")
            .setMessage("¿Desactivar a ${m.nombre}? Dejará de aparecer en las listas de activas.")
            .setPositiveButton("Desactivar") { _, _ -> desactivar(id) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun desactivar(id: Long) {
        lifecycleScope.launch {
            when (val r = repo.desactivarMascota(id)) {
                is ApiResult.Exito -> {
                    Toast.makeText(this@MascotasActivity, "Mascota desactivada", Toast.LENGTH_SHORT).show()
                    cargar()
                }
                // 403 si el rol no puede; 409 si tiene citas programadas
                is ApiResult.Falla -> mostrarFalla(r)
            }
        }
    }

    /** Reactivar = guardar de nuevo con estado ACTIVO: la mascota y su historia clínica vuelven a las listas. */
    private fun reactivar(m: Mascota) {
        lifecycleScope.launch {
            when (val r = repo.guardarMascota(m.copy(estado = "ACTIVO"))) {
                is ApiResult.Exito -> {
                    Toast.makeText(this@MascotasActivity, "Mascota reactivada", Toast.LENGTH_SHORT).show()
                    cargar()
                }
                is ApiResult.Falla -> mostrarFalla(r)
            }
        }
    }
}
