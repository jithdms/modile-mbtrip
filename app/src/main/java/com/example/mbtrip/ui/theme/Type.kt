package com.example.mbtrip.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.example.mbtrip.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Material 3 Typography — 제목은 굵고 촘촘하게, 본문은 넉넉한 줄간격 (한글 기준)
// 글자색은 넣지 않는다 (버튼 안 흰 글씨 등 LocalContentColor 를 따르도록)
// 글꼴: 프리텐다드 (res/font, SIL Open Font License)
val Pretendard = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold),
)

private val Base = TextStyle(fontFamily = Pretendard)

val Typography = Typography(
    displayLarge = Base.copy(fontWeight = FontWeight.Bold, fontSize = 52.sp, lineHeight = 60.sp, letterSpacing = (-1).sp),
    displayMedium = Base.copy(fontWeight = FontWeight.Bold, fontSize = 44.sp, lineHeight = 52.sp, letterSpacing = (-0.8).sp),
    headlineLarge = Base.copy(fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 38.sp, letterSpacing = (-0.6).sp),
    // 랜딩 로고, 큰 숫자
    displaySmall = Base.copy(fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 42.sp, letterSpacing = (-0.5).sp),
    // 화면 큰 제목
    headlineMedium = Base.copy(fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 34.sp, letterSpacing = (-0.6).sp),
    headlineSmall = Base.copy(fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 30.sp, letterSpacing = (-0.4).sp),
    // 대제목 (앱바 타이틀)
    titleLarge = Base.copy(fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 28.sp, letterSpacing = (-0.3).sp),
    // 중제목 (섹션 제목)
    titleMedium = Base.copy(fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 24.sp, letterSpacing = (-0.2).sp),
    titleSmall = Base.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 21.sp),
    // 본문
    bodyLarge = Base.copy(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.sp),
    bodyMedium = Base.copy(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp, letterSpacing = 0.sp),
    bodySmall = Base.copy(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp, letterSpacing = 0.sp),
    // 버튼·탭·칩
    labelLarge = Base.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.sp),
    labelMedium = Base.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.sp),
    labelSmall = Base.copy(fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = 0.2.sp),
)
