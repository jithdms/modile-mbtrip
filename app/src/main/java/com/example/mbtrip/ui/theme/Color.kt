package com.example.mbtrip.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor

/*
 * ───────────── MBTrip 컬러 시스템 ─────────────
 * 브랜드 색 #A7E7F5 (맑은 아쿠아 하늘) 을 가운데에 두고 밝은 쪽·진한 쪽으로 단계를 나눴다.
 *  - 배경·카드·강조 면  → Aqua50 ~ Aqua300
 *  - 흰 바탕 위 글자·아이콘 강조 → Aqua700 (흰 바탕 대비 4.5 이상)
 *  - 본문 글자·진한 버튼 → Ink (깊은 바다 남색)
 * 화면에서 색이 필요하면 Color(0xFF…) 를 새로 만들지 말고 여기 이름을 쓴다.
 */

// 아쿠아 단계
val Aqua50 = Color(0xFFF3FCFE)
val Aqua100 = Color(0xFFE4F8FC)
val Aqua200 = Color(0xFFCAF1F9)
val Aqua300 = Color(0xFFA7E7F5)   // ★ 브랜드 메인
val Aqua400 = Color(0xFF7FD9EE)
val Aqua500 = Color(0xFF4EC4E0)
val Aqua600 = Color(0xFF26A6C6)
val Aqua700 = Color(0xFF15809E)
val Aqua800 = Color(0xFF0E5F78)

// 글자·선
val Ink = Color(0xFF0F2A3A)         // 제목, 진한 버튼
val InkSoft = Color(0xFF3E5868)     // 본문
val Muted = Color(0xFF7D93A1)       // 보조 글자
val Faint = Color(0xFFB3C3CC)       // 안내 문구, 꺼진 상태
val Line = Color(0xFFE1EDF2)        // 구분선, 카드 테두리
val Field = Color(0xFFF0F7FA)       // 입력칸 바탕

// 포인트 (여행 느낌: 노을·햇살·민트·라일락)
val Coral = Color(0xFFFF8F7E)
val Sunny = Color(0xFFFFD36E)
val Mint = Color(0xFFB4EFCB)        // 자유 블록
val MintDeep = Color(0xFF3FAE76)
val Lilac = Color(0xFFC9C4FF)
val Danger = Color(0xFFE5484D)
val DangerSoft = Color(0xFFFDECEC)
val Warning = Color(0xFFE88A1A)
val WarningSoft = Color(0xFFFFF4E2)

// ── 예전 이름 (다른 화면에서 쓰고 있어서 남겨둠) ──
val Primary = Aqua300
val PrimaryVariant = Aqua400
val Secondary = Aqua700
val Background = Color(0xFFF7FAFB)
val Surface = Color(0xFFFFFFFF)
val TextPrimary = Ink
val TextSecondary = Muted

// ── 면 칠하기 (단색) ──
// 예전엔 그라데이션이었지만 심플한 톤을 위해 모두 단색으로 바꿨다. 이름은 다른 화면이 쓰고 있어서 유지.
object MBBrush {
    /** 메인 버튼: 브랜드 아쿠아 */
    val Primary: Brush = SolidColor(Aqua300)

    /** 진한 버튼: 남색 */
    val Deep: Brush = SolidColor(Ink)

    /** 화면 배경 */
    val Sky: Brush = SolidColor(Aqua50)

    /** 큰 제목 글자 */
    val Title: Brush = SolidColor(Ink)
}
