package org.utl.pruebaproyecto.ui.control

import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.MqttGlobalPublishFilter
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import org.utl.pruebaproyecto.MainActivity
import org.utl.pruebaproyecto.R
import org.utl.pruebaproyecto.databinding.FragmentControlAsistenciaBinding
import org.json.JSONObject
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

class ControlAsistencia : Fragment() {

    private var _binding: FragmentControlAsistenciaBinding? = null
    private val binding get() = _binding!!

    private lateinit var mqttClient: Mqtt5AsyncClient
    private val handler = Handler(Looper.getMainLooper())

    // Canal de notificación para controles manuales
    private val CONTROL_CHANNEL_ID = "control_manual_notifications"

    // Constantes para el tiempo de bloqueo (en milisegundos)
    private val TIEMPO_BLOQUEO_VENTILADOR = 10000L // 10 segundos
    private val TIEMPO_BLOQUEO_BOMBA = 4000L // 4 segundos

    // Variables para controlar el estado de los switches
    private var ventiladorBloqueado = false
    private var bombaBloqueada = false

    // Variables para monitoreo de humedad del suelo
    private var humedadSueloActual: Double = -1.0 // -1 indica que no hay datos aún
    private val UMBRAL_HUMEDAD_ALTA = 60.0 // Umbral para considerar sobre-hidratación
    private var datosRecibidos = false // Flag para saber si ya recibimos datos del MQTT
    private var switchPendiente: CompoundButton? = null // Switch que está esperando datos
    private lateinit var iconoEstado: ImageView
    private lateinit var textoEstado: TextView
    private lateinit var tvRecomendacion: TextView


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentControlAsistenciaBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        createNotificationChannel()
        setupMqttClient()
        setupSwitches()
        tvRecomendacion = view.findViewById(R.id.tv_recomendacion)
        iconoEstado = view.findViewById(R.id.icono_estado)
        textoEstado = view.findViewById(R.id.tv_estado_composta)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun setupMqttClient() {
        try {
            mqttClient = MqttClient.builder()
                .useMqttVersion5()
                .serverHost("cf75cf39e5f2479d81724999059bc7b9.s1.eu.hivemq.cloud")
                .serverPort(8883)
                .sslWithDefaultConfig()
                .buildAsync()

            mqttClient.connectWith()
                .simpleAuth()
                .username("Alejandro")
                .password("Telefono123!".toByteArray(StandardCharsets.UTF_8))
                .applySimpleAuth()
                .send()
                .whenComplete { _, throwable ->
                    if (throwable != null) {
                        Log.e("MQTT_CONTROL", "Error conectando MQTT", throwable)
                        handler.post {
                            Toast.makeText(requireContext(), "Error de conexión MQTT", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.i("MQTT_CONTROL", "✅ MQTT conectado para control")
                        suscribirASensores()
                    }
                }

        } catch (e: Exception) {
            Log.e("MQTT_CONTROL", "Error inicializando MQTT", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun suscribirASensores() {
        try {
            // Suscribirse al tópico de sensores para obtener datos de humedad del suelo
            mqttClient.subscribeWith()
                .topicFilter("composta/sensores")
                .qos(MqttQos.AT_LEAST_ONCE)
                .send()
                .whenComplete { _, throwable ->
                    if (throwable != null) {
                        Log.e("MQTT_CONTROL", "Error suscribiéndose a sensores", throwable)
                    } else {
                        Log.i("MQTT_CONTROL", "✅ Suscrito a sensores")
                    }
                }

            // Escuchar mensajes de sensores
            mqttClient.toAsync().publishes(MqttGlobalPublishFilter.ALL) { publish ->
                val payload = String(publish.payloadAsBytes, StandardCharsets.UTF_8)
                handler.post {
                    procesarDatosSensores(payload)
                }
            }

        } catch (e: Exception) {
            Log.e("MQTT_CONTROL", "Error suscribiéndose a sensores", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun procesarDatosSensores(jsonString: String) {
        try {
            val json = JSONObject(jsonString)
            humedadSueloActual = json.getDouble("humedad_suelo")
            val temp = json.getDouble("temperatura")
            val nh3 = json.getDouble("NH3")
            val ch4 = json.getDouble("CH4")
            val co2 = json.getDouble("CO2")
            actualizarEstadoComposta(temp, nh3, ch4, co2,humedadSueloActual)

            datosRecibidos = true
            Log.d("MQTT_CONTROL", "Humedad del suelo actualizada: $humedadSueloActual%")

            // Si hay un switch esperando datos, procesarlo ahora
            switchPendiente?.let { switch ->
                verificarHumedadYActivarBomba(switch)
                switchPendiente = null
            }

        } catch (e: Exception) {
            Log.e("MQTT_CONTROL", "Error procesando datos de sensores", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CONTROL_CHANNEL_ID,
                "Control Manual",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones de control manual de dispositivos"
                enableLights(true)
                lightColor = Color.BLUE
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 100, 200)
            }

            val manager = requireContext().getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }



    @RequiresApi(Build.VERSION_CODES.N)
    private fun setupSwitches() {
        iconoEstado = requireView().findViewById(R.id.icono_estado)
        textoEstado = requireView().findViewById(R.id.tv_estado_composta)


        // Switch Encender Ventilador - SIN CAMBIOS
        binding.switchVentilador.setOnCheckedChangeListener { switch, isChecked ->
            if (isChecked && !ventiladorBloqueado) {
                // Activar ventilador
                ventiladorBloqueado = true
                switch.isEnabled = false // Deshabilitar switch

                // Marcar control manual y mostrar notificación personalizada
                marcarControlManual("ventilador")
                mostrarNotificacionControlManual(
                    " Control Manual - Ventilador",
                    "Ventilador activado manualmente por ${TIEMPO_BLOQUEO_VENTILADOR/1000} segundos",
                    "ventilador"
                )

                Toast.makeText(requireContext(), "Ventilador activado manualmente por ${TIEMPO_BLOQUEO_VENTILADOR/1000} segundos", Toast.LENGTH_SHORT).show()

                // Enviar comando MQTT
                enviarComandoMqtt("composta/ventilador", "1")

                // Programar desactivación automática
                handler.postDelayed({
                    if (_binding != null) { // Verificar que el fragment aún existe
                        // Desbloquear primero para permitir el cambio
                        ventiladorBloqueado = false
                        binding.switchVentilador.isEnabled = true

                        // Cambiar a desactivado
                        binding.switchVentilador.isChecked = false

                        // Notificación de desactivación
                        mostrarNotificacionControlManual(
                            "Control Manual - Ventilador",
                            "Ventilador desactivado automáticamente",
                            "ventilador"
                        )

                        Toast.makeText(requireContext(), "Ventilador desactivado automáticamente", Toast.LENGTH_SHORT).show()
                    }
                }, TIEMPO_BLOQUEO_VENTILADOR)

            } else if (!isChecked && ventiladorBloqueado) {
                // Si está bloqueado, evitar que se desactive manualmente
                switch.isChecked = true
            }
        }

        // Switch Activar sistema de riego (bomba) - MODIFICADO
        binding.switchRiego.setOnCheckedChangeListener { switch, isChecked ->
            if (isChecked && !bombaBloqueada) {
                // Verificar si tenemos datos del MQTT
                if (!datosRecibidos || humedadSueloActual < 0) {
                    // No tenemos datos aún, mostrar mensaje de espera
                    mostrarMensajeEsperandoDatos(switch)
                } else {
                    // Tenemos datos, proceder normalmente
                    verificarHumedadYActivarBomba(switch)
                }
            } else if (!isChecked && bombaBloqueada) {
                // Si está bloqueado, evitar que se desactive manualmente
                switch.isChecked = true
            }
        }


    }

    private fun mostrarMensajeEsperandoDatos(switch: CompoundButton) {
        // Desactivar el switch temporalmente
        switch.isChecked = false
        switchPendiente = switch

        Toast.makeText(
            requireContext(),
            "⏳ Obteniendo datos de sensores...",
            Toast.LENGTH_SHORT
        ).show()


        // Timeout por si no llegan datos en 10 segundos
        handler.postDelayed({
            if (switchPendiente != null) {
                switchPendiente = null
                handler.post {
                    Toast.makeText(
                        requireContext(),
                        "❌ No se pudieron obtener datos de sensores",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }, 10000)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun verificarHumedadYActivarBomba(switch: CompoundButton) {
        when {
            humedadSueloActual >= UMBRAL_HUMEDAD_ALTA -> {
                // Sobre-hidratación: No permitir activación
                switch.isChecked = false
                mostrarNotificacionSobreHidratacion()
                Toast.makeText(
                    requireContext(),
                    "Sobre-hidratación detectada (${humedadSueloActual.toInt()}%). Riego no disponible",
                    Toast.LENGTH_LONG
                ).show()
            }
            else -> {
                // Cualquier otro nivel: Siempre mostrar diálogo de confirmación
                mostrarDialogoConfirmacionRiego(switch)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun mostrarDialogoConfirmacionRiego(switch: CompoundButton) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_confirmacion_riego, null)
        val tvMensaje = dialogView.findViewById<TextView>(R.id.tvMensaje)
        val btnAceptar = dialogView.findViewById<Button>(R.id.btnAceptar)
        val btnCancelar = dialogView.findViewById<Button>(R.id.btnCancelar)

        tvMensaje.text = "La humedad del suelo es del ${humedadSueloActual.toInt()}%.\n¿Desea activar el sistema de riego?"

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnAceptar.setOnClickListener {
            activarBombaDirectamente(switch)
            dialog.dismiss()
        }

        btnCancelar.setOnClickListener {
            switch.isChecked = false
            Toast.makeText(requireContext(), "El riego fue cancelado", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }



    @RequiresApi(Build.VERSION_CODES.N)
    private fun activarBombaDirectamente(switch: CompoundButton) {
        // Activar bomba
        bombaBloqueada = true
        switch.isEnabled = false // Deshabilitar switch

        // Marcar control manual y mostrar notificación personalizada
        marcarControlManual("bomba")
        mostrarNotificacionControlManual(
            "💧 Control Manual - Riego",
            "Sistema de riego activado (Humedad: ${humedadSueloActual.toInt()}%) por ${TIEMPO_BLOQUEO_BOMBA/1000} segundos",
            "bomba"
        )

        Toast.makeText(
            requireContext(),
            "💧 Sistema de riego activado por ${TIEMPO_BLOQUEO_BOMBA/1000} segundos",
            Toast.LENGTH_SHORT
        ).show()

        // Enviar comando MQTT
        enviarComandoMqtt("composta/bomba", "1")

        // Programar desactivación automática
        handler.postDelayed({
            if (_binding != null) { // Verificar que el fragment aún existe
                // Desbloquear primero para permitir el cambio
                bombaBloqueada = false
                binding.switchRiego.isEnabled = true

                // Cambiar a desactivado
                binding.switchRiego.isChecked = false

                // Notificación de desactivación
                mostrarNotificacionControlManual(
                    "💧 Control Manual - Riego",
                    "Sistema de riego desactivado automáticamente",
                    "bomba"
                )

                Toast.makeText(requireContext(), "Sistema de riego desactivado automáticamente", Toast.LENGTH_SHORT).show()
            }
        }, TIEMPO_BLOQUEO_BOMBA)
    }

    private fun mostrarNotificacionSobreHidratacion() {
        try {
            val intent = Intent(requireContext(), MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("redirect", "control_asistencia")
            }

            val pendingIntent = PendingIntent.getActivity(
                requireContext(),
                System.currentTimeMillis().toInt(),
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val notification = NotificationCompat.Builder(requireContext(), CONTROL_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_logo_micomposta)
                .setContentTitle("⚠️ Sobre-hidratación Detectada")
                .setContentText("Humedad del suelo: ${humedadSueloActual.toInt()}%. Riego no disponible por seguridad.")
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("La humedad del suelo está muy alta (${humedadSueloActual.toInt()}%). El sistema de riego se ha deshabilitado para evitar sobre-hidratación. Espere a que la humedad disminuya."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setColor(Color.RED)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setVibrate(longArrayOf(0, 300, 200, 300, 200, 300))
                .build()

            val manager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(System.currentTimeMillis().toInt(), notification)

        } catch (e: Exception) {
            Log.e("NOTIF_CONTROL", "Error mostrando notificación de sobre-hidratación", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun enviarComandoMqtt(topic: String, mensaje: String) {
        try {
            if (::mqttClient.isInitialized) {
                mqttClient.publishWith()
                    .topic(topic)
                    .payload(mensaje.toByteArray(StandardCharsets.UTF_8))
                    .qos(MqttQos.AT_LEAST_ONCE)
                    .send()
                    .whenComplete { _, throwable ->
                        if (throwable != null) {
                            Log.e("MQTT_CONTROL", "Error enviando mensaje a $topic", throwable)
                            handler.post {
                                Toast.makeText(requireContext(), "Error enviando comando", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Log.i("MQTT_CONTROL", "✅ Comando enviado: $topic = $mensaje")
                        }
                    }
            } else {
                Log.e("MQTT_CONTROL", "Cliente MQTT no inicializado")
                Toast.makeText(requireContext(), "Error: MQTT no conectado", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("MQTT_CONTROL", "Error enviando comando MQTT", e)
            Toast.makeText(requireContext(), "Error enviando comando", Toast.LENGTH_SHORT).show()
        }
    }

    private fun marcarControlManual(dispositivo: String) {
        try {
            val file = File(requireContext().filesDir, "control_manual_activo.json")
            val json = if (file.exists()) {
                JSONObject(file.readText())
            } else {
                JSONObject()
            }

            json.put("${dispositivo}_timestamp", System.currentTimeMillis())
            file.writeText(json.toString())

            Log.d("CONTROL_MANUAL", "🎮 Marcado control manual para $dispositivo")
        } catch (e: Exception) {
            Log.e("CONTROL_MANUAL", "Error marcando control manual", e)
        }
    }

    private fun mostrarNotificacionControlManual(titulo: String, mensaje: String, tipo: String) {
        try {
            val intent = Intent(requireContext(), MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("redirect", "control_asistencia")
            }

            val pendingIntent = PendingIntent.getActivity(
                requireContext(),
                System.currentTimeMillis().toInt(),
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val icono = R.drawable.ic_logo_micomposta
            val sonido = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val notification = NotificationCompat.Builder(requireContext(), CONTROL_CHANNEL_ID)
                .setSmallIcon(icono)
                .setContentTitle(titulo)
                .setContentText(mensaje)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setSound(sonido)
                .setVibrate(longArrayOf(0, 200, 100, 200))
                .setColor(Color.BLUE)
                .build()

            val manager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(System.currentTimeMillis().toInt(), notification)

        } catch (e: Exception) {
            Log.e("NOTIF_CONTROL", "Error mostrando notificación de control", e)
        }
    }

    private fun showActionsMenu() {
        // Aquí puedes implementar un menú desplegable o navegar a otra pantalla
        Toast.makeText(requireContext(), "Menú de acciones", Toast.LENGTH_SHORT).show()
    }

    private fun applyRecommendation() {
        // Lógica para aplicar la recomendación - NO TOCAR POR AHORA
        Toast.makeText(requireContext(), "Aplicando recomendación: Agregar agua", Toast.LENGTH_SHORT).show()

        // Ejemplo: activar automáticamente el sistema de riego
        binding.switchRiego.isChecked = true
    }

    // Métodos para controlar los dispositivos - SIN CAMBIOS POR AHORA
    private fun activarSistemaMezcla() {
        // Lógica para activar sistema de mezcla
    }

    private fun desactivarSistemaMezcla() {
        // Lógica para desactivar sistema de mezcla
    }

    private fun actualizarEstadoComposta(temperatura: Double, nh3: Double, ch4: Double, co2: Double, humedadSuelo: Double) {
        val esCritico = temperatura > 45 || nh3 > 5.0 || ch4 > 20.0 || co2 > 1000.0
        val esAtencion = temperatura > 35 || nh3 > 4.0 || ch4 > 15.0 || co2 > 800.0
        val esSobrehidratacion = humedadSuelo > 60.0

        val recomendacion = when {
            humedadSuelo > 80.0 -> "💧 Exceso severo de humedad, detener riego"
            humedadSuelo > 70.0 -> "💧 Humedad muy alta, revisar reducir riego"
            humedadSuelo > 60.0 -> "💧 Sobre-hidratación leve, monitorear riego"
            temperatura > 45 -> "🌡️ Temperatura muy alta, ventilar"
            nh3 > 5.0 -> "🧪 Nivel alto de amoníaco, ventilar"
            ch4 > 20.0 -> "🧪 Metano elevado, ventilar"
            co2 > 1000.0 -> "🧪 CO₂ elevado, aumentar oxigenación"
            temperatura > 35 -> "🌡️ Temperatura en aumento, encender ventilador"
            nh3 > 4.0 -> "🧪 Amoníaco presente, posible ventilación"
            ch4 > 15.0 -> "🧪 Metano detectable, posible ventilación"
            co2 > 800.0 -> "🧪 CO₂ subiendo, ventilar si es necesario"
            else -> "✅ Todo en orden, monitoreando...."
        }

        when {
            esCritico -> {
                iconoEstado.setImageResource(R.drawable.ic_alerta_roja)
                textoEstado.text = "Crítico"
                textoEstado.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
            }
            esAtencion || esSobrehidratacion -> {
                iconoEstado.setImageResource(R.drawable.ic_alerta_amarilla)
                textoEstado.text = "Atención"
                textoEstado.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark))
            }
            else -> {
                iconoEstado.setImageResource(R.drawable.ic_alerta_verde)
                textoEstado.text = "Normal"
                textoEstado.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))
            }
        }

        tvRecomendacion.text = recomendacion
    }





    override fun onDestroyView() {
        super.onDestroyView()

        // Limpiar handlers pendientes
        handler.removeCallbacksAndMessages(null)
        switchPendiente = null

        // Desconectar MQTT
        try {
            if (::mqttClient.isInitialized) {
                mqttClient.disconnect()
            }
        } catch (e: Exception) {
            Log.e("MQTT_CONTROL", "Error desconectando MQTT", e)
        }

        _binding = null
    }
}