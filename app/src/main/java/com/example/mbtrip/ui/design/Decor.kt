package com.example.mbtrip.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.mbtrip.ui.theme.Background

/*
 * ───────────── 화면 바탕 ─────────────
 * 예전엔 빛 번짐·구름이 움직였지만, 심플한 톤을 위해 단색 바탕만 남겼다.
 */

/**
 * 화면 바탕 (단색). 그 위에 content 를 얹는다.
 * intensity · clouds 는 예전 호출과 맞추려고 남긴 값이며 무시된다.
 */
@Composable
fun SkyBackground(
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") intensity: Float = 1f,
    @Suppress("UNUSED_PARAMETER") clouds: Boolean = false,
    base: Color = Background,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier.background(base), content = content)
}
