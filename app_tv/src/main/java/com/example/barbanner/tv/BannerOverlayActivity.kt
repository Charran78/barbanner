package com.example.barbanner.tv

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api

/**
 * Activity overlay transparente para mostrar banners cuando SYSTEM_ALERT_WINDOW está bloqueado.
 * Esta es una alternativa al overlay tradicional para TV boxes que bloquean ese permiso.
 */
class BannerOverlayActivity : ComponentActivity() {

    companion object {
        const val EXTRA_CONTENT = "extra_content"
        const val EXTRA_TYPE = "extra_type"
        const val EXTRA_SHOW_EFFECTS = "extra_show_effects"
        const val EXTRA_DURATION_MS = "extra_duration_ms"
        const val TAG = "BannerOverlay"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configurar ventana como overlay transparente
        window.apply {
            setFlags(
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            )
            setBackgroundDrawableResource(android.R.color.transparent)
        }

        val content = intent.getStringExtra(EXTRA_CONTENT) ?: "Sin mensaje"
        val type = intent.getStringExtra(EXTRA_TYPE) ?: "OTROS"
        val showEffects = intent.getBooleanExtra(EXTRA_SHOW_EFFECTS, true)
        val durationMs = intent.getLongExtra(EXTRA_DURATION_MS, 5000L)

        Log.d(TAG, "Mostrando banner: $content (tipo: $type)")

        setContent {
            BannerOverlayContent(
                content = content,
                type = type,
                showEffects = showEffects
            )
        }

        // Auto-cerrar después de la duración especificada
        Handler(Looper.getMainLooper()).postDelayed({
            Log.d(TAG, "Cerrando banner overlay")
            finish()
        }, durationMs)
    }

    @OptIn(ExperimentalTvMaterial3Api::class)
    @Composable
    fun BannerOverlayContent(
        content: String,
        type: String,
        showEffects: Boolean
    ) {
        val (backgroundColor, textColor) = when (type) {
            "OFERTA" -> Color(0xFFFF9800) to Color.White
            "TAPA" -> Color(0xFF4CAF50) to Color.White
            "LOTERÍA" -> Color(0xFFE91E63) to Color.White
            "EVENTO" -> Color(0xFF2196F3) to Color.White
            "PERSONAL" -> Color(0xFF9C27B0) to Color.White
            "NAVIDAD" -> Color(0xFFD32F2F) to Color(0xFFFFD700)
            else -> Color(0xFF607D8B) to Color.White
        }

        val animatedAlpha = if (showEffects) {
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

        val icon = when (type) {
            "OFERTA" -> "🔥"
            "TAPA" -> "🍽️"
            "LOTERÍA" -> "🎰"
            "EVENTO" -> "⚽"
            "PERSONAL" -> "🎉"
            "NAVIDAD" -> "🎄"
            else -> "📢"
        }

        // Banner en la parte inferior de la pantalla
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
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
                    Text(
                        text = icon,
                        fontSize = 48.sp,
                        color = textColor
                    )

                    Column {
                        Text(
                            text = content,
                            color = textColor,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "• $type •",
                            color = textColor.copy(alpha = 0.85f),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = icon,
                        fontSize = 48.sp,
                        color = textColor
                    )
                }
            }
        }
    }
}
