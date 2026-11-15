package com.ucompensar.vetcare

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.ucompensar.vetcare.api.ApiResult
import com.ucompensar.vetcare.databinding.ActivityMainBinding
import com.ucompensar.vetcare.models.Estadisticas
import kotlinx.coroutines.launch

/**
 * Home: perfil del usuario autenticado, resumen del sistema, módulos
 * (y administración para el ADMIN), modo oscuro y cierre de sesión.
 */
class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Sin sesión guardada no se puede estar en el Home
        if (!repo.session.haySesion()) {
            irALogin()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ajustarBarras(binding.raiz)

        pintarEncabezado()
        configurarModulos()
        configurarOpciones()
        cargarDatos()
    }

    // ------------------------------------------------------------ encabezado

    /** Muestra nombre y rol. Primero con lo guardado; luego se refresca con /me. */
    private fun pintarEncabezado() {
        val nombre = repo.session.nombre()
        binding.tvSaludo.text = if (nombre.isBlank()) "Hola" else "Hola, $nombre"
        binding.tvRol.text = "Rol: " + textoRoles()
        // Usuarios y Veterinarios solo los gestiona el administrador
        binding.llAdmin.visibility = if (repo.session.esAdmin()) View.VISIBLE else View.GONE
    }

    private fun textoRoles(): String =
        repo.session.roles()
            .joinToString(" y ") {
                when (it) {
                    "ADMIN" -> "Administrador"
                    "VETERINARIO" -> "Veterinario"
                    else -> it
                }
            }
            .ifBlank { "sin rol" }

    // ------------------------------------------------------- datos de la API

    /** GET /me (perfil) y GET /estadisticas (resumen). */
    private fun cargarDatos() {
        binding.progreso.visibility = View.VISIBLE
        lifecycleScope.launch {

            when (val p = repo.perfil()) {
                is ApiResult.Exito -> {
                    repo.session.guardarPerfil(p.datos)
                    pintarEncabezado()
                }
                is ApiResult.Falla -> {
                    // 401 lleva al login; sin conexión se avisa una sola vez
                    binding.progreso.visibility = View.GONE
                    mostrarErrorResumen()
                    mostrarFalla(p)
                    return@launch
                }
            }

            when (val e = repo.estadisticas()) {
                is ApiResult.Exito -> pintarResumen(e.datos)
                is ApiResult.Falla -> {
                    mostrarErrorResumen()
                    mostrarFalla(e)
                }
            }
            binding.progreso.visibility = View.GONE
        }
    }

    /** Muestra cada cifra del resumen en su tarjeta. */
    private fun pintarResumen(e: Estadisticas) {
        binding.tvResumen.visibility = View.GONE
        binding.tvNumMascotas.text = e.mascotas.toString()
        binding.tvNumPropietarios.text = e.propietarios.toString()
        binding.tvNumVeterinarios.text = e.veterinarios.toString()
        binding.tvNumCitasProgramadas.text = e.citasProgramadas.toString()
        binding.tvNumCitasHoy.text = e.citasHoy.toString()
        binding.tvNumVacunas.text = e.vacunasPorVencer30Dias.toString()
    }

    private fun mostrarErrorResumen() {
        binding.tvResumen.text = "No se pudo cargar el resumen."
        binding.tvResumen.visibility = View.VISIBLE
    }

    // --------------------------------------------------------------- módulos

    /**
     * Cada módulo se conecta cuando su pantalla existe. Ejemplo para el Paso 10:
     *   binding.btnMascotas.setOnClickListener {
     *       startActivity(android.content.Intent(this, MascotasActivity::class.java))
     *   }
     */
    private fun configurarModulos() {
        binding.btnMascotas.setOnClickListener {
            startActivity(android.content.Intent(this, MascotasActivity::class.java))
        }
        binding.btnPropietarios.setOnClickListener {
            startActivity(android.content.Intent(this, PropietariosActivity::class.java))
        }
        binding.btnCitas.setOnClickListener {
            startActivity(android.content.Intent(this, CitasActivity::class.java))
        }
        binding.btnHistorias.setOnClickListener {
            startActivity(android.content.Intent(this, HistoriasActivity::class.java))
        }
        binding.btnVacunas.setOnClickListener {
            startActivity(android.content.Intent(this, VacunasActivity::class.java))
        }
        binding.btnUsuarios.setOnClickListener {
            startActivity(android.content.Intent(this, UsuariosActivity::class.java))
        }
        binding.btnVeterinarios.setOnClickListener {
            startActivity(android.content.Intent(this, VeterinariosActivity::class.java))
        }
    }

    private fun proximamente(nombre: String) {
        Toast.makeText(this, "$nombre: próximamente", Toast.LENGTH_SHORT).show()
    }

    // --------------------------------------------------------------- opciones

    private fun configurarOpciones() {
        // Primero se fija el estado y después se escucha, para no disparar el cambio al abrir
        binding.swTema.isChecked = repo.session.modoOscuro
        binding.swTema.setOnCheckedChangeListener { _, marcado ->
            repo.session.modoOscuro = marcado
            repo.session.aplicarTema()          // la pantalla se recrea con el tema nuevo
        }

        binding.btnSalir.setOnClickListener { cerrarSesion() }
    }

    /** RF-12: avisa al servidor (revoca el token), borra la sesión local y vuelve al login. */
    private fun cerrarSesion() {
        lifecycleScope.launch {
            repo.cerrarSesion()
            irALogin("Sesión cerrada correctamente.")
        }
    }

}