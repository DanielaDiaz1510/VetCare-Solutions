package com.ucompensar.vetcare

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ucompensar.vetcare.databinding.ItemHistoriaBinding
import com.ucompensar.vetcare.models.HistorialClinico

/** Dibuja cada atención de la historia clínica. Al tocar una tarjeta avisa a la pantalla. */
class HistoriaAdapter(
    private val alTocar: (HistorialClinico) -> Unit
) : RecyclerView.Adapter<HistoriaAdapter.Holder>() {

    private var lista: List<HistorialClinico> = emptyList()

    fun actualizar(nueva: List<HistorialClinico>) {
        lista = nueva
        notifyDataSetChanged()
    }

    class Holder(val b: ItemHistoriaBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(ItemHistoriaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = lista.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val h = lista[position]
        holder.b.tvFecha.text = formatoFecha(h.fechaAtencion)
        holder.b.tvMascota.text = "Mascota: ${h.nombreMascota ?: "—"}"
        holder.b.tvMotivo.text = "Motivo: ${h.motivoConsulta}"
        holder.b.tvDiagnostico.text = "Diagnóstico: ${h.diagnostico ?: "Sin diagnóstico"}"
        holder.b.tvVeterinario.text = "Atendió: ${h.nombreCompletoVeterinario ?: "—"}"
        holder.b.root.setOnClickListener { alTocar(h) }
    }
}