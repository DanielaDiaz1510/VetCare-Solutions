package com.ucompensar.vetcare

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ucompensar.vetcare.api.ApiResult
import com.ucompensar.vetcare.databinding.ActivityVacunasBinding
import com.ucompensar.vetcare.models.Mascota
import com.ucompensar.vetcare.models.Vacunacion
import kotlinx.coroutines.launch

/**
 * Carnet de vacunación: lista con filtro por mascota y por refuerzos próximos (RF-09).
 * Al tocar un registro se muestra su detalle (RF-10).
 */
class VacunasActivity : BaseActivity() {

    private lateinit var binding: ActivityVacunasBinding
    private lateinit var adaptador: VacunacionAdapter
    private var mascotas: List<Mascota> = emptyList()
    private var mascotasListas = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVacunasBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ajustarBarras(binding.raiz)

        adaptador = VacunacionAdapter { vacunacion -> verDetalle(vacunacion) }
        binding.rvVacunaciones.layoutManager = LinearLayoutManager(this)
        binding.rvVacunaciones.adapter = adaptador

        binding.btnBuscar.setOnClickListener { cargar() }

        // Al elegir una mascota o marcar "próximas", la lista se recarga sola.
        binding.spMascota.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                if (mascotasListas) cargar()
            }

            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        binding.cbProximas.setOnCheckedChangeListener { _, _ ->
            if (mascotasListas) cargar()
        }

        binding.btnNueva.setOnClickListener {
            startActivity(Intent(this, RegistroVacunacionActivity::class.java))
        }

        cargarMascotas()
    }

    /** Se recarga al volver de registrar, para ver el nuevo registro. */
    override fun onResume() {
        super.onResume()
        if (mascotasListas) cargar()
    }

    // --------------------------------------------------------------- consulta

    private fun cargarMascotas() {
        binding.progreso.visibility = View.VISIBLE
        lifecycleScope.launch {
            when (val r = repo.mascotas(estado = "TODOS")) {
                is ApiResult.Exito -> {
                    mascotas = r.datos
                    val nombres = listOf("Todas las mascotas") +
                            mascotas.map { "${it.nombre} (${it.especie})" }
                    binding.spMascota.adapter = ArrayAdapter(
                        this@VacunasActivity,
                        android.R.layout.simple_spinner_dropdown_item,
                        nombres
                    )
                    mascotasListas = true
                    cargar()
                }
                is ApiResult.Falla -> {
                    binding.progreso.visibility = View.GONE
                    mostrarFalla(r)
                }
            }
        }
    }

    private fun cargar() {
        binding.progreso.visibility = View.VISIBLE
        val pos = binding.spMascota.selectedItemPosition
        val idMascota = if (pos <= 0) null else mascotas.getOrNull(pos - 1)?.id
        val proximasDias = if (binding.cbProximas.isChecked) 30 else null
        lifecycleScope.launch {
            val resultado = repo.vacunaciones(idMascota = idMascota, proximasDias = proximasDias)
            binding.progreso.visibility = View.GONE
            when (resultado) {
                is ApiResult.Exito -> {
                    val lista = resultado.datos
                    adaptador.actualizar(lista)
                    binding.tvContador.text = "${lista.size} vacunación(es)"
                    binding.tvVacio.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
                }
                is ApiResult.Falla -> {
                    binding.tvVacio.visibility = View.GONE
                    mostrarFalla(resultado)
                }
            }
        }
    }

    private fun verDetalle(v: Vacunacion) {
        val texto = "Mascota: ${v.nombreMascota ?: "—"}\n" +
                "Vacuna: ${v.nombreVacuna ?: "—"}\n" +
                "Aplicada: ${v.fechaAplicacion}\n" +
                "Próximo refuerzo: ${v.fechaProxima ?: "sin programar"}\n" +
                "Observaciones: ${v.observaciones ?: "—"}"
        AlertDialog.Builder(this)
            .setTitle("Detalle de la vacunación")
            .setMessage(texto)
            .setPositiveButton("Cerrar", null)
            .show()
    }
}