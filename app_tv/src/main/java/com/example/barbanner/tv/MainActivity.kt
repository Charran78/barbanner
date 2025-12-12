package com.example.barbanner.tv

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val ipAddress = remember { mutableStateOf(getIpAddress()) }

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("BarBanner TV Receptor", fontSize = 32.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Dirección IP: ${ipAddress.value}", fontSize = 20.sp)
                Text("Puerto: ${BannerService.SERVER_PORT}", fontSize = 20.sp)
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = {
                    if (!Settings.canDrawOverlays(this@MainActivity)) {
                        requestOverlayPermission()
                    } else {
                        startBannerService()
                    }
                }) {
                    Text("Iniciar Servicio de Banner")
                }
            }
        }
    }

    private fun getIpAddress(): String {
        return try {
            val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
            val ipAddress = wifiManager.connectionInfo.ipAddress
            if (ipAddress == 0) {
                "No disponible"
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
            "Error obteniendo IP"
        }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        if (Settings.canDrawOverlays(this)) {
            startBannerService()
            // El servicio ahora maneja el registro NSD automáticamente
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(Intent(this, BannerService::class.java))
    }

    private fun startBannerService() {
        val intent = Intent(this, BannerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}