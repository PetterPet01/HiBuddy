package com.example.hibuddy.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

//val Purple80 = Color(0xFF7C6AF7)
//val PurpleGrey80 = Color(0xFF3D3B5C)
//val Pink80 = Color(0xFFFF4D6D)
//
//val Purple40 = Color(0xFF5B4FCF)
//val PurpleGrey40 = Color(0xFF2A2840)
//val Pink40 = Color(0xFFE03055)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF7C6AF7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2D2B4E),
    secondary = Color(0xFFFF4D6D),
    onSecondary = Color.White,
    background = Color(0xFF0D0D14),
    surface = Color(0xFF13131F),
    onBackground = Color(0xFFF0EFF8),
    onSurface = Color(0xFFF0EFF8),
    surfaceVariant = Color(0xFF1E1D2E),
    outline = Color(0xFF2E2D45),
)

@Composable
fun HiBuddyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
