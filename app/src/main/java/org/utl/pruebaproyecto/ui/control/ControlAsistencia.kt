package org.utl.pruebaproyecto.ui.control

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import org.utl.pruebaproyecto.databinding.FragmentControlAsistenciaBinding

class ControlAsistencia : Fragment() {

    private var _binding: FragmentControlAsistenciaBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentControlAsistenciaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        setupSwitches()
    }

    private fun setupClickListeners() {
        // Botón de regreso
//        binding.btnBack.setOnClickListener {
//            requireActivity().onBackPressed()
//        }

        // Menú de acciones
        binding.btnMenuAcciones.setOnClickListener {
            showActionsMenu()
        }

        // Botón aplicar recomendación
        binding.btnAplicarRecomendacion.setOnClickListener {
            applyRecommendation()
        }
    }

    private fun setupSwitches() {
        // Switch Encender Ventilador
        binding.switchVentilador.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(requireContext(), "Ventilador encendido", Toast.LENGTH_SHORT).show()
                // Aquí puedes agregar la lógica para encender el ventilador
                encenderVentilador()
            } else {
                Toast.makeText(requireContext(), "Ventilador apagado", Toast.LENGTH_SHORT).show()
                // Aquí puedes agregar la lógica para apagar el ventilador
                apagarVentilador()
            }
        }

        // Switch Activar sistema de riego
        binding.switchRiego.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(requireContext(), "Sistema de riego activado", Toast.LENGTH_SHORT).show()
                activarSistemaRiego()
            } else {
                Toast.makeText(requireContext(), "Sistema de riego desactivado", Toast.LENGTH_SHORT).show()
                desactivarSistemaRiego()
            }
        }

        // Switch Activar sistema de mezcla
        binding.switchMezcla.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(requireContext(), "Sistema de mezcla activado", Toast.LENGTH_SHORT).show()
                activarSistemaMezcla()
            } else {
                Toast.makeText(requireContext(), "Sistema de mezcla desactivado", Toast.LENGTH_SHORT).show()
                desactivarSistemaMezcla()
            }
        }
    }

    private fun showActionsMenu() {
        // Aquí puedes implementar un menú desplegable o navegar a otra pantalla
        Toast.makeText(requireContext(), "Menú de acciones", Toast.LENGTH_SHORT).show()
    }

    private fun applyRecommendation() {
        // Lógica para aplicar la recomendación
        Toast.makeText(requireContext(), "Aplicando recomendación: Agregar agua", Toast.LENGTH_SHORT).show()

        // Ejemplo: activar automáticamente el sistema de riego
        binding.switchRiego.isChecked = true
    }

    // Métodos para controlar los dispositivos
    private fun encenderVentilador() {
        // Aquí implementarías la comunicación con el hardware/API
        // Por ejemplo: enviar comando al microcontrolador
    }

    private fun apagarVentilador() {
        // Lógica para apagar ventilador
    }

    private fun activarSistemaRiego() {
        // Lógica para activar sistema de riego
    }

    private fun desactivarSistemaRiego() {
        // Lógica para desactivar sistema de riego
    }

    private fun activarSistemaMezcla() {
        // Lógica para activar sistema de mezcla
    }

    private fun desactivarSistemaMezcla() {
        // Lógica para desactivar sistema de mezcla
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}