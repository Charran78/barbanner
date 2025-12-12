// ============================================
// 1. BannerAction.kt (COMPARTIDO - mismo en mobile y tv)
// ============================================
package com.example.barbanner.shared

import kotlinx.serialization.Serializable

@Serializable
sealed class BannerAction {
    @Serializable
    data class Show(
        val content: String,
        val type: String,
        val displayCount: Int = 1, // 1, 2, 3... para repeticiones, -1 para continuo
        val showEffects: Boolean = true,
        val effectType: String? = null
    ) : BannerAction()

    @Serializable
    object Hide : BannerAction()
}
