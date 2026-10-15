package com.ejemplo.shaketowake

import android.app.Service
import android.app.PendingIntent
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.Context
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.os.Build
import android.os.PowerManager
import android.util.Log
import kotlin.math.sqrt

class ScreenWakeService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var powerManager: PowerManager
    private var wakeLock: PowerManager.WakeLock? = null
    private var accelerometer: Sensor? = null
    private var lastShakeTime = 0L
    private val shakeThreshold = 25f
    private val shakeTimerInterval = 200L

    // Tiempo que el sensor acumulará datos antes de despertar a la CPU (en microsegundos).
    // 5,000,000 us = 5 segundos. Esto ahorra mucha batería.
    private val maxReportLatencyUs = 5000000

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        startForegroundService()
        registerSensorListener()
        registerChargerReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun startForegroundService() {
        val channelId = "screen_wake_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Screen Wake Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = Notification.Builder(this, channelId)
            .setContentTitle("Shake to Wake Active")
            .setContentText("Agita para encender pantalla")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    private fun registerSensorListener() {
        if (accelerometer != null) {
            // AQUÍ ESTÁ LA MAGIA DEL AHORRO DE BATERÍA (Sensor Batching)
            // El sensor guardará los datos y nos los entregará cada 5 segundos.
            sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL,
                maxReportLatencyUs
            )
        }
    }

    private fun registerChargerReceiver() {
        val intentFilter = IntentFilter(Intent.ACTION_POWER_CONNECTED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(chargerReceiver, intentFilter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(chargerReceiver, intentFilter)
        }
    }

    private val chargerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_POWER_CONNECTED) {
                wakeUpScreen()
                Log.d("ShakeToWake", "Pantalla encendida por cargador")
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val acceleration = sqrt(x * x + y * y + z * z)
            val currentTime = System.currentTimeMillis()

            if (acceleration > shakeThreshold && 
                (currentTime - lastShakeTime) > shakeTimerInterval) {
                lastShakeTime = currentTime
                wakeUpScreen()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun wakeUpScreen() {
        try {
            if (wakeLock == null) {
                wakeLock = powerManager.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "ShakeToWake::WakeLock"
                )
            }

            wakeLock?.acquire(10000)
            Log.d("ShakeToWake", "Pantalla encendida por shake")
        } catch (e: Exception) {
            Log.e("ShakeToWake", "Error al encender pantalla: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        unregisterReceiver(chargerReceiver)
        
        // ARREGLO DEL CRASH: Solo liberar el WakeLock si está activo.
        // Si no está activo, no hacemos nada para evitar la excepción.
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        wakeLock = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
