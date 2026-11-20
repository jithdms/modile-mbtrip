package com.example.mbtrip.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/*
 * primary 는 TextButton 글자·입력칸 테두리·커서·탭 밑줄에도 쓰여서
 * 흰 바탕에서 잘 읽히는 Aqua700 으로 두고, 브랜드 색 #A7E7F5 는 primaryContainer(면) 로 쓴다.
 */
private val LightColorScheme = lightColorScheme(
    primary = Aqua700,
    onPrimary = Color.White,
    primaryContainer = Aqua300,
    onPrimaryContainer = Ink,
    secondary = Aqua600,
    onSecondary = Color.White,
    secondaryContainer = Aqua100,
    onSecondaryContainer = Ink,
    tertiary = Coral,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE6E1),
    onTertiaryContainer = Ink,
    background = Aqua50,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Field,
    onSurfaceVariant = Muted,
    surfaceTint = Aqua300,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Aqua50,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Aqua100,
    outline = Faint,
    outlineVariant = Line,
    error = Danger,
    onError = Color.White,
    errorContainer = DangerSoft,
    onErrorContainer = Danger,
    scrim = Color(0x990F2A3A),
)

private val DarkColorScheme = darkColorScheme(
    primary = Aqua300,
    onPrimary = Ink,
    primaryContainer = Aqua800,
    onPrimaryContainer = Aqua100,
    secondary = Aqua400,
    background = Color(0xFF0B1E29),
    surface = Color(0xFF12293A),
)

/** 앱 전체 모서리: 작은 칩 12 · 카드 20 · 모달 28 */
val MBShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

@Composable
fun MBTripTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // 디자인 통일을 위해 기본은 false
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = MBShapes,
        content = content
    )
}
