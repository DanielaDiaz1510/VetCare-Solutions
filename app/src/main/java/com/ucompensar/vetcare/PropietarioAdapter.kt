package com.ucompensar.vetcare

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ucompensar.vetcare.databinding.ItemPropietarioBinding
import com.ucompensar.vetcare.models.Propietario

/** Dibuja cada propietario de la lista. Al tocar una tarjeta avisa a la pantalla. */
class PropietarioAdapter(
    private val alTocar: (Propietario) -> Unit
) : RecyclerView.Adapter<PropietarioAdapter.Holder>() {

    private var lista: List<Propietario> = emptyList()

    fun actualizar(nueva: List<Propietario>) {
        lista = nueva
        notifyDataSetChanged()
    }

    class Holder(val b: ItemPropietarioBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(ItemPropietarioBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = lista.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val p = lista[position]
        holder.b.tvNombre.text = p.nombreCompleto
        holder.b.tvDocumento.text = "${p.tipoDocumento} ${p.numeroDocumento}"
        holder.b.tvContacto.text = listOfNotNull(p.telefono, p.email).joinToString(" · ").ifEmpty { "Sin datos de contacto" }
        holder.b.root.setOnClickListener { alTocar(p) }
    }
}