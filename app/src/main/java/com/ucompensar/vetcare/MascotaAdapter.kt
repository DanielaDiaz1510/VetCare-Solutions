package com.ucompensar.vetcare

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ucompensar.vetcare.databinding.ItemMascotaBinding
import com.ucompensar.vetcare.models.Mascota

/** Dibuja cada mascota de la lista. Al tocar una tarjeta avisa a la pantalla. */
class MascotaAdapter(
    private val alTocar: (Mascota) -> Unit
) : RecyclerView.Adapter<MascotaAdapter.Holder>() {

    private var lista: List<Mascota> = emptyList()

    fun actualizar(nueva: List<Mascota>) {
        lista = nueva
        notifyDataSetChanged()
    }

    class Holder(val b: ItemMascotaBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(ItemMascotaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = lista.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val m = lista[position]
        holder.b.tvNombre.text = m.nombre
        holder.b.tvDetalle.text = listOfNotNull(m.especie, m.raza).joinToString(" · ")
        holder.b.tvPropietario.text = "Propietario: ${m.nombrePropietario ?: "—"}"

        val inactivo = m.estado == "INACTIVO"
        holder.b.tvEstado.text = if (inactivo) "INACTIVA" else "ACTIVA"
        holder.b.tvEstado.setTextColor(
            ContextCompat.getColor(
                holder.b.root.context,
                if (inactivo) R.color.estado_error else R.color.estado_ok
            )
        )

        holder.b.root.setOnClickListener { alTocar(m) }
    }
}