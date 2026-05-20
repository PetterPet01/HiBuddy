package com.example.hibuddy.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF7C6AF7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2D2B4E),
    onPrimaryContainer = Color(0xFFE8E5FF),
    secondary = Color(0xFFFF4D6D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4D1825),
    onSecondaryContainer = Color(0xFFFFD9E0),
    background = Color(0xFF0D0D14),
    surface = Color(0xFF13131F),
    onBackground = Color(0xFFF0EFF8),
    onSurface = Color(0xFFF0EFF8),
    surfaceVariant = Color(0xFF1E1D2E),
    onSurfaceVariant = Color(0xFFC9C6D8),
    outline = Color(0xFF2E2D45),
    error = Color(0xFFFF6B87),
    errorContainer = Color(0xFF3D101C),
    onErrorContainer = Color(0xFFFFD9E0),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4F46B8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7E4FF),
    onPrimaryContainer = Color(0xFF17104A),
    secondary = Color(0xFFB91F45),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD9E3),
    onSecondaryContainer = Color(0xFF470013),
    tertiary = Color(0xFF006B5F),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFBFEFE5),
    onTertiaryContainer = Color(0xFF00201C),
    background = Color(0xFFF6F7FB),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF15161F),
    onSurface = Color(0xFF15161F),
    surfaceVariant = Color(0xFFE8EAF2),
    onSurfaceVariant = Color(0xFF4D5061),
    outline = Color(0xFF74788A),
    error = Color(0xFFBA1A3A),
    errorContainer = Color(0xFFFFD9E0),
    onErrorContainer = Color(0xFF41000D),
)

@Composable
fun HiBuddyTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}

object HiBuddyColors {
    val success: Color
        @Composable get() = if (isLightTheme) Color(0xFF0B7A48) else Color(0xFF4CAF50)

    val successContainer: Color
        @Composable get() = if (isLightTheme) Color(0xFFDDF7E8) else Color(0xFF153A27)

    val onSuccessContainer: Color
        @Composable get() = if (isLightTheme) Color(0xFF00391F) else Color(0xFFDDF7E8)

    val warning: Color
        @Composable get() = if (isLightTheme) Color(0xFF8A5A00) else Color(0xFFFFD166)

    val warningContainer: Color
        @Composable get() = if (isLightTheme) Color(0xFFFFF1CC) else Color(0xFF4A3400)

    val onWarningContainer: Color
        @Composable get() = if (isLightTheme) Color(0xFF3F2900) else Color(0xFFFFE9B3)

    val info: Color
        @Composable get() = if (isLightTheme) Color(0xFF006878) else Color(0xFF4ECDC4)

    val infoContainer: Color
        @Composable get() = if (isLightTheme) Color(0xFFD6F5FA) else Color(0xFF103A42)

    val onInfoContainer: Color
        @Composable get() = if (isLightTheme) Color(0xFF002F37) else Color(0xFFD6F5FA)

    val mutedContent: Color
        @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)

    val disabledContent: Color
        @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f)

    private val isLightTheme: Boolean
        @Composable get() = MaterialTheme.colorScheme.background.luminance() > 0.5f
}

@Composable
fun hiBuddyTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.72f),
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    errorBorderColor = MaterialTheme.colorScheme.error,
    errorLabelColor = MaterialTheme.colorScheme.error,
    errorCursorColor = MaterialTheme.colorScheme.error,
)
