package org.utl.pruebaproyecto.ui.monitoreo

import android.animation.ObjectAnimator
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.MqttGlobalPublishFilter
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient
import org.utl.pruebaproyecto.R
import org.json.JSONObject
import java.nio.charset.StandardCharsets

class Monitoreo : Fragment() {
    private lateinit var progressBar: ProgressBar
    private lateinit var tvProgress: TextView
    private lateinit var tvTemperatura: TextView
    private lateinit var tvHumedad: TextView
    private lateinit var tvHumedadSuelo: TextView
    private lateinit var tvNivelNH3: TextView
    private lateinit var tvNivelCH4: TextView
    private lateinit var tvNivelCO2: TextView
    private lateinit var mqttClient: Mqtt5BlockingClient


    private val UMBRAL_NH3 = 4f
    private val UMBRAL_CH4 = 15.0f
    private val UMBRAL_CO2 = 800.0f

    private val DURACION_TOTAL_DIAS = 60
    private val PREFS_NAME = "com.monitoreo"
    private val PREF_FECHA_INICIO = "fecha_inicio"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_monitoreo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressBar = view.findViewById(R.id.progressBar)
        tvProgress = view.findViewById(R.id.tvProgress)
        tvTemperatura = view.findViewById(R.id.tvTemperatura)
        tvHumedad = view.findViewById(R.id.tvHumedad)
        tvHumedadSuelo = view.findViewById(R.id.tvHumedadSuelo)
        tvNivelNH3 = view.findViewById(R.id.tvNivelNH3)
        tvNivelCH4 = view.findViewById(R.id.tvNivelCH4)
        tvNivelCO2 = view.findViewById(R.id.tvNivelCO2)

        setupFechaInicioSiNoExiste()
        setupMqttClient()
    }

    private fun setupFechaInicioSiNoExiste() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains(PREF_FECHA_INICIO)) {
            val fechaActual = System.currentTimeMillis()
            val veinteDiasEnMillis = 0L * 24 * 60 * 60 * 1000
            val fechaInicioSimulada = fechaActual - veinteDiasEnMillis
            prefs.edit().putLong(PREF_FECHA_INICIO, fechaInicioSimulada).apply()
        }
    }

    private fun obtenerDiasTranscurridos(): Int {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val fechaInicio = prefs.getLong(PREF_FECHA_INICIO, System.currentTimeMillis())
        val ahora = System.currentTimeMillis()
        val diferencia = ahora - fechaInicio
        return (diferencia / (1000 * 60 * 60 * 24)).toInt()
    }

    private fun calcularProgresoConFecha(): Int {
        val dias = obtenerDiasTranscurridos()
        return (dias * 100 / DURACION_TOTAL_DIAS).coerceIn(0, 100)
    }

    private fun setupMqttClient() {
        try {
            mqttClient = MqttClient.builder()
                .useMqttVersion5()
                .serverHost("cf75cf39e5f2479d81724999059bc7b9.s1.eu.hivemq.cloud")
                .serverPort(8883)
                .sslWithDefaultConfig()
                .buildBlocking()

            val connAck = mqttClient.connectWith()
                .simpleAuth()
                .username("Alejandro")
                .password("Telefono123!".toByteArray(StandardCharsets.UTF_8))
                .applySimpleAuth()
                .send()

            if (connAck.reasonCode.isError) {
                Log.e("MQTT", "Error de conexión: ${connAck.reasonCode}")
                mostrarValoresPorDefecto()
                return
            }

            mqttClient.subscribeWith()
                .topicFilter("composta/sensores")
                .qos(MqttQos.AT_LEAST_ONCE)
                .send()

            mqttClient.toAsync().publishes(MqttGlobalPublishFilter.ALL) { publish ->
                val payload = String(publish.payloadAsBytes, StandardCharsets.UTF_8)
                activity?.runOnUiThread {
                    actualizarDatos(payload)
                }
            }

        } catch (e: Exception) {
            Log.e("MQTT", "Error en MQTT", e)
            mostrarValoresPorDefecto()
        }
    }

    fun actualizarDatos(jsonString: String) {
        try {
            val json = JSONObject(jsonString)

            val temperatura = json.getDouble("temperatura")
            val humedad = json.getDouble("humedad")
            val nh3 = json.getDouble("NH3")
            val ch4 = json.getDouble("CH4")
            val co2 = json.getDouble("CO2")
            val humedadSuelo = json.getDouble("humedad_suelo")

            tvTemperatura.text = "${temperatura}°C"
            tvHumedad.text = "${humedad}%"

            tvHumedadSuelo.text = "${humedadSuelo}%"
            val colorHumedadSuelo = when {
                humedadSuelo < 20 -> android.R.color.holo_orange_dark
                humedadSuelo in 20.0..60.0 -> android.R.color.holo_green_dark
                else -> android.R.color.holo_red_dark
            }
            tvHumedadSuelo.setTextColor(ContextCompat.getColor(requireContext(), colorHumedadSuelo))

            actualizarNivelGas(tvNivelNH3, nh3.toFloat(), UMBRAL_NH3)
            actualizarNivelGas(tvNivelCH4, ch4.toFloat(), UMBRAL_CH4)
            actualizarNivelGas(tvNivelCO2, co2.toFloat(), UMBRAL_CO2)

            val porcentaje = calcularProgresoConFecha()
            actualizarProgreso(porcentaje)

        } catch (e: Exception) {
            Log.e("MQTT", "Error al procesar datos", e)
            mostrarValoresPorDefecto()
        }
    }

    private fun actualizarNivelGas(textView: TextView, valor: Float, umbral: Float) {
        if (valor > umbral) {
            textView.text = "ALTO"
            textView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
        } else {
            textView.text = "ESTABLE"
            textView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))
        }
    }

    private fun actualizarProgreso(porcentaje: Int) {
        ObjectAnimator.ofInt(progressBar, "progress", porcentaje)
            .setDuration(1000)
            .start()

        tvProgress.text = "${porcentaje}%"

        val color = when {
            porcentaje >= 80 -> android.R.color.holo_green_dark
            porcentaje >= 60 -> android.R.color.holo_orange_dark
            else -> android.R.color.holo_red_dark
        }
        tvProgress.setTextColor(ContextCompat.getColor(requireContext(), color))
    }

    private fun mostrarValoresPorDefecto() {
        tvTemperatura.text = "-- °C"
        tvHumedad.text = "-- %"
        tvHumedadSuelo.text = "-- %"
        tvNivelNH3.text = "SIN DATOS"
        tvNivelCH4.text = "SIN DATOS"
        tvNivelCO2.text = "SIN DATOS"

        val gris = ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
        tvNivelNH3.setTextColor(gris)
        tvNivelCH4.setTextColor(gris)
        tvNivelCO2.setTextColor(gris)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            mqttClient.disconnect()
        } catch (e: Exception) {
            Log.e("MQTT", "Error al desconectar", e)
        }
    }

    companion object {
        fun newInstance() = Monitoreo()
    }
}
