package com.example.barbanner.mobile

import androidx.compose.ui.graphics.Color

object ThemeManager {
    
    data class AppTheme(
        val name: String,
        val primary: Color,
        val secondary: Color,
        val accent: Color,
        val background: Color,
        val surface: Color,
        val icon: String? = null // Emoji o referencia a icono
    )
    
    val themes = listOf(
        AppTheme(
            "Default",
            Color(0xFF2196F3),  // Azul profesional
            Color(0xFF03A9F4),
            Color(0xFF00BCD4),
            Color(0xFFF5F5F5),
            Color.White,
            "📱"
        ),
        AppTheme(
            "Navidad",
            Color(0xFFD32F2F),  // Rojo
            Color(0xFF388E3C),  // Verde
            Color(0xFFFFD700),  // Dorado
            Color(0xFFF5F5F5),
            Color(0xFFFFF8E1),
            "🎄"
        ),
        AppTheme(
            "Reyes Magos",
            Color(0xFF7B1FA2),  // Morado
            Color(0xFF1976D2),  // Azul
            Color(0xFFFFC107),  // Amarillo
            Color(0xFFF3E5F5),
            Color(0xFFE1F5FE),
            "👑"
        ),
        AppTheme(
            "San Valentín",
            Color(0xFFE91E63),  // Rosa
            Color(0xFFF8BBD0),  // Rosa claro
            Color(0xFFC2185B),  // Rosa oscuro
            Color(0xFFFCE4EC),
            Color(0xFFFCE4EC),
            "❤️"
        ),
        AppTheme(
            "Carnaval",
            Color(0xFF9C27B0),  // Morado
            Color(0xFFE1BEE7),  // Lila
            Color(0xFF4A148C),  // Púrpura
            Color(0xFFF3E5F5),
            Color.White,
            "🎭"
        ),
        AppTheme(
            "Semana Santa",
            Color(0xFF3F51B5),  // Azul índigo
            Color(0xFFC5CAE9),  // Azul claro
            Color(0xFF1A237E),  // Azul oscuro
            Color(0xFFE8EAF6),
            Color.White,
            "✝️"
        ),
        AppTheme(
            "Pascua",
            Color(0xFF4CAF50),  // Verde
            Color(0xFFC8E6C9),  // Verde claro
            Color(0xFF1B5E20),  // Verde oscuro
            Color(0xFFE8F5E9),
            Color.White,
            "🐣"
        ),
        AppTheme(
            "Primavera",
            Color(0xFF4CAF50),  // Verde
            Color(0xFFFFC107),  // Amarillo
            Color(0xFFE91E63),  // Rosa
            Color(0xFFF1F8E9),
            Color(0xFFFFFDE7),
            "🌸"
        ),
        AppTheme(
            "Verano",
            Color(0xFF00BCD4),  // Cian
            Color(0xFFB2EBF2),  // Cian claro
            Color(0xFF0097A7),  // Cian oscuro
            Color(0xFFE0F7FA),
            Color(0xFFFFFDE7),
            "☀️"
        ),
        AppTheme(
            "Otoño",
            Color(0xFFFF9800),  // Naranja
            Color(0xFFFFE0B2),  // Naranja claro
            Color(0xFFE65100),  // Naranja oscuro
            Color(0xFFFFF3E0),
            Color(0xFFFFFDE7),
            "🍂"
        ),
        AppTheme(
            "Invierno",
            Color(0xFF2196F3),  // Azul
            Color(0xFFBBDEFB),  // Azul claro
            Color(0xFF0D47A1),  // Azul oscuro
            Color(0xFFE3F2FD),
            Color.White,
            "❄️"
        ),
        AppTheme(
            "Halloween",
            Color(0xFF7B1FA2),  // Morado
            Color(0xFF212121),  // Negro
            Color(0xFFFF9800),  // Naranja
            Color(0xFF424242),
            Color(0xFF212121),
            "🎃"
        ),
        AppTheme(
            "Difuntos",
            Color(0xFF607D8B),  // Gris azulado
            Color(0xFFB0BEC5),  // Gris claro
            Color(0xFF37474F),  // Gris oscuro
            Color(0xFFECEFF1),
            Color(0xFFCFD8DC),
            "🕯️"
        ),
        AppTheme(
            "Hispanidad",
            Color(0xFFF44336),  // Rojo
            Color(0xFFFFCDD2),  // Rojo claro
            Color(0xFFD32F2F),  // Rojo oscuro
            Color(0xFFFFEBEE),
            Color(0xFFFFFDE7),
            "🇪🇸"
        ),
        AppTheme(
            "Festivo",
            Color(0xFF607D8B),  // Gris azulado
            Color(0xFFCFD8DC),  // Gris claro
            Color(0xFF37474F),  // Gris oscuro
            Color(0xFFECEFF1),
            Color.White,
            "🎉"
        )
    )
    
    fun getThemeByName(name: String): AppTheme {
        return themes.find { it.name == name } ?: themes.first()
    }
}