package com.ucompensar.vetcare

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ucompensar.vetcare.databinding.ItemVacunacionBinding
import com.ucompensar.vetcare.models.Vacunacion

/** Dibuja cada registro del carnet de vacunación. Al tocar una tarjeta avisa a la pantalla. */
class VacunacionAdapter(
    private val alTocar: (Vacunacion) -> Unit
) : RecyclerView.Adapter<VacunacionAdapter.Holder>() {

    private var lista: List<Vacunacion> = emptyList()

    fun actualizar(nueva: List<Vacunacion>) {
        lista = nueva
        notifyDataSetChanged()
    }

    class Holder(val b: ItemVacunacionBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(ItemVacunacionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = lista.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val v = lista[position]
        holder.b.tvVacuna.text = v.nombreVacuna ?: "Vacuna"
        holder.b.tvMascota.text = "Mascota: ${v.nombreMascota ?: "—"}"
        holder.b.tvAplicacion.text = "Aplicada: ${v.fechaAplicacion}"
        holder.b.tvProxima.text = v.fechaProxima?.let { "Próximo refuerzo: $it" } ?: "Sin refuerzo programado"
        holder.b.root.setOnClickListener { alTocar(v) }
    }
}