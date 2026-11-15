package com.ucompensar.vetcare

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ucompensar.vetcare.databinding.ItemVeterinarioBinding
import com.ucompensar.vetcare.models.Veterinario

/** Dibuja cada veterinario de la lista. Al tocar una tarjeta avisa a la pantalla. */
class VeterinarioAdapter(
    private val alTocar: (Veterinario) -> Unit
) : RecyclerView.Adapter<VeterinarioAdapter.Holder>() {

    private var lista: List<Veterinario> = emptyList()
    private var cuentas: Map<Long, String> = emptyMap()   // idUsuario -> nombre de usuario

    fun actualizar(nueva: List<Veterinario>, usuarios: Map<Long, String>) {
        lista = nueva
        cuentas = usuarios
        notifyDataSetChanged()
    }

    class Holder(val b: ItemVeterinarioBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(ItemVeterinarioBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = lista.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val v = lista[position]
        val ctx = holder.b.root.context
        val activo = v.estado != "INACTIVO"
        holder.b.tvNombre.text = v.nombreCompleto
        holder.b.tvEstado.text = if (activo) "ACTIVO" else "INACTIVO"
        holder.b.tvEstado.setTextColor(
            ContextCompat.getColor(ctx, if (activo) R.color.estado_ok else R.color.estado_error)
        )
        holder.b.tvLinea1.text = "${v.tipoDocumento} ${v.numeroDocumento} · ${v.especialidad ?: "Sin especialidad"}"
        val cuenta = v.idUsuario?.let { cuentas[it] ?: "vinculada" }
        holder.b.tvLinea2.text = if (cuenta == null) "Sin cuenta de acceso" else "Cuenta: $cuenta"
        holder.b.root.setOnClickListener { alTocar(v) }
    }
}
