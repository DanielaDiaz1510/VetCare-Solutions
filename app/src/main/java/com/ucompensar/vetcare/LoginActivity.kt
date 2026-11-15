package com.ucompensar.vetcare

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.ucompensar.vetcare.api.ApiResult
import com.ucompensar.vetcare.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch
import com.google.android.material.color.MaterialColors

class LoginActivity : BaseActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Si ya hay una sesión guardada, se salta el login
        if (repo.session.haySesion()) {
            irAHome()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ajustarBarras(binding.raiz)

        binding.etUsuario.setText(repo.session.ultimoUsuario)
        binding.etServidor.setText(repo.session.baseUrl)

        // Mensaje que dejó otra pantalla (por ejemplo "Su sesión expiró")
        intent.getStringExtra(EXTRA_MENSAJE)?.let { mensaje ->
            // "Su sesión expiró" es un problema; los demás avisos (registro recibido,
            // sesión cerrada) son informativos y no deben verse como un error
            mostrarMensaje(mensaje, esError = mensaje.contains("expir", ignoreCase = true))
        }

        binding.btnIngresar.setOnClickListener { intentarLogin() }
        binding.etClave.setOnEditorActionListener { _, accion, _ ->
            if (accion == EditorInfo.IME_ACTION_DONE) {
                intentarLogin()
                true
            } else false
        }
        binding.tvConfigurar.setOnClickListener {
            val visible = binding.tilServidor.visibility == View.VISIBLE
            binding.tilServidor.visibility = if (visible) View.GONE else View.VISIBLE
        }
        binding.tvRegistro.setOnClickListener {
            startActivity(Intent(this, RegistroActivity::class.java))
        }
    }

    private fun intentarLogin() {
        val usuario = binding.etUsuario.text?.toString().orEmpty().trim()
        val clave = binding.etClave.text?.toString().orEmpty()

        // Validación en el cliente (RNF-07): evita llamadas inútiles al servidor
        binding.tilUsuario.error = null
        binding.tilClave.error = null
        binding.tvError.visibility = View.GONE
        var valido = true
        if (usuario.isEmpty()) {
            binding.tilUsuario.error = "Ingrese su usuario o correo"
            valido = false
        }
        if (clave.isEmpty()) {
            binding.tilClave.error = "Ingrese su contraseña"
            valido = false
        }
        if (!valido) return

        // Si el usuario cambió la dirección del servidor, se guarda
        val servidor = binding.etServidor.text?.toString().orEmpty()
        if (servidor.isNotBlank()) repo.session.baseUrl = servidor

        enCarga(true)
        lifecycleScope.launch {
            when (val resultado = repo.iniciarSesion(usuario, clave)) {
                is ApiResult.Exito -> {
                    Toast.makeText(
                        this@LoginActivity,
                        "Bienvenido, ${resultado.datos.nombre}",
                        Toast.LENGTH_SHORT
                    ).show()
                    irAHome()
                }
                is ApiResult.Falla -> {
                    enCarga(false)
                    mostrarError(resultado.mensaje)
                }
            }
        }
    }

    private fun enCarga(cargando: Boolean) {
        binding.progreso.visibility = if (cargando) View.VISIBLE else View.GONE
        binding.btnIngresar.isEnabled = !cargando
    }

    private fun mostrarError(mensaje: String) = mostrarMensaje(mensaje, esError = true)

    /** Muestra un texto bajo los campos: en color de error o en el color principal del tema. */
    private fun mostrarMensaje(mensaje: String, esError: Boolean) {
        val atributo = if (esError) {
            androidx.appcompat.R.attr.colorError
        } else {
            androidx.appcompat.R.attr.colorPrimary
        }
        binding.tvError.setTextColor(MaterialColors.getColor(binding.tvError, atributo))
        binding.tvError.text = mensaje
        binding.tvError.visibility = View.VISIBLE
    }

    private fun irAHome() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}