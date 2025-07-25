package org.utl.pruebaproyecto.ui.monitoreo

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import org.utl.pruebaproyecto.R

class Monitoreo : Fragment() {

    private var param1: String? = null
    private var param2: String? = null
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout
        return inflater.inflate(R.layout.fragment_monitoreo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Referencia a la ProgressBar
        progressBar = view.findViewById(R.id.progressBar)

        // Lógica para cambiar el progreso (simulación)
        val porcentaje = calcularProgresoSimulado()
        animarProgreso(porcentaje)
    }

    private fun calcularProgresoSimulado(): Int {
        // Aquí puedes poner tu lógica real, este es solo un ejemplo
        return 64
    }

    private fun animarProgreso(valor: Int) {
        ObjectAnimator.ofInt(progressBar, "progress", 0, valor)
            .setDuration(1000)
            .start()
    }

    companion object {
        private const val ARG_PARAM1 = "param1"
        private const val ARG_PARAM2 = "param2"

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            Monitoreo().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
