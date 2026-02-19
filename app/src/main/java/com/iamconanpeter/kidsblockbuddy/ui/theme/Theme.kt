package com.iamconanpeter.kidsblockbuddy.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Colors = darkColorScheme(
    primary = Color(0xFF60A5FA),
    secondary = Color(0xFF34D399),
    background = Color(0xFF030712),
    surface = Color(0xFF111827)
)

@Composable
fun KidsBlockBuddyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Colors,
        content = content
    )
}
