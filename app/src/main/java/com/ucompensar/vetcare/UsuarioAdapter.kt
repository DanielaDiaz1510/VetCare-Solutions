package com.ucompensar.vetcare

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ucompensar.vetcare.databinding.ItemUsuarioBinding
import com.ucompensar.vetcare.models.Usuario

/** Dibuja cada cuenta de acceso. Al tocar una tarjeta avisa a la pantalla. */
class UsuarioAdapter(
    private val alTocar: (Usuario) -> Unit
) : RecyclerView.Adapter<UsuarioAdapter.Holder>() {

    private var lista: List<Usuario> = emptyList()
    private var conPerfil: Set<Long> = emptySet()       // ids de usuario que ya tienen perfil de veterinario

    fun actualizar(nueva: List<Usuario>, usuariosConPerfil: Set<Long>) {
        lista = nueva
        conPerfil = usuariosConPerfil
        notifyDataSetChanged()
    }

    class Holder(val b: ItemUsuarioBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(ItemUsuarioBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = lista.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val u = lista[position]
        val ctx = holder.b.root.context
        holder.b.tvNombre.text = u.nombre
        holder.b.tvEstado.text = if (u.activo) "ACTIVO" else "INACTIVO"
        holder.b.tvEstado.setTextColor(
            ContextCompat.getColor(ctx, if (u.activo) R.color.estado_ok else R.color.estado_error)
        )
        holder.b.tvLinea1.text = "${u.username} · ${u.email}"
        val roles = (u.roles ?: emptyList()).joinToString(", ").ifEmpty { "sin rol" }
        val perfil = when {
            !u.esVeterinario -> ""
            conPerfil.contains(u.idUsuario) -> " · Perfil de veterinario: sí"
            else -> " · Perfil de veterinario: pendiente"
        }
        holder.b.tvLinea2.text = "Rol: $roles$perfil"
        holder.b.root.setOnClickListener { alTocar(u) }
    }
}
