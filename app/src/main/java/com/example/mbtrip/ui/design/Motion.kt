package com.example.mbtrip.ui.design

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.mbtrip.ui.theme.Aqua600
import kotlinx.coroutines.delay

/*
 * ───────────── MBTrip 모션 (심플) ─────────────
 * 눈에 띄는 장식(둥실·깜빡·빛 스침·회전)은 빼고, 쓰는 데 도움이 되는 움직임만 남겼다.
 *  - bouncyClick   : 누르면 아주 살짝 작아졌다 돌아옴 (그림자 없음)
 *  - enterFromBelow: 처음 나올 때 살짝 아래에서 나타남 (index 로 순서대로)
 *  - enterPop      : 처음 나올 때 부드럽게 나타남 (살짝 커지며)
 *  - shake         : 틀렸을 때 좌우로 흔들림
 *  - animatedCount / animatedFill : 숫자·막대가 0 에서 차오름
 */

object MBMotion {
    /** 튀지 않고 부드럽게 멈추는 스프링 */
    val Bouncy = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 900f)
    val Gentle = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 300f)

    const val STAGGER_MS = 50L
}

/**
 * 누르면 아주 살짝(최대 3%) 작아졌다 돌아오는 클릭. 그림자는 쓰지 않는다.
 * 크기·모양 다음, background 보다 **앞에** 둔다 → 배경까지 같이 줄어들고 물결도 모양 안에만 생긴다.
 *   Modifier.fillMaxWidth().height(52.dp).bouncyClick(shape) { … }.background(…)
 * (elevation · shadowColor 는 예전 호출과 맞추려고 남긴 값이며 무시된다)
 */
fun Modifier.bouncyClick(
    shape: Shape = RoundedCornerShape(16.dp),
    enabled: Boolean = true,
    scaleDown: Float = 0.97f,
    @Suppress("UNUSED_PARAMETER") elevation: Dp = 0.dp,
    @Suppress("UNUSED_PARAMETER") shadowColor: Color = Color.Unspecified,
    role: Role = Role.Button,
    onClick: () -> Unit,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val target = scaleDown.coerceAtLeast(0.97f)
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) target else 1f,
        animationSpec = MBMotion.Bouncy,
        label = "press",
    )
    Modifier
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.shape = shape
            clip = true
        }
        .clickable(
            interactionSource = interaction,
            indication = LocalIndication.current,
            enabled = enabled,
            role = role,
            onClick = onClick,
        )
}

/** 처음 그려질 때 살짝 아래에서 나타난다. index 가 클수록 조금씩 늦게 */
fun Modifier.enterFromBelow(
    index: Int = 0,
    distance: Dp = 12.dp,
    delayStepMs: Long = MBMotion.STAGGER_MS,
    startDelayMs: Long = 0L,
): Modifier = composed {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(startDelayMs + index.coerceAtMost(8) * delayStepMs)
        progress.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
    }
    val px = with(LocalDensity.current) { distance.coerceAtMost(12.dp).toPx() }
    Modifier.graphicsLayer {
        val p = progress.value
        alpha = p
        translationY = (1f - p) * px
    }
}

/** 처음 그려질 때 부드럽게 나타난다 (살짝 작게 시작) */
fun Modifier.enterPop(index: Int = 0, delayStepMs: Long = MBMotion.STAGGER_MS, startDelayMs: Long = 0L, from: Float = 0.96f): Modifier = composed {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(startDelayMs + index.coerceAtMost(8) * delayStepMs)
        progress.animateTo(1f, tween(260, easing = FastOutSlowInEasing))
    }
    val start = from.coerceAtLeast(0.96f)
    Modifier.graphicsLayer {
        val p = progress.value
        val sc = start + (1f - start) * p
        scaleX = sc; scaleY = sc
        alpha = p
    }
}

/** trigger 가 바뀔 때마다 좌우로 흔들림 (틀린 코드 등). trigger 가 null 이면 가만히 */
fun Modifier.shake(trigger: Any?): Modifier = composed {
    val x = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger != null) {
            x.animateTo(
                0f,
                keyframes {
                    durationMillis = 420
                    (-14f) at 60
                    12f at 120
                    (-9f) at 190
                    7f at 260
                    (-3f) at 330
                    0f at 420
                },
            )
        }
    }
    val density = LocalDensity.current.density
    Modifier.graphicsLayer { translationX = x.value * density }
}

/** 0 에서 target 까지 숫자가 올라가는 효과 */
@Composable
fun animatedCount(target: Int, durationMs: Int = 1100, delayMs: Int = 200): State<Int> {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    return animateIntAsState(
        targetValue = if (started) target else 0,
        animationSpec = tween(durationMs, delayMillis = delayMs, easing = FastOutSlowInEasing),
        label = "count",
    )
}

/** 0 에서 target 까지 차오르는 비율 (막대 그래프용) */
@Composable
fun animatedFill(target: Float, durationMs: Int = 1000, delayMs: Int = 250): State<Float> {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    return animateFloatAsState(
        targetValue = if (started) target else 0f,
        animationSpec = tween(durationMs, delayMillis = delayMs, easing = FastOutSlowInEasing),
        label = "fill",
    )
}
