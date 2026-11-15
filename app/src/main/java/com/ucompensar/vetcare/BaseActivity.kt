package com.ucompensar.vetcare

import android.content.Intent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar
import com.ucompensar.vetcare.api.ApiResult
import com.ucompensar.vetcare.repository.VetRepository

/**
 * Base de todas las pantallas. Reúne lo que todas necesitan:
 *  - acceso al repositorio
 *  - qué hacer cuando la API falla (401 sesión vencida, 403 sin permiso, otros)
 */
abstract class BaseActivity : AppCompatActivity() {

    protected val repo by lazy { VetRepository(this) }

    /**
     * Si la pantalla trae una barra superior (id "toolbar"), su flecha de retroceso
     * vuelve a la pantalla anterior. Así no hay que configurarla en cada Activity.
     */
    override fun setContentView(view: View?) {
        super.setContentView(view)
        findViewById<MaterialToolbar>(R.id.toolbar)?.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    /**
     * Respuesta uniforme ante una falla de la API:
     *  - 401 con sesión abierta -> el token venció o fue revocado: vuelve al login.
     *  - 403 -> "Acceso denegado" (el rol no tiene permiso).
     *  - cualquier otro -> mensaje del servidor.
     */
    protected fun mostrarFalla(falla: ApiResult.Falla) {
        when {
            falla.sesionInvalida && repo.session.haySesion() -> {
                repo.session.cerrarSesion()
                irALogin("Su sesión expiró. Inicie sesión de nuevo.")
            }
            falla.sinPermiso -> {
                AlertDialog.Builder(this)
                    .setTitle("Acceso denegado")
                    .setMessage(falla.mensaje)
                    .setPositiveButton("Entendido", null)
                    .show()
            }
            else -> Toast.makeText(this, falla.mensaje, Toast.LENGTH_LONG).show()
        }
    }

    protected fun irALogin(mensaje: String? = null) {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_MENSAJE, mensaje)
        }
        startActivity(intent)
        finish()
    }

    /** Evita que el contenido quede bajo la barra de estado, la de navegación o el teclado. */
    protected fun ajustarBarras(vista: View) {
        ViewCompat.setOnApplyWindowInsetsListener(vista) { v, insets ->
            val b = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            v.setPadding(b.left, b.top, b.right, b.bottom)
            insets
        }
    }

    companion object {
        const val EXTRA_MENSAJE = "mensaje"
    }
}