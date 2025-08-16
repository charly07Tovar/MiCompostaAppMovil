package org.utl.pruebaproyecto.ui.notificaciones

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import org.utl.pruebaproyecto.databinding.FragmentHistorialNotificacionesBinding
import java.util.*

class HistorialNotificaciones : Fragment() {
    private var _binding: FragmentHistorialNotificacionesBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: NotificacionesAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistorialNotificacionesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        cargarNotificaciones()
    }

    private fun setupRecyclerView() {
        adapter = NotificacionesAdapter(mutableListOf()) { notificacionId ->
            marcarComoLeida(notificacionId)
        }

        binding.recyclerViewNotifications.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@HistorialNotificaciones.adapter
        }
    }

    private fun cargarNotificaciones() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("NOTIFICACIONES", "Usuario no autenticado")
            return
        }

        db.collection("MiCompostaApp").document(userId).collection("notificaciones")
            .orderBy("fecha", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("NOTIFICACIONES", "Error cargando notificaciones", error)
                    return@addSnapshotListener
                }

                val notificaciones = mutableListOf<Notificacion>()
                snapshot?.documents?.forEach { document ->
                    try {
                        val motivo = document.getString("motivo") ?: "Sin motivo"
                        val titulo = generarTituloDesdeMotivo(motivo)

                        val notificacion = Notificacion(
                            id = document.id,
                            fecha = document.getDate("fecha") ?: Date(),
                            titulo = titulo,
                            descripcion = motivo,
                            leida = document.getBoolean("leida") ?: false
                        )
                        notificaciones.add(notificacion)
                    } catch (e: Exception) {
                        Log.e("NOTIFICACIONES", "Error procesando documento", e)
                    }
                }

                adapter.actualizarDatos(notificaciones)
            }
    }

    private fun generarTituloDesdeMotivo(motivo: String): String {
        return when {
            motivo.contains("NH3", ignoreCase = true) && motivo.contains("normalizado", ignoreCase = true) ->
                "NH3 normalizado"
            motivo.contains("CH4", ignoreCase = true) && motivo.contains("normalizado", ignoreCase = true) ->
                "CH4 normalizado"
            motivo.contains("CO2", ignoreCase = true) && motivo.contains("normalizado", ignoreCase = true) ->
                "CO2 normalizado"
            motivo.contains("Temperatura", ignoreCase = true) && motivo.contains("normalizada", ignoreCase = true) ->
                "Temperatura normalizada"
            motivo.contains("Ventilador activado", ignoreCase = true) -> "Ventilador activado"
            motivo.contains("Ventilador desactivado", ignoreCase = true) -> "Ventilador desactivado"
            motivo.contains("bomba se activó", ignoreCase = true) -> "Riego activado"
            motivo.contains("Bomba desactivada", ignoreCase = true) -> "Riego desactivado"
            motivo.contains("NH3", ignoreCase = true) -> "Alerta: Nivel alto de amoníaco"
            motivo.contains("CH4", ignoreCase = true) -> "Alerta: Nivel alto de metano"
            motivo.contains("CO2", ignoreCase = true) -> "Alerta: Nivel alto de CO2"
            motivo.contains("Temperatura crítica", ignoreCase = true) -> "Alerta: Temperatura elevada"
            else -> "Notificación de sensores"
        }
    }


    private fun marcarComoLeida(notificacionId: String) {
        val userId = auth.currentUser?.uid ?: return

        db.collection("MiCompostaApp").document(userId).collection("notificaciones")
            .document(notificacionId)
            .update("leida", true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
