package com.ucompensar.vetcare

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ucompensar.vetcare.databinding.ItemCitaBinding
import com.ucompensar.vetcare.models.Cita

/** Convierte "2026-12-20T10:00:00" en "2026-12-20 10:00". */
fun formatoFecha(iso: String?): String = iso?.replace('T', ' ')?.take(16) ?: "—"

/** Dibuja cada cita de la lista. Al tocar una tarjeta avisa a la pantalla. */
class CitaAdapter(
    private val alTocar: (Cita) -> Unit
) : RecyclerView.Adapter<CitaAdapter.Holder>() {

    private var lista: List<Cita> = emptyList()

    fun actualizar(nueva: List<Cita>) {
        lista = nueva
        notifyDataSetChanged()
    }

    class Holder(val b: ItemCitaBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(ItemCitaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = lista.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val c = lista[position]
        holder.b.tvFecha.text = "${formatoFecha(c.fechaHora)} · ${c.duracionMinutos ?: 30} min"
        holder.b.tvEstado.text = c.estado ?: "—"
        holder.b.tvEstado.setTextColor(
            ContextCompat.getColor(
                holder.b.root.context,
                when (c.estado) {
                    "ATENDIDA" -> R.color.estado_ok
                    "CANCELADA" -> R.color.estado_error
                    else -> R.color.estado_info
                }
            )
        )
        holder.b.tvMascota.text = "Mascota: ${c.nombreMascota ?: "—"}"
        holder.b.tvVeterinario.text = "Veterinario: ${c.nombreVeterinario ?: "—"}"
        holder.b.tvMotivo.text = "Motivo: ${c.motivo ?: "—"}"
        holder.b.root.setOnClickListener { alTocar(c) }
    }
}