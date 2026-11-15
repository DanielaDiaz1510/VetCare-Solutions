package com.ucompensar.vetcare

import android.app.Application
import com.ucompensar.vetcare.preferences.SessionManager

/** Se ejecuta una vez al abrir la app: aplica el tema (claro/oscuro) que eligió el usuario (RF-11). */
class VetCareApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SessionManager(this).aplicarTema()
    }
}