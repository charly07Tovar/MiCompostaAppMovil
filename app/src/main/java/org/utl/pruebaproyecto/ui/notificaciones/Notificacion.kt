package org.utl.pruebaproyecto.ui.notificaciones

import java.util.*

data class Notificacion(
    val id: String = "",
    val fecha: Date = Date(),
    val titulo: String = "Ventilador activado",
    val descripcion: String,
    val leida: Boolean = false,
    val tipo: TipoNotificacion = TipoNotificacion.VENTILADOR
) {
    enum class TipoNotificacion {
        VENTILADOR,
        BOMBA,
        ALERTA,
        INFO
    }

    companion object {
        fun fromMotivo(motivo: String): Notificacion {
            return Notificacion(
                descripcion = when (motivo) {
                    "NH3 Alto" -> "El ventilador se activó por niveles altos de NH3"
                    "CH4 Alto" -> "El ventilador se activó por niveles altos de CH4"
                    "CO2 Alto" -> "El ventilador se activó por niveles altos de CO2"
                    "Temperatura Alta" -> "El ventilador se activó por temperatura alta"
                    else -> "El ventilador se activó por razones desconocidas"
                }
            )
        }
    }
}