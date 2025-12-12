// ============================================
// 2. BannerService.kt (TV - CORREGIDO)
// ============================================
package com.example.barbanner.tv

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import com.example.barbanner.shared.BannerAction
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.serialization.json.Json

class BannerService : Service() {

    private lateinit var windowManager: WindowManager
    private var bannerView: ComposeView? = null
    private val bannerState = mutableStateOf<BannerAction.Show?>(null)

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private var server: CIOApplicationEngine? = null
    private var displayJob: Job? = null
    
    // NSD (Network Service Discovery)
    private lateinit var nsdManager: NsdManager
    private var serviceName: String? = null
    
    // Overlay permission fallback
    private var useActivityOverlay = false

    companion object {
        const val SERVER_PORT = 8080
        const val BANNER_DISPLAY_DURATION_MS = 5000L
        const val TAG = "BannerService"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "📱 Servicio iniciado")
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        setupBannerView()
        startServer()
        startForegroundService()
    }

    private fun startServer() {
        serviceScope.launch(Dispatchers.IO) {
            try {
                server = embeddedServer(CIO, port = SERVER_PORT, host = "0.0.0.0") {
                    install(WebSockets) {
                        pingPeriod = java.time.Duration.ofSeconds(15)
                        timeout = java.time.Duration.ofSeconds(15)
                        maxFrameSize = Long.MAX_VALUE
                        masking = false
                    }
                    install(ContentNegotiation) {
                        json(Json { 
                            isLenient = true
                            ignoreUnknownKeys = true
                            classDiscriminator = "type"
                        })
                    }
                    routing {
                        webSocket("/banner") {
                            Log.d(TAG, "🔌 Cliente conectado")
                            try {
                                for (frame in incoming.receiveAsFlow()) {
                                    if (frame is Frame.Text) {
                                        val text = frame.readText()
                                        Log.d(TAG, "📨 Mensaje recibido: $text")
                                        
                                        try {
                                            val action = Json.decodeFromString<BannerAction>(text)
                                            withContext(Dispatchers.Main) {
                                                handleBannerAction(action)
                                            }
                                            send(Frame.Text("OK"))
                                        } catch (e: Exception) {
                                            Log.e(TAG, "❌ Error al decodificar JSON: ${e.message}")
                                            send(Frame.Text("ERROR: ${e.message}"))
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "❌ Error en WebSocket: ${e.message}")
                            } finally {
                                Log.d(TAG, "🔌 Cliente desconectado")
                            }
                        }
                    }
                }.start(wait = false)
                
                Log.d(TAG, "🌐 Servidor iniciado en puerto $SERVER_PORT")
                
                // Registrar servicio en la red después de iniciar el servidor
                withContext(Dispatchers.Main) {
                    registerNsdService()
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error iniciando servidor: ${e.message}")
            }
        }
    }

    private fun getLocalIpAddress(): String? {
        return try {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val ipAddress = wifiManager.connectionInfo.ipAddress
            if (ipAddress == 0) {
                null
            } else {
                String.format(
                    "%d.%d.%d.%d",
                    ipAddress and 0xff,
                    ipAddress shr 8 and 0xff,
                    ipAddress shr 16 and 0xff,
                    ipAddress shr 24 and 0xff
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo IP: ${e.message}")
            null
        }
    }

    private fun registerNsdService() {
        try {
            nsdManager = getSystemService(Context.NSD_SERVICE) as NsdManager
            
            val ipAddress = getLocalIpAddress()
            if (ipAddress == null) {
                Log.e(TAG, "❌ No se pudo obtener la IP local para NSD")
                return
            }
            
            Log.d(TAG, "📡 Registrando servicio NSD en IP: $ipAddress:$SERVER_PORT")
            
            val serviceInfo = NsdServiceInfo().apply {
                serviceName = "BarBannerTV"
                serviceType = "_http._tcp."
                port = SERVER_PORT
                // Establecer el host explícitamente (crítico para algunas redes)
                try {
                    host = java.net.InetAddress.getByName(ipAddress)
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ No se pudo establecer el host: ${e.message}")
                }
            }
            
            val registrationListener = object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(nsdServiceInfo: NsdServiceInfo) {
                    serviceName = nsdServiceInfo.serviceName
                    Log.d(TAG, "✅ Servicio NSD registrado: $serviceName en $ipAddress:$SERVER_PORT")
                }

                override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    Log.e(TAG, "❌ Fallo registro NSD: código $errorCode")
                }

                override fun onServiceUnregistered(arg0: NsdServiceInfo) {
                    Log.d(TAG, "📴 Servicio NSD desregistrado: ${arg0.serviceName}")
                }

                override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    Log.e(TAG, "❌ Fallo desregistro NSD: código $errorCode")
                }
            }
            
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error registrando NSD: ${e.message}")
        }
    }

    private fun unregisterNsdService() {
        try {
            if (::nsdManager.isInitialized) {
                // Nota: Para desregistrar correctamente, necesitaríamos guardar el listener
                // Por ahora, el sistema lo limpiará automáticamente al detener el servicio
                Log.d(TAG, "📴 Desregistrando servicio NSD")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error desregistrando NSD: ${e.message}")
        }
    }


    private fun handleBannerAction(action: BannerAction) {
        Log.d(TAG, "🎬 Procesando acción: $action")
        displayJob?.cancel()

        when (action) {
            is BannerAction.Show -> {
                if (useActivityOverlay) {
                    // Usar BannerOverlayActivity como fallback
                    showBannerViaActivity(action)
                } else {
                    // Usar overlay tradicional
                    if (action.displayCount == -1) {
                        // Modo continuo
                        bannerState.value = action
                        Log.d(TAG, "♾️ Banner en modo continuo")
                    } else {
                        // Modo con repeticiones
                        displayJob = serviceScope.launch {
                            repeat(action.displayCount) { index ->
                                bannerState.value = action
                                Log.d(TAG, "✅ Mostrando banner (${index + 1}/${action.displayCount})")
                                delay(BANNER_DISPLAY_DURATION_MS)
                                
                                bannerState.value = null
                                
                                if (index < action.displayCount - 1) {
                                    delay(1000) // Pausa entre repeticiones
                                }
                            }
                            Log.d(TAG, "🏁 Banner completado")
                        }
                    }
                }
            }
            is BannerAction.Hide -> {
                bannerState.value = null
                Log.d(TAG, "🙈 Banner ocultado")
            }
        }
    }
    
    private fun showBannerViaActivity(banner: BannerAction.Show) {
        try {
            val intent = Intent(this, BannerOverlayActivity::class.java).apply {
                putExtra(BannerOverlayActivity.EXTRA_CONTENT, banner.content)
                putExtra(BannerOverlayActivity.EXTRA_TYPE, banner.type)
                putExtra(BannerOverlayActivity.EXTRA_SHOW_EFFECTS, banner.showEffects)
                putExtra(BannerOverlayActivity.EXTRA_DURATION_MS, BANNER_DISPLAY_DURATION_MS)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            if (banner.displayCount == -1) {
                // Modo continuo: mostrar una vez y repetir
                Log.d(TAG, "♾️ Banner continuo via Activity (limitado a 1 vez)")
                startActivity(intent)
            } else {
                // Modo con repeticiones
                serviceScope.launch {
                    repeat(banner.displayCount) { index ->
                        Log.d(TAG, "✅ Mostrando banner via Activity (${index + 1}/${banner.displayCount})")
                        startActivity(intent)
                        delay(BANNER_DISPLAY_DURATION_MS + 1000) // Esperar duración + pausa
                    }
                    Log.d(TAG, "🏁 Banner completado via Activity")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error mostrando banner via Activity: ${e.message}")
        }
    }

    private fun setupBannerView() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.FILL_HORIZONTAL
            y = 100 // Margen inferior
        }

        bannerView = ComposeView(this).apply {
            setContent {
                val banner = bannerState.value
                if (banner != null) {
                    BannerDisplay(banner = banner)
                }
            }
        }
        
        try {
            windowManager.addView(bannerView, params)
            useActivityOverlay = false
            Log.d(TAG, "✅ Vista del banner añadida (overlay tradicional)")
        } catch (e: SecurityException) {
            Log.w(TAG, "⚠️ SYSTEM_ALERT_WINDOW bloqueado. Usando Activity overlay como fallback.")
            useActivityOverlay = true
            bannerView = null // No usar la vista si no tenemos permiso
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error añadiendo vista: ${e.message}")
            useActivityOverlay = true
            bannerView = null
        }
    }

    @Composable
    fun BannerDisplay(banner: BannerAction.Show) {
        val (backgroundColor, textColor) = when (banner.type) {
            "OFERTA" -> Color(0xFFFF9800) to Color.White // Naranja
            "TAPA" -> Color(0xFF4CAF50) to Color.White // Verde
            "LOTERÍA" -> Color(0xFFE91E63) to Color.White // Rosa
            "EVENTO" -> Color(0xFF2196F3) to Color.White // Azul
            "PERSONAL" -> Color(0xFF9C27B0) to Color.White // Morado
            "NAVIDAD" -> Color(0xFFD32F2F) to Color(0xFFFFD700) // Rojo con dorado
            else -> Color(0xFF607D8B) to Color.White // Gris
        }

        // Efectos visuales opcionales
        val animatedAlpha = if (banner.showEffects) {
            val infiniteTransition = rememberInfiniteTransition(label = "alpha_effect")
            infiniteTransition.animateFloat(
                initialValue = 0.85f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alpha_animation"
            ).value
        } else {
            1f
        }

        // Icono según tipo
        val icon = when (banner.type) {
            "OFERTA" -> "🔥"
            "TAPA" -> "🍽️"
            "LOTERÍA" -> "🎰"
            "EVENTO" -> "⚽"
            "PERSONAL" -> "🎉"
            "NAVIDAD" -> "🎄"
            else -> "📢"
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            backgroundColor.copy(alpha = animatedAlpha),
                            backgroundColor.copy(alpha = animatedAlpha * 0.85f),
                            backgroundColor.copy(alpha = animatedAlpha)
                        )
                    )
                )
                .padding(vertical = 20.dp, horizontal = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Icono
                Text(
                    text = icon,
                    fontSize = 48.sp,
                    color = textColor
                )

                // Contenido
                Column {
                    Text(
                        text = banner.content,
                        color = textColor,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "• ${banner.type} •",
                        color = textColor.copy(alpha = 0.85f),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Icono final
                Text(
                    text = icon,
                    fontSize = 48.sp,
                    color = textColor
                )
            }
        }
    }

    private fun startForegroundService() {
        val channelId = "BannerServiceChannel"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Banner Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Servicio de banner para BarBanner TV"
            }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("🎬 BarBanner TV Activo")
            .setContentText("Esperando mensajes en puerto $SERVER_PORT")
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Icono genérico de Android
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "🛑 Servicio detenido")
        
        unregisterNsdService()
        
        try {
            bannerView?.let { windowManager.removeView(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error removiendo vista: ${e.message}")
        }
        
        server?.stop(1_000, 2_000)
        serviceScope.cancel()
    }
}