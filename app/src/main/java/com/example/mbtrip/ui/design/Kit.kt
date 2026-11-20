package com.example.mbtrip.ui.design

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mbtrip.timeline.ui.BackButton
import com.example.mbtrip.ui.theme.Aqua100
import com.example.mbtrip.ui.theme.Aqua300
import com.example.mbtrip.ui.theme.Aqua500
import com.example.mbtrip.ui.theme.Aqua700
import com.example.mbtrip.ui.theme.Faint
import com.example.mbtrip.ui.theme.Field
import com.example.mbtrip.ui.theme.Ink
import com.example.mbtrip.ui.theme.Line
import com.example.mbtrip.ui.theme.Muted

/*
 * ───────────── MBTrip 공용 부품 (심플) ─────────────
 * 그림자 없이 흰 면 + 얇은 회색 테두리(1dp)로 구분한다. 아이콘은 res/drawable 의 그림(ic_mb_*)을 쓴다.
 */

/** 테두리 두께 — 앱 전체 공통 */
val Hairline: Dp = 1.dp

enum class MBButtonStyle {
    /** 브랜드 아쿠아 면 (화면의 핵심 버튼 하나) */
    Brand,

    /** 진한 남색 (저장·완료 같은 확정 버튼) */
    Deep,

    /** 흰 바탕 + 얇은 테두리 */
    Outline,

    /** 연한 아쿠아 면 (보조 버튼) */
    Soft,
}

/** res/drawable 그림 아이콘 (한 가지 색으로 칠함) */
@Composable
fun MBIcon(@DrawableRes res: Int, modifier: Modifier = Modifier, size: Dp = 20.dp, tint: Color = Ink, contentDescription: String? = null) {
    Icon(
        painter = painterResource(res),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        tint = tint,
    )
}

/** 꽉 찬 버튼. 글자가 바뀌면(예: 참여하기 → 확인 중…) 부드럽게 바뀐다 */
@Composable
fun MBButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: MBButtonStyle = MBButtonStyle.Brand,
    height: Dp = 52.dp,
    loading: Boolean = false,
    @DrawableRes leadingIcon: Int? = null,
) {
    val shape = RoundedCornerShape(14.dp)
    val fg = when {
        !enabled -> Faint
        style == MBButtonStyle.Deep -> Color.White
        else -> Ink
    }
    val bg = when {
        !enabled -> Field
        style == MBButtonStyle.Brand -> Aqua300
        style == MBButtonStyle.Deep -> Ink
        style == MBButtonStyle.Outline -> Color.White
        else -> Aqua100
    }
    val border = when {
        !enabled -> Line
        style == MBButtonStyle.Outline -> Line
        style == MBButtonStyle.Brand -> Aqua500.copy(alpha = 0.35f)
        else -> Color.Transparent
    }
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .bouncyClick(shape = shape, enabled = enabled && !loading, onClick = onClick)
            .background(bg, shape)
            .border(Hairline, border, shape),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = loading to text,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "btnText",
        ) { (isLoading, label) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isLoading) {
                    CircularProgressIndicator(Modifier.size(16.dp), color = fg, strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                } else if (leadingIcon != null) {
                    MBIcon(leadingIcon, size = 18.dp, tint = fg)
                    Spacer(Modifier.width(8.dp))
                }
                Text(label, color = fg, style = MaterialTheme.typography.labelLarge, fontSize = 15.sp)
            }
        }
    }
}

/** 흰 카드 — 그림자 없이 얇은 테두리. onClick 이 있으면 눌림 효과 */
@Composable
fun MBCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    contentPadding: PaddingValues = PaddingValues(20.dp),
    background: Brush? = null,
    onClick: (() -> Unit)? = null,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .then(if (onClick != null) Modifier.bouncyClick(shape, scaleDown = 0.98f, onClick = onClick) else Modifier)
            .background(background ?: SolidColor(Color.White), shape)
            .border(Hairline, Line, shape)
            .padding(contentPadding),
        verticalArrangement = verticalArrangement,
        content = content,
    )
}

