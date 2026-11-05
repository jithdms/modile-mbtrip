package com.example.mbtrip.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// 다이어트/다크모드 컬러셋 (필요시 Primary 계열로 맞추거나 기존 유지)
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryVariant,
    secondary = Secondary,
    background = Background,
    surface = Surface
)

// 라이트모드 컬러셋 (우리가 지정한 여행 컨셉 색상 매핑!)
private val LightColorScheme = lightColorScheme(
    primary = Primary,           // #A9D7F5 스카이 블루
    secondary = Secondary,       // 딥 블루
    background = Background,     // 화사한 여행 배경색
    surface = Surface,           // 카드 배경용 화이트
    onPrimary = TextPrimary,     // Primary 위 글씨 색상
    onSecondary = Surface,       // Secondary 위 글씨 색상
    onBackground = TextPrimary,  // 배경 위 기본 글씨 색상
    onSurface = TextPrimary      // 서페이스 위 글씨 색상
)

@Composable
fun MBTripTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color는 디자인 통일을 위해 false로 두거나 원하시면 true로 유지하셔도 됩니다.
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
        content = content
    )
}