// MainActivity.kt - Versión mejorada
package com.example.barbanner.mobile

import android.content.Context
import android.content.SharedPreferences
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

class MainActivity : ComponentActivity() {

    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var nsdManager: NsdManager
    private val discoveredServices = mutableStateListOf<NsdServiceInfo>()
    private lateinit var prefs: SharedPreferences

    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(regType: String) {
            Log.d("BARBANNER", "🔍 Buscando TVs en la red...")
        }

        override fun onServiceFound(service: NsdServiceInfo) {
            Log.d("BARBANNER", "📺 TV encontrada: ${service.serviceName}")
            nsdManager.resolveService(service, createResolveListener())
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            Log.w("BARBANNER", "⚠️ TV desconectada: ${service.serviceName}")
            discoveredServices.removeIf { it.serviceName == service.serviceName }
        }

        override fun onDiscoveryStopped(serviceType: String) {
            Log.i("BARBANNER", "🔚 Búsqueda detenida")
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e("BARBANNER", "❌ Error en búsqueda: $errorCode")
            nsdManager.stopServiceDiscovery(this)
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e("BARBANNER", "❌ Error al detener búsqueda: $errorCode")
            nsdManager.stopServiceDiscovery(this)
        }
    }

    private fun createResolveListener(): NsdManager.ResolveListener {
        return object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e("BARBANNER", "❌ No se pudo conectar a: ${serviceInfo.serviceName}")
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                Log.d("BARBANNER", "✅ Conectado a: ${serviceInfo.serviceName}")
                if (!discoveredServices.any { it.serviceName == serviceInfo.serviceName }) {
                    discoveredServices.add(serviceInfo)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nsdManager = getSystemService(Context.NSD_SERVICE) as NsdManager
        prefs = getSharedPreferences("BarBannerPrefs", Context.MODE_PRIVATE)

        setContent {
            val savedThemeIndex = prefs.getInt("selected_theme", 0)
            val selectedTheme = remember { mutableStateOf(AppTheme.themes[savedThemeIndex]) }
            var selectedService by remember { mutableStateOf<NsdServiceInfo?>(null) }

            BarBannerAppTheme(theme = selectedTheme.value) {
                if (selectedService == null) {
                    DiscoveryScreen(
                        services = discoveredServices,
                        currentTheme = selectedTheme.value,
                        onThemeChange = { newTheme ->
                            selectedTheme.value = newTheme
                            prefs.edit { putInt("selected_theme", AppTheme.themes.indexOf(newTheme)) }
                        },
                        onServiceSelected = { service -> selectedService = service }
                    )
                } else {
                    ControlScreen(
                        serviceInfo = selectedService!!,
                        currentTheme = selectedTheme.value,
                        onDisconnect = { selectedService = null }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        nsdManager.discoverServices("_http._tcp.", NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    override fun onPause() {
        super.onPause()
        nsdManager.stopServiceDiscovery(discoveryListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        activityScope.cancel()
    }
}

// Modelo de Tema
data class AppTheme(
    val name: String,
    val primary: Color,
    val secondary: Color,
    val accent: Color,
    val background: Color,
    val surface: Color
) {
    companion object {
        val themes = listOf(
            AppTheme(
                "Default",
                Color(0xFF2196F3),  // Azul profesional
                Color(0xFF03A9F4),
                Color(0xFF00BCD4),
                Color(0xFFF5F5F5),
                Color.White
            ),
            AppTheme(
                "Navidad",
                Color(0xFFD32F2F),  // Rojo navideño
                Color(0xFF388E3C),  // Verde
                Color(0xFFFFD700),  // Dorado
                Color(0xFFF5F5F5),
                Color(0xFFFFF8E1)
            ),
            // ... otros temas (los implementaremos todos)
        )
    }
}

// Tema personalizado para la app
@Composable
fun BarBannerAppTheme(
    theme: AppTheme,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ColorScheme(
            primary = theme.primary,
            secondary = theme.secondary,
            tertiary = theme.accent,
            background = theme.background,
            surface = theme.surface,
            error = Color(0xFFB00020),
            onPrimary = Color.White,
            onSecondary = Color.Black,
            onTertiary = Color.Black,
            onBackground = Color.Black,
            onSurface = Color.Black,
            onError = Color.White
        ),
        content = content
    )
}

// Pantalla de descubrimiento
@Composable
fun DiscoveryScreen(
    services: List<NsdServiceInfo>,
    currentTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit,
    onServiceSelected: (NsdServiceInfo) -> Unit
) {
    var showThemePicker by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(currentTheme.primary, currentTheme.secondary)
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo/Hero
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .background(Color.White, RoundedCornerShape(24.dp))
                    .border(4.dp, currentTheme.accent, RoundedCornerShape(24.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Tv,
                    contentDescription = "BarBanner",
                    tint = currentTheme.primary,
                    modifier = Modifier.size(80.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                "BarBanner Mobile",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Text(
                "Control remoto para TVs",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.9f)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            if (services.isEmpty()) {
                // Estado de búsqueda
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(60.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Buscando TVs en la red...",
                        color = Color.White,
                        fontSize = 18.sp
                    )
                    Text(
                        "Asegúrate de que la TV esté encendida",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
            } else {
                // Lista de TVs encontradas
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .heightIn(max = 400.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.95f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "📺 TVs Disponibles",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = currentTheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(services) { service ->
                                TvCard(
                                    service = service,
                                    theme = currentTheme,
                                    onClick = { onServiceSelected(service) }
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Botón para cambiar tema
            Button(
                onClick = { showThemePicker = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = currentTheme.primary
                ),
                border = BorderStroke(2.dp, currentTheme.accent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Palette, contentDescription = "Tema")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cambiar Tema")
            }
        }
        
        // Selector de temas (Modal)
        if (showThemePicker) {
            ThemePickerDialog(
                currentTheme = currentTheme,
                onThemeSelected = { newTheme ->
                    onThemeChange(newTheme)
                    showThemePicker = false
                },
                onDismiss = { showThemePicker = false }
            )
        }
    }
}

@Composable
fun TvCard(
    service: NsdServiceInfo,
    theme: AppTheme,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = theme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, theme.primary.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(theme.primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .border(2.dp, theme.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Tv,
                    contentDescription = null,
                    tint = theme.primary,
                    modifier = Modifier.size(30.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = service.serviceName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "${service.host?.hostAddress ?: "IP desconocida"}:${service.port}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Icon(
                Icons.Filled.ArrowForward,
                contentDescription = "Conectar",
                tint = theme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlScreen(
    serviceInfo: NsdServiceInfo,
    currentTheme: AppTheme,
    onDisconnect: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("BarBannerPrefs", Context.MODE_PRIVATE) }
    
    // Estado del formulario con valores guardados
    var message by remember {
        mutableStateOf(prefs.getString("last_message", "¡Oferta especial hoy!") ?: "")
    }
    var alertType by remember {
        mutableStateOf(prefs.getString("last_type", "OFERTA") ?: "OFERTA")
    }
    var displayCount by remember {
        mutableStateOf(prefs.getInt("last_display_count", 1))
    }
    var showEffects by remember {
        mutableStateOf(prefs.getBoolean("show_effects", true))
    }
    
    val client = remember {
        HttpClient(CIO) {
            install(WebSockets)
            install(ContentNegotiation) { json() }
        }
    }
    
    val alertTypes = listOf("OFERTA", "TAPA", "EVENTO", "LOTERÍA", "PERSONAL", "OTROS")
    val displayOptions = listOf(1, 2, 3, -1) // -1 para continuo
    
    fun savePreferences() {
        prefs.edit {
            putString("last_message", message)
            putString("last_type", alertType)
            putInt("last_display_count", displayCount)
            putBoolean("show_effects", showEffects)
        }
    }
    
    fun sendBannerAction(action: BannerAction) {
        scope.launch(Dispatchers.IO) {
            try {
                val host = serviceInfo.host?.hostAddress ?: run {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "❌ No se pudo obtener la IP", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                
                // Serializar con el discriminador correcto
                val json = Json { 
                    classDiscriminator = "type"
                    encodeDefaults = true
                }
                val jsonString = json.encodeToString(action)
                
                Log.d("BARBANNER_MOBILE", "📤 Enviando: $jsonString")
                
                client.webSocket(
                    host = host,
                    port = serviceInfo.port,
                    path = "/banner"
                ) {
                    send(Frame.Text(jsonString))
                    
                    // Esperar respuesta
                    val response = incoming.receive()
                    if (response is Frame.Text) {
                        val responseText = response.readText()
                        Log.d("BARBANNER_MOBILE", "📥 Respuesta: $responseText")
                    }
                }
                
                withContext(Dispatchers.Main) {
                    if (action is BannerAction.Show) {
                        savePreferences()
                        Toast.makeText(context, "✅ Banner publicado", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "⏸️ Banner ocultado", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("BARBANNER_MOBILE", "❌ Error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "❌ Error: ${e.message?.take(50) ?: "Conexión fallida"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(currentTheme.background)
            .padding(16.dp)
    ) {
        // Header con info de conexión
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = currentTheme.surface
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDisconnect,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = "Desconectar",
                        tint = currentTheme.primary
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Conectado a:",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        serviceInfo.serviceName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = currentTheme.primary
                    )
                    Text(
                        "${serviceInfo.host?.hostAddress ?: "IP desconocida"}:${serviceInfo.port}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(Color.Green, RoundedCornerShape(6.dp))
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Formulario de control
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = currentTheme.surface
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "📝 Crear Banner",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = currentTheme.primary
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Campo de mensaje
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Mensaje del banner") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = currentTheme.primary,
                        focusedLabelColor = currentTheme.primary,
                        cursorColor = currentTheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Selector de tipo
                Text(
                    "Tipo de anuncio:",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    alertTypes.forEach { type ->
                        FilterChip(
                            selected = alertType == type,
                            onClick = { alertType = type },
                            label = { Text(type) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = currentTheme.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Selector de frecuencia
                Text(
                    "Frecuencia:",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    displayOptions.forEach { count ->
                        val label = when (count) {
                            1 -> "1 vez"
                            2 -> "2 veces"
                            3 -> "3 veces"
                            -1 -> "Continuo"
                            else -> "$count veces"
                        }
                        
                        FilterChip(
                            selected = displayCount == count,
                            onClick = { displayCount = count },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = currentTheme.secondary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Toggle para efectos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = showEffects,
                        onCheckedChange = { showEffects = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = currentTheme.accent,
                            checkedTrackColor = currentTheme.accent.copy(alpha = 0.5f)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Efectos especiales", fontSize = 14.sp)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Botones de acción
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    val action = BannerAction.Show(
                        content = message,
                        type = alertType,
                        displayCount = displayCount
                    )
                    sendBannerAction(action)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = currentTheme.primary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("PUBLICAR EN TV", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            
            Button(
                onClick = { sendBannerAction(BannerAction.Hide) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = currentTheme.surface,
                    contentColor = currentTheme.primary
                ),
                border = BorderStroke(2.dp, currentTheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.VisibilityOff, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("OCULTAR BANNER", fontSize = 16.sp)
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Footer
        Text(
            "BarBanner Mobile • v1.0 • Conectado",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
fun ThemePickerDialog(
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🎨 Seleccionar Tema") },
        text = {
            LazyColumn {
                items(AppTheme.themes.chunked(3)) { rowThemes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowThemes.forEach { theme ->
                            ThemeOption(
                                theme = theme,
                                isSelected = theme == currentTheme,
                                onClick = { onThemeSelected(theme) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

@Composable
fun ThemeOption(
    theme: AppTheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .weight(1f)
            .height(100.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) BorderStroke(3.dp, theme.accent) else null,
        colors = CardDefaults.cardColors(
            containerColor = theme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Muestra de colores del tema
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(20.dp)
                        .background(theme.primary)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(20.dp)
                        .background(theme.secondary)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(20.dp)
                        .background(theme.accent)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = theme.name,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) theme.primary else Color.Gray
            )
        }
    }
}