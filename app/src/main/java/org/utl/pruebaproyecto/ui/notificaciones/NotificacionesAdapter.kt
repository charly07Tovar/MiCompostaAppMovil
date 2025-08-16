package org.utl.pruebaproyecto.ui.notificaciones

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*
import org.utl.pruebaproyecto.R

class NotificacionesAdapter(
    private var notificaciones: List<Notificacion>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<NotificacionesAdapter.NotificacionViewHolder>() {

    inner class NotificacionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitulo: TextView = itemView.findViewById(R.id.text_title)
        val tvDescripcion: TextView = itemView.findViewById(R.id.text_description)
        val tvTiempo: TextView = itemView.findViewById(R.id.text_time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificacionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notificacion, parent, false)
        return NotificacionViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificacionViewHolder, position: Int) {
        val notificacion = notificaciones[position]

        // Configurar los textos
        holder.tvTitulo.text = notificacion.titulo
        holder.tvDescripcion.text = notificacion.descripcion

        // Formatear la fecha
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        if (esHoy(notificacion.fecha)) {
            holder.tvTiempo.text = sdf.format(notificacion.fecha)
        } else {
            val sdfFecha = SimpleDateFormat("dd/MM", Locale.getDefault())
            holder.tvTiempo.text = sdfFecha.format(notificacion.fecha)
        }

        // Cambiar estilo si no está leída
        if (!notificacion.leida) {
            holder.tvTitulo.setTextColor(holder.itemView.context.getColor(R.color.colorPrimaryDark))
            holder.tvDescripcion.setTextColor(holder.itemView.context.getColor(R.color.colorPrimary))
        } else {
            holder.tvTitulo.setTextColor(holder.itemView.context.getColor(android.R.color.darker_gray))
            holder.tvDescripcion.setTextColor(holder.itemView.context.getColor(android.R.color.darker_gray))
        }

        holder.itemView.setOnClickListener {
            onItemClick(notificacion.id)
        }
    }

    private fun esHoy(fecha: Date): Boolean {
        val hoy = Calendar.getInstance()
        val fechaNotif = Calendar.getInstance()
        fechaNotif.time = fecha

        return hoy.get(Calendar.YEAR) == fechaNotif.get(Calendar.YEAR) &&
                hoy.get(Calendar.MONTH) == fechaNotif.get(Calendar.MONTH) &&
                hoy.get(Calendar.DAY_OF_MONTH) == fechaNotif.get(Calendar.DAY_OF_MONTH)
    }

    override fun getItemCount() = notificaciones.size

    fun actualizarDatos(nuevasNotificaciones: List<Notificacion>) {
        this.notificaciones = nuevasNotificaciones
        notifyDataSetChanged()
    }
}