/** 동그라미 아이콘 버튼 (흰 바탕 + 얇은 테두리) */
@Composable
fun CircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    background: Color = Color.White,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .size(size)
            .bouncyClick(CircleShape, onClick = onClick)
            .background(background, CircleShape)
            .border(Hairline, Line, CircleShape),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** 서브 화면 머리: ◀ 뒤로 + 제목 (+ 부제목) + 오른쪽 actions */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(modifier.fillMaxWidth().padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                BackButton(onBack, color = Ink)
                Spacer(Modifier.width(2.dp))
            } else {
                Spacer(Modifier.width(12.dp))
            }
            AnimatedContent(
                targetState = title,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier.weight(1f),
                label = "headerTitle",
            ) { t ->
                Text(
                    t, style = MaterialTheme.typography.titleLarge, color = Ink,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            actions()
        }
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Muted,
                modifier = Modifier.padding(start = if (onBack != null) 50.dp else 12.dp),
            )
        }
    }
}

/** 고를 수 있는 칩. 고르면 남색으로 바뀐다 */
@Composable
fun MBChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int = 0,
    enabled: Boolean = true,
    @Suppress("UNUSED_PARAMETER") selectedBrush: Brush = SolidColor(Ink),
) {
    val shape = RoundedCornerShape(50)
    val fg by animateColorAsState(
        when {
            !enabled -> Faint
            selected -> Color.White
            else -> Ink
        },
        label = "chipFg",
    )
    val bg by animateColorAsState(if (selected) Ink else Color.White, label = "chipBg")
    val borderColor by animateColorAsState(if (selected) Ink else Line, label = "chipBorder")
    Row(
        modifier
            .bouncyClick(shape, enabled = enabled, onClick = onClick)
            .background(bg, shape)
            .border(Hairline, borderColor, shape)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 13.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, color = fg)
        if (count > 0) {
            Spacer(Modifier.width(6.dp))
            Text("$count", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (selected) Aqua300 else Aqua700)
        }
    }
}

/** 입력칸 공통 색 */
@Composable
fun mbFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Aqua500,
    unfocusedBorderColor = Line,
    disabledBorderColor = Line,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    disabledContainerColor = Field,
    cursorColor = Aqua700,
    focusedLabelColor = Aqua700,
    unfocusedLabelColor = Muted,
    focusedTextColor = Ink,
    unfocusedTextColor = Ink,
    focusedLeadingIconColor = Aqua700,
    unfocusedLeadingIconColor = Muted,
    focusedPlaceholderColor = Faint,
    unfocusedPlaceholderColor = Faint,
)

/** 카드 안 구역 이름 (작은 아이콘 + 회색 글자) */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, @DrawableRes icon: Int? = null) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            MBIcon(icon, size = 16.dp, tint = Aqua700)
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = MaterialTheme.typography.labelMedium, color = Muted)
    }
}

/**
 * 처음 보일 때 0 에서 차오르는 가로 막대.
 * @param fraction 0~1
 */
@Composable
fun FillBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    track: Color = Aqua100,
    fill: Brush = SolidColor(Aqua500),
) {
    val animated by animatedFill(fraction.coerceIn(0f, 1f))
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(track),
    ) {
        Box(
            Modifier
                .width(maxWidth * animated)
                .fillMaxHeight()
                .clip(RoundedCornerShape(50))
                .background(fill),
        )
    }
}

/** 이니셜이 들어간 동그라미 (사진이 없을 때 프로필) */
@Composable
fun GradientAvatar(text: String, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(Aqua100)
            .border(Hairline, Line, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(text.take(1), color = Aqua700, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.4f).sp)
    }
}

/** 작은 꼬리표 (날짜·개수 등) */
@Composable
fun SoftBadge(text: String, modifier: Modifier = Modifier, color: Color = Aqua100, textColor: Color = Aqua700) {
    Text(
        text,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = textColor,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}
