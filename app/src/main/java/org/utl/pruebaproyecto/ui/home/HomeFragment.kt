package org.utl.pruebaproyecto.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import org.utl.pruebaproyecto.R
import org.utl.pruebaproyecto.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Si tienes un TextView en tu layout actual, puedes mantenerlo o comentarlo
        // val textView: TextView = binding.textHome
        // homeViewModel.text.observe(viewLifecycleOwner) {
        //     textView.text = it
        // }

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Monitoreo de condiciones
        binding.cardMonitoring.setOnClickListener {
            Toast.makeText(context, "Monitoreo de condiciones", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.nav_monitoreo_condiciones)
        }

        // Control y asistencia activa
        binding.cardControl.setOnClickListener {
            Toast.makeText(context, "Control y asistencia activa", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.nav_control_asistencia)
        }

        // Historial de registros
        binding.cardHistory.setOnClickListener {
            Toast.makeText(context, "Historial de registros", Toast.LENGTH_SHORT).show()
             findNavController().navigate(R.id.nav_historial)
        }

        // Notificaciones
        binding.cardNotifications.setOnClickListener {
            Toast.makeText(context, "Notificaciones", Toast.LENGTH_SHORT).show()
            Toast.makeText(context, "Modificacion de prueba para inicializar rama de dev charly", Toast.LENGTH_SHORT).show()
             findNavController().navigate(R.id.nav_notificaciones)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}