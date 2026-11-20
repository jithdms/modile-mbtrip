package com.example.mbtrip.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mbtrip.ui.design.MBButton
import com.example.mbtrip.ui.design.MBButtonStyle
import com.example.mbtrip.ui.design.MBCard
import com.example.mbtrip.ui.design.ScreenHeader
import com.example.mbtrip.ui.design.SoftBadge
import com.example.mbtrip.ui.theme.Ink
import com.example.mbtrip.ui.theme.InkSoft

/**
 * 1. 공통 버튼 (Primary / Secondary)
 *  - Primary  : 브랜드 아쿠아 그라데이션 + 빛 스침 + 통통 눌림
 *  - Secondary: 흰 바탕 + 아쿠아 테두리
 */
@Composable
fun MBTripButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = true,
    enabled: Boolean = true
) {
    MBButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        style = if (isPrimary) MBButtonStyle.Brand else MBButtonStyle.Outline,
    )
}

/**
 * 2. 공통 카드 (여행 정보 및 리스트 항목용 카드)
 */
@Composable
fun MBTripCard(
    title: String,
    subtitle: String,
    dateInfo: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MBCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = Ink, modifier = Modifier.weight(1f))
            SoftBadge(dateInfo)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = InkSoft)
    }
}

/**
 * 3. 공통 상단바 — 흰 동그라미 뒤로가기 + 굵은 제목 (배경은 화면 하늘이 비치도록 투명).
 * 상태바 여백은 MainActivity 의 Scaffold 가 이미 준다.
 */
@Composable
fun MBTripTopBar(
    title: String,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    ScreenHeader(
        title = title,
        onBack = onBackClick,
        actions = actions,
    )
}
