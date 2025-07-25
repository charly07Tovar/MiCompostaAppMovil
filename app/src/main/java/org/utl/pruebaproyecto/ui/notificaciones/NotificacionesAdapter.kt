package org.utl.pruebaproyecto.ui.notificaciones

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.utl.pruebaproyecto.R

class NotificacionesAdapter : RecyclerView.Adapter<NotificacionesAdapter.NotificationViewHolder>() {

    private var notifications = mutableListOf<NotificationItem>()

    fun updateNotifications(newNotifications: List<NotificationItem>) {
        notifications.clear()
        notifications.addAll(newNotifications)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notificacion, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(notifications[position])
    }

    override fun getItemCount(): Int = notifications.size

    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconWarning: ImageView = itemView.findViewById(R.id.icon_warning)
        private val textTitle: TextView = itemView.findViewById(R.id.text_title)
        private val textDescription: TextView = itemView.findViewById(R.id.text_description)
        private val textTime: TextView = itemView.findViewById(R.id.text_time)

        fun bind(notification: NotificationItem) {
            textTitle.text = notification.title
            textDescription.text = notification.description
            textTime.text = notification.time

            // Configurar icono según el tipo
            when (notification.type) {
                NotificationType.WARNING -> {
                    iconWarning.setImageResource(R.drawable.ic_alerta)
                    iconWarning.setColorFilter(android.graphics.Color.parseColor("#FF9800"))
                }
                NotificationType.ERROR -> {
                    iconWarning.setImageResource(R.drawable.ic_alerta)
                    iconWarning.setColorFilter(android.graphics.Color.parseColor("#F44336"))
                }
                NotificationType.INFO -> {
                    iconWarning.setImageResource(R.drawable.ic_alerta)
                    iconWarning.setColorFilter(android.graphics.Color.parseColor("#2196F3"))
                }
            }
        }
    }
}
