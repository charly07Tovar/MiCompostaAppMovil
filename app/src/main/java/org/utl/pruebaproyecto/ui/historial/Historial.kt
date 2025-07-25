package org.utl.pruebaproyecto.ui.historial

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import org.utl.pruebaproyecto.R
import java.text.SimpleDateFormat
import java.util.*

class Historial : Fragment() {

    private lateinit var tvDateRange: TextView
    private lateinit var layoutDateSelector: LinearLayout

    private var startDate: Calendar = Calendar.getInstance()
    private var endDate: Calendar = Calendar.getInstance()

    private val dateFormat = SimpleDateFormat("dd MMM. yyyy", Locale("es", "ES"))

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_historial, container, false)

        initViews(root)
        setupClickListeners()
        updateDateRange()

        return root
    }

    private fun initViews(root: View) {
        tvDateRange = root.findViewById(R.id.tvDateRange)
        layoutDateSelector = root.findViewById(R.id.layoutDateSelector)

        // Configurar fechas por defecto (última semana)
        startDate.add(Calendar.DAY_OF_MONTH, -7)
        // endDate ya está en hoy
    }

    private fun setupClickListeners() {
        layoutDateSelector.setOnClickListener {
            showDateRangePicker()
        }
    }

    private fun showDateRangePicker() {
        // Selector de fecha inicial
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                startDate.set(year, month, dayOfMonth)
                showEndDatePicker()
            },
            startDate.get(Calendar.YEAR),
            startDate.get(Calendar.MONTH),
            startDate.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showEndDatePicker() {
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                endDate.set(year, month, dayOfMonth)
                updateDateRange()
                // Aquí podrías llamar a actualizar los datos del gráfico
                loadHistorialData()
            },
            endDate.get(Calendar.YEAR),
            endDate.get(Calendar.MONTH),
            endDate.get(Calendar.DAY_OF_MONTH)
        ).apply {
            // La fecha final no puede ser anterior a la inicial
            datePicker.minDate = startDate.timeInMillis
        }.show()
    }

    private fun updateDateRange() {
        val startDateStr = dateFormat.format(startDate.time)
        val endDateStr = dateFormat.format(endDate.time)
        tvDateRange.text = "$startDateStr → $endDateStr"
    }

    private fun loadHistorialData() {
        // Aquí cargarías los datos reales desde tu API o base de datos
        // Por ejemplo:
        // viewModel.loadHistorialData(startDate.time, endDate.time)

        // Por ahora solo mostramos un mensaje
        // Toast.makeText(requireContext(), "Cargando datos...", Toast.LENGTH_SHORT).show()
    }
}