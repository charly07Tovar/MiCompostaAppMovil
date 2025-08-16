package org.utl.pruebaproyecto.ui.notificaciones

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.MqttGlobalPublishFilter
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient
import org.utl.pruebaproyecto.MainActivity
import org.utl.pruebaproyecto.R
import org.json.JSONObject
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.Executors

class NotificacionService : Service() {

    private lateinit var mqttClient: Mqtt5BlockingClient
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val CHANNEL_ID = "composta_notifications"
    private val FOREGROUND_CHANNEL_ID = "composta_service" // Canal para el servicio persistente
    private val FOREGROUND_NOTIFICATION_ID = 1
    private val ESTADO_FILE = "ultimo_estado_sensores.json"
    private val CONTROL_MANUAL_FILE = "control_manual_activo.json"

    // Variables para controlar el estado anterior y evitar notificaciones duplicadas
    private var ultimoEstadoVentilador = -1
    private var ultimoEstadoBomba = -1
    private var ultimoMotivo = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        cargarUltimoEstado() // Cargar el estado previo antes de iniciar
        startForegroundService()
        setupMqttClient()
    }

    // ✨ NUEVAS FUNCIONES PARA PERSISTENCIA DEL ESTADO
    private fun cargarUltimoEstado() {
        try {
            val file = File(filesDir, ESTADO_FILE)
            if (file.exists()) {
                val jsonString = file.readText()
                val json = JSONObject(jsonString)

                ultimoEstadoVentilador = json.optInt("ventilador", -1)
                ultimoEstadoBomba = json.optInt("bomba", -1)
                ultimoMotivo = json.optString("motivo_ventilador", "")

                Log.d("ESTADO_JSON", "✅ Estado cargado: Ventilador=$ultimoEstadoVentilador, Bomba=$ultimoEstadoBomba, Motivo=$ultimoMotivo")
            } else {
                Log.d("ESTADO_JSON", "📄 No existe archivo de estado previo, iniciando desde cero")
            }
        } catch (e: Exception) {
            Log.e("ESTADO_JSON", "❌ Error al cargar estado previo", e)
            // Mantener valores por defecto en caso de error
            ultimoEstadoVentilador = -1
            ultimoEstadoBomba = -1
            ultimoMotivo = ""
        }
    }

    // ✨ FUNCIÓN PARA VERIFICAR SI HAY CONTROL MANUAL ACTIVO
    private fun esControlManual(dispositivo: String): Boolean {
        return try {
            val file = File(filesDir, CONTROL_MANUAL_FILE)
            if (file.exists()) {
                val jsonString = file.readText()
                val json = JSONObject(jsonString)
                val timestamp = json.optLong("${dispositivo}_timestamp", 0)
                val ahora = System.currentTimeMillis()

                // Considerar control manual si han pasado menos de 30 segundos
                val esManual = (ahora - timestamp) < 30000

                if (esManual) {
                    Log.d("CONTROL_MANUAL", "🎮 Control manual activo para $dispositivo")
                }

                esManual
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("CONTROL_MANUAL", "Error verificando control manual", e)
            false
        }
    }

    private fun guardarUltimoEstado() {
        try {
            val json = JSONObject().apply {
                put("ventilador", ultimoEstadoVentilador)
                put("bomba", ultimoEstadoBomba)
                put("motivo_ventilador", ultimoMotivo)
                put("timestamp", System.currentTimeMillis())
            }

            val file = File(filesDir, ESTADO_FILE)
            file.writeText(json.toString())

            Log.d("ESTADO_JSON", "💾 Estado guardado: ${json.toString()}")
        } catch (e: Exception) {
            Log.e("ESTADO_JSON", "❌ Error al guardar estado", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY // El sistema reiniciará el servicio si es terminado
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Canal para alertas del compostaje
            val alertChannel = NotificationChannel(
                CHANNEL_ID,
                "Alertas del Compostaje",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones importantes del sistema"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 300, 500)
            }

            // Canal para el servicio persistente (menos intrusivo)
            val serviceChannel = NotificationChannel(
                FOREGROUND_CHANNEL_ID,
                "Servicio de Monitoreo",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Servicio ejecutándose en segundo plano"
                enableLights(false)
                enableVibration(false)
            }

            notificationManager.createNotificationChannel(alertChannel)
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }

    private fun startForegroundService() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )




    }

    private fun setupMqttClient() {
        try {
            Log.d("MQTT_DEBUG", "Iniciando configuración cliente MQTT...")

            mqttClient = MqttClient.builder()
                .useMqttVersion5()
                .serverHost("cf75cf39e5f2479d81724999059bc7b9.s1.eu.hivemq.cloud")
                .serverPort(8883)
                .sslWithDefaultConfig()
                .buildBlocking()

            Log.d("MQTT_DEBUG", "Cliente MQTT creado, intentando conectar...")

            mqttClient.connectWith()
                .simpleAuth()
                .username("Alejandro")
                .password("Telefono123!".toByteArray(StandardCharsets.UTF_8))
                .applySimpleAuth()
                .send()

            Log.i("MQTT_SUCCESS", "✅ Conexión MQTT establecida exitosamente")

            mqttClient.subscribeWith()
                .topicFilter("composta/sensores")
                .qos(MqttQos.AT_LEAST_ONCE)
                .send()

            Log.i("MQTT_SUCCESS", "✅ Suscrito al topic: composta/sensores")

            // Ejecutor para manejar mensajes en background
            val executor = Executors.newSingleThreadExecutor()

            mqttClient.toAsync().publishes(MqttGlobalPublishFilter.ALL) { publish ->
                executor.execute {
                    try {
                        val payload = String(publish.payloadAsBytes, StandardCharsets.UTF_8)
                        Log.d("MQTT_MESSAGE", "📨 Mensaje recibido: $payload")
                        procesarMensaje(payload)
                    } catch (e: Exception) {
                        Log.e("MQTT_ERROR", "Error procesando mensaje", e)
                    }
                }
            }

        } catch (e: Exception) {
            Log.e("MQTT_ERROR", "❌ Error en conexión MQTT: ${e.message}", e)

            if (e.message?.contains("NOT_AUTHORIZED") == true) {
                Log.e("MQTT_ERROR", "🚫 Credenciales MQTT incorrectas - revisar usuario/contraseña")
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    Log.d("MQTT_DEBUG", "🔄 Reintentando conexión MQTT...")
                    setupMqttClient()
                }, 30000) // 30 segundos
            } else {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    Log.d("MQTT_DEBUG", "🔄 Reintentando conexión MQTT...")
                    setupMqttClient()
                }, 5000)
            }
        }
    }

    private fun procesarMensaje(jsonString: String) {
        try {
            val json = JSONObject(jsonString)

            // Obtener datos de sensores
            val estadoVentilador = json.optInt("ventilador", 0)
            val estadoBomba = json.optInt("bomba", 0)
            val motivoVentilador = json.optString("motivo_ventilador", "")

            // Procesar notificaciones del ventilador
            if (debeNotificarVentilador(estadoVentilador, motivoVentilador) && !esControlManual("ventilador")) {
                val motivo = obtenerMotivoVentilador(motivoVentilador)
                mostrarNotificacion(motivo, estadoVentilador, "ventilador")
                guardarNotificacion(motivo, estadoVentilador, "ventilador")

                // Actualizar estado anterior
                ultimoEstadoVentilador = estadoVentilador
                ultimoMotivo = motivoVentilador
                guardarUltimoEstado() // Guardar estado en JSON
            }

            // Procesar notificaciones de la bomba
            if (debeNotificarBomba(estadoBomba) && !esControlManual("bomba")) {
                val motivo = obtenerMotivoBomba(estadoBomba)
                val titulo = if (estadoBomba == 1) "Bomba activada" else "Bomba desactivada"
                mostrarNotificacion(motivo, estadoBomba, "bomba")
                guardarNotificacion(motivo, estadoBomba, "bomba")

                // Actualizar estado anterior
                ultimoEstadoBomba = estadoBomba
                guardarUltimoEstado() // Guardar estado en JSON
            }

        } catch (e: Exception) {
            Log.e("PROCESAR_MSG", "Error al procesar mensaje", e)
        }
    }

    private fun debeNotificarVentilador(estadoActual: Int, motivoActual: String): Boolean {
        // Notificar solo si:
        // 1. El ventilador cambió de estado (0->1 o 1->0)
        // 2. El motivo cambió aunque el estado sea el mismo (solo si está encendido)
        return (estadoActual != ultimoEstadoVentilador) ||
                (motivoActual != ultimoMotivo && estadoActual == 1)
    }

    private fun debeNotificarBomba(estadoActual: Int): Boolean {
        // Notificar solo si la bomba cambió de estado (0->1 o 1->0)
        return (estadoActual != ultimoEstadoBomba)
    }

    private fun obtenerMotivoVentilador(motivoRaw: String): String {
        return when (motivoRaw) {
            "NH3 Alto" -> "Niveles peligrosos de NH3 detectados"
            "CH4 Alto" -> "Niveles peligrosos de CH4 detectados"
            "CO2 Alto" -> "Niveles peligrosos de CO2 detectados"
            "Temperatura Alta" -> "Temperatura crítica detectada"
            "Apagado" -> "Ventilador desactivado - Condiciones normalizadas"
            else -> if (motivoRaw.isEmpty()) "Ventilador activado automáticamente" else "Sistema de ventilación: $motivoRaw"
        }
    }

    private fun obtenerMotivoBomba(estadoBomba: Int): String {
        return if (estadoBomba == 1) {
            "La bomba se activó automáticamente - Nivel de humedad bajo detectado"
        } else {
            "Bomba desactivada - Nivel de humedad normalizado"
        }
    }

    private fun mostrarNotificacion(motivo: String, estado: Int, tipo: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            // CAMBIO: Usar SINGLE_TOP en lugar de CLEAR_TASK para no reiniciar la app
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("redirect", "control_asistencia")
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val (titulo, icono, prioridad, sonido, vibracion) = when (tipo) {
            "ventilador" -> {
                val titulo = if (estado == 1) "¡Ventilador activado!" else "Ventilador desactivado"
                val icono = if (estado == 1) R.drawable.ic_alerta else R.drawable.ic_notificacion
                val prioridad = if (estado == 1) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT
                val sonido = if (estado == 1) RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val vibracion = if (estado == 1) longArrayOf(0, 500, 200, 500) else longArrayOf(0, 200)
                arrayOf(titulo, icono, prioridad, sonido, vibracion)
            }
            "bomba" -> {
                val titulo = if (estado == 1) "¡Riego activado!" else "Riego desactivado"
                val icono = if (estado == 1) R.drawable.ic_alerta else R.drawable.ic_notificacion
                val prioridad = if (estado == 1) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT
                val sonido = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val vibracion = if (estado == 1) longArrayOf(0, 300, 200, 300) else longArrayOf(0, 200)
                arrayOf(titulo, icono, prioridad, sonido, vibracion)
            }
            else -> {
                arrayOf("Notificación del sistema", R.drawable.ic_notificacion, NotificationCompat.PRIORITY_DEFAULT,
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), longArrayOf(0, 200))
            }
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(icono as Int)
            .setContentTitle(titulo as String)
            .setContentText(motivo)
            .setPriority(prioridad as Int)
            .setCategory(if (estado == 1) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSound(sonido as android.net.Uri)
            .setVibrate(vibracion as LongArray)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    // ✨ FUNCIÓN OPTIMIZADA - Solo guarda información esencial de notificaciones
    private fun guardarNotificacion(motivo: String, estado: Int, tipo: String) {
        auth.currentUser?.uid?.let { userId ->
            val titulo = when (tipo) {
                "ventilador" -> if (estado == 1) "Ventilador activado" else "Ventilador desactivado"
                "bomba" -> if (estado == 1) "Riego activado" else "Riego desactivado"
                else -> "Notificación del sistema"
            }

            val notificacion = hashMapOf(
                "fecha" to Calendar.getInstance().time,
                "titulo" to titulo,
                "motivo" to motivo,
                "estado" to estado,
                "leida" to false,
                "tipo" to tipo,
                "prioridad" to if (estado == 1) "alta" else "normal"
            )

            db.collection("MiCompostaApp").document(userId).collection("notificaciones")
                .add(notificacion)
                .addOnSuccessListener {
                    Log.d("NOTIFICACION", "✅ Notificación de $tipo guardada exitosamente")
                }
                .addOnFailureListener { e ->
                    Log.e("NOTIFICACION", "❌ Error al guardar notificación de $tipo", e)
                }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            guardarUltimoEstado() // Guardar estado antes de destruir el servicio
            if (::mqttClient.isInitialized) {
                mqttClient.disconnect()
                Log.i("MQTT_DEBUG", "Cliente MQTT desconectado")
            }
        } catch (e: Exception) {
            Log.e("MQTT_ERROR", "Error al desconectar", e)
        }
    }
}