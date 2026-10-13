package com.ejemplo.shaketowake

import android.content.Intent
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    private var isServiceRunning = false

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            ShakeToWakeUI(
                isRunning = isServiceRunning,
                onToggleService = { startStop ->
                    if (startStop) startService() else stopService()
                }
            )
        }
    }

    private fun startService() {
        val serviceIntent = Intent(this, ScreenWakeService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        isServiceRunning = true
    }

    private fun stopService() {
        val serviceIntent = Intent(this, ScreenWakeService::class.java)
        stopService(serviceIntent)
        isServiceRunning = false
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isServiceRunning) stopService()
    }
}

@Composable
fun ShakeToWakeUI(isRunning: Boolean, onToggleService: (Boolean) -> Unit) {
    var serviceActive by remember { mutableStateOf(isRunning) }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF6200EE),
            surface = Color(0xFF121212)
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Shake to Wake",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(
                            color = if (serviceActive) Color(0xFF4CAF50) else Color(0xFF555555),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(60.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (serviceActive) "ON" else "OFF",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        serviceActive = !serviceActive
                        onToggleService(serviceActive)
                    },
                    modifier = Modifier
                        .height(56.dp)
                        .fillMaxWidth(0.8f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (serviceActive) Color(0xFFD32F2F) else Color(0xFF4CAF50)
                    )
                ) {
                    Text(
                        text = if (serviceActive) "DETENER" else "INICIAR",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                Text(
                    text = "Agita el dispositivo para encender la pantalla",
                    fontSize = 14.sp,
                    color = Color(0xFFBBBBBB),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Estado: ${if (serviceActive) "Escuchando movimiento..." else "Servicio detenido"}",
                    fontSize = 12.sp,
                    color = if (serviceActive) Color(0xFF4CAF50) else Color(0xFFFF9800)
                )
            }
        }
    }
}
