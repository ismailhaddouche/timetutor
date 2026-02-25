package com.haddouche.timetutor.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter

@Composable
fun UserProfileImage(
    photoUrl: String?,
    firstName: String?,
    lastName: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    fontSize: TextUnit = MaterialTheme.typography.titleMedium.fontSize
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (!photoUrl.isNullOrBlank()) {
            Image(
                painter = rememberAsyncImagePainter(photoUrl),
                contentDescription = "Foto de perfil",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            val initial = (firstName?.take(1) ?: "") + (lastName?.take(1) ?: "")
            val text = if (initial.isBlank()) "?" else initial.uppercase()
            
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize
            )
        }
    }
}

// Helper para parsear color adaptativo
@Composable
fun parseColor(hex: String, isDark: Boolean = isSystemInDarkTheme()): Color {
    val fallbackDarkSurface = MaterialTheme.colorScheme.surfaceVariant
    return try {
        val baseColorInt = android.graphics.Color.parseColor(hex)
        if (isDark) {
            if (hex.equals("#FFFFFF", ignoreCase = true) || hex.equals("#FAFAFA", ignoreCase = true) || hex.equals("#F5F5F5", ignoreCase = true)) {
                // If it's pure white or very light gray in DB, render as a dark surface in dark mode
                fallbackDarkSurface
            } else {
                // Convert pastel colors to darker variations for dark theme
                val hsv = FloatArray(3)
                android.graphics.Color.colorToHSV(baseColorInt, hsv)
                hsv[1] = (hsv[1] * 1.6f).coerceAtMost(1f) // Increase saturation to avoid washing out
                hsv[2] = (hsv[2] * 0.45f).coerceAtMost(1f) // Greatly reduce brightness to match dark theme
                Color(android.graphics.Color.HSVToColor(hsv))
            }
        } else {
            Color(baseColorInt)
        }
    } catch (e: Exception) {
        if (isDark) Color(0xFF1E1E1E) else Color.White
    }
}
