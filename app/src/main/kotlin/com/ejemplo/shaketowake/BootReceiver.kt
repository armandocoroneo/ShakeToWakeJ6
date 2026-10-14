package com.ejemplo.shaketowake

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, ScreenWakeService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context?.startForegroundService(serviceIntent)
            } else {
                context?.startService(serviceIntent)
            }
            Log.d("ShakeToWake", "Servicio iniciado después del boot")
        }
    }
}
