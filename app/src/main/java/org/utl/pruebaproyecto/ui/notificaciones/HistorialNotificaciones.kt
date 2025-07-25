package org.utl.pruebaproyecto.ui.notificaciones

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.utl.pruebaproyecto.R
import org.utl.pruebaproyecto.databinding.FragmentHistorialNotificacionesBinding


class HistorialNotificaciones : Fragment() {
    private var _binding: FragmentHistorialNotificacionesBinding? = null
    private val binding get() = _binding!!

    private lateinit var notificationsAdapter: NotificacionesAdapter

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
        loadNotifications()
//        binding.btnBack.setOnClickListener {
//            requireActivity().onBackPressed()
//        }
    }

    private fun setupRecyclerView() {
        notificationsAdapter = NotificacionesAdapter()
        binding.recyclerViewNotifications.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = notificationsAdapter
        }
    }

    private fun loadNotifications() {
        // Datos de ejemplo - aquí puedes cargar desde una base de datos o API
        val notifications = listOf(
            NotificationItem(
                title = "Humedad crítica en suelo: 18%",
                description = "Riesgo de deshidratación",
                time = "ahora",
                type = NotificationType.WARNING
            ),
            NotificationItem(
                title = "Temperatura excesiva: 47° C",
                description = "Esto podría afectar significativamente el proceso del compostaje",
                time = "ahora",
                type = NotificationType.WARNING
            ),
            NotificationItem(
                title = "Alta concentración de gases",
                description = "Ventile el compostaje",
                time = "ahora",
                type = NotificationType.WARNING
            )
        )

        notificationsAdapter.updateNotifications(notifications)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// Data classes para las notificaciones
data class NotificationItem(
    val title: String,
    val description: String,
    val time: String,
    val type: NotificationType
)

enum class NotificationType {
    WARNING, INFO, ERROR

}