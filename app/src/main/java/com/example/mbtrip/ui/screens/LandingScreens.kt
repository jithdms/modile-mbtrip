package com.example.mbtrip.ui.screen

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.mbtrip.R
import com.example.mbtrip.navigation.Screen
import com.example.mbtrip.session.Account
import com.example.mbtrip.timeline.ui.TimelineColors
import com.example.mbtrip.ui.design.Hairline
import com.example.mbtrip.ui.design.MBButton
import com.example.mbtrip.ui.design.MBButtonStyle
import com.example.mbtrip.ui.design.MBIcon
import com.example.mbtrip.ui.design.bouncyClick
import com.example.mbtrip.ui.theme.Aqua100
import com.example.mbtrip.ui.theme.Aqua300
import com.example.mbtrip.ui.theme.Aqua500
import com.example.mbtrip.ui.theme.Aqua700
import com.example.mbtrip.ui.theme.Background
import com.example.mbtrip.ui.theme.Ink
import com.example.mbtrip.ui.theme.InkSoft
import com.example.mbtrip.ui.theme.Line
import com.example.mbtrip.ui.theme.Mint
import com.example.mbtrip.ui.theme.Muted

/*
 * 랜딩 — 세로로 긴 기능 소개 페이지 (웹 페이지처럼 스크롤에 맞춰 움직인다)
 *  - 맨 위 얇은 막대: 얼마나 내려왔는지
 *  - 대표 사진: 페이지보다 천천히 움직여 깊이감 (패럴랙스)
 *  - 구역마다: 화면에 들어오는 순간 아래에서 떠오르며 나타남 (기능은 왼쪽·오른쪽 번갈아)
 *  - 기능 미리보기: 나타난 뒤 안쪽이 움직임 (막대가 차오르고, 코드가 한 글자씩, 일정 블록이 하나씩 …)
 */

/** 웹에서 많이 쓰는 'ease-out' 곡선 */
private val EaseOut = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

@Composable
fun LandingScreen(navController: NavController) {
    // 이미 로그인했으면 바로 다음 단계로
    val start = {
        if (Account.isSignedIn) navController.navigate(Screen.AfterLogin.route) else navController.navigate(Screen.Login.route)
    }
    val list = rememberLazyListState()
    val itemCount = 12

    Column(Modifier.fillMaxSize().background(Color.White)) {
        // ① 위 줄 (고정) + 스크롤 진행 막대
        val scrolled by remember { derivedStateOf { list.firstVisibleItemIndex > 0 || list.firstVisibleItemScrollOffset > 8 } }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("MBTrip", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
            Text(
                if (Account.isSignedIn) "내 여행" else "로그인",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Aqua700,
                modifier = Modifier
                    .bouncyClick(RoundedCornerShape(8.dp), onClick = start)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
        ScrollProgress(list, itemCount, visibleLine = scrolled)

        LazyColumn(
            state = list,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 40.dp),
        ) {
            // ② 대표 사진 + 소개
            item {
                Column(Modifier.padding(horizontal = 20.dp).padding(top = 8.dp)) {
                    ParallaxPhoto(list)
                    Spacer(Modifier.height(24.dp))
                    Reveal(list, 0, delayMs = 150) {
                        Text("TRAVEL PLANNER", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp, color = Aqua700)
                    }
                    Spacer(Modifier.height(6.dp))
                    Reveal(list, 0, delayMs = 250) {
                        Text("성향이 달라도\n함께 짜는 여행 일정", style = MaterialTheme.typography.headlineMedium, color = Ink)
                    }
                    Spacer(Modifier.height(10.dp))
                    Reveal(list, 0, delayMs = 350) {
                        Text(
                            "계획형(J)과 유연형(P)이 함께 떠나도 괜찮아요. MBTrip이 동행자 성향에 맞춰 " +
                                "확정 일정과 자유 시간의 비율을 잡아 드려요.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkSoft,
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Reveal(list, 0, delayMs = 450) {
                        MBButton(text = "시작하기", onClick = start, style = MBButtonStyle.Brand)
                    }
                    Spacer(Modifier.height(18.dp))
                    // 아래로 내려 보라는 안내
                    Reveal(list, 0, delayMs = 700) {
                        Text(
                            "아래로 내려서 기능을 둘러보세요",
                            fontSize = 12.sp, color = Muted, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            item { SectionDivider() }

            item {
                Reveal(list, 2) {
                    Text(
                        "MBTrip으로 할 수 있는 것",
                        style = MaterialTheme.typography.titleLarge,
                        color = Ink,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
            }

            // ③ 기능 소개 (왼쪽·오른쪽 번갈아 들어온다)
            item {
                FeatureBlock(list, 3, fromLeft = true, number = "01", icon = R.drawable.ic_mb_compass, title = "여행 성향 진단",
                    body = "10개 질문에 답하면 나는 계획형(J)인지 유연형(P)인지 알려 드려요. 결과는 일정 비율을 정할 때 쓰여요.") { on ->
                    DiagnosisPreview(on)
                }
            }
            item {
                FeatureBlock(list, 4, fromLeft = false, number = "02", icon = R.drawable.ic_mb_key, title = "여행 방 만들기 · 초대코드",
                    body = "출발지·목적지·날짜·인원만 고르면 방이 생겨요. 6자리 초대코드를 보내면 친구가 바로 들어와요.") { on ->
                    CodePreview(on)
                }
            }
            item {
                FeatureBlock(list, 5, fromLeft = true, number = "03", icon = R.drawable.ic_mb_calendar, title = "확정 · 자유 일정표",
                    body = "꼭 가야 할 곳은 확정 블록, 그때그때 정할 시간은 자유 블록. 동행자 성향에 맞는 비율도 함께 보여 드려요.") { on ->
                    TimelinePreview(on)
                }
            }
            item {
                FeatureBlock(list, 6, fromLeft = false, number = "04", icon = R.drawable.ic_mb_vote, title = "일정 투표",
                    body = "바꾸고 싶은 일정은 혼자 고치지 말고 제안하세요. 동행자가 찬성·반대로 의견을 남기고 함께 정해요.") { on ->
                    VotePreview(on)
                }
            }
            item {
                FeatureBlock(list, 7, fromLeft = true, number = "05", icon = R.drawable.ic_mb_photos, title = "여행 로그",
                    body = "다녀온 여행의 사진과 후기를 한곳에 모아 둬요. 날짜나 장소로 찾아볼 수 있어요.") { on ->
                    PhotosPreview(on)
                }
            }

            item { SectionDivider() }

            // ④ 이용 순서 — 단계가 하나씩, 사이 선이 그어지며
            item {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    Reveal(list, 9) { Text("이렇게 진행돼요", style = MaterialTheme.typography.titleLarge, color = Ink) }
                    Spacer(Modifier.height(16.dp))
                    val steps = listOf(
                        "성향 진단" to "처음 한 번, 1분이면 끝나요",
                        "여행 방 만들기" to "목적지와 날짜, 인원을 골라요",
                        "친구 초대" to "초대코드를 보내거나 아이디로 찾아요",
                        "함께 일정 짜기" to "블록을 추가하고 투표로 정해요",
                    )
                    steps.forEachIndexed { i, (t, d) ->
                        StepRow(list, 9, i, t, d, last = i == steps.lastIndex)
                    }
                }
            }

            // ⑤ 맨 아래 시작하기 — 살짝 작게 시작해 커지며
            item {
                Reveal(list, 10, scaleFrom = 0.92f) {
                    Column(
                        Modifier
                            .padding(start = 20.dp, end = 20.dp, top = 32.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Aqua100)
                            .border(Hairline, Aqua300, RoundedCornerShape(20.dp))
                            .padding(24.dp),
                    ) {
                        Text("첫 여행을 만들어 볼까요?", style = MaterialTheme.typography.titleLarge, color = Ink)
                        Spacer(Modifier.height(6.dp))
                        Text("가입하고 성향 진단을 마치면 바로 여행 방을 만들 수 있어요.", style = MaterialTheme.typography.bodyMedium, color = InkSoft)
                        Spacer(Modifier.height(18.dp))
                        MBButton(text = "시작하기", onClick = start, style = MBButtonStyle.Deep)
                    }
                }
            }
            item {
                Text(
                    "MBTrip · 친구와 함께 짜는 여행 일정표",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/* ───────────────────────── 스크롤 애니메이션 ───────────────────────── */

/**
 * 목록의 index 번째 줄이 화면 아래쪽 12% 선을 넘어 들어오면 true.
 * (한 번 보이면 계속 true — 다시 올라가도 사라지지 않는다)
 */
@Composable
private fun rememberRevealed(list: LazyListState, index: Int): Boolean {
    val inView by remember(index) {
        derivedStateOf {
            val info = list.layoutInfo
            val item = info.visibleItemsInfo.firstOrNull { it.index == index } ?: return@derivedStateOf false
            val trigger = info.viewportEndOffset - (info.viewportEndOffset - info.viewportStartOffset) * 0.12f
            item.offset < trigger
        }
    }
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(inView) { if (inView) revealed = true }
    return revealed
}

/**
 * 화면에 들어오면 떠오르며 나타나는 상자.
 * @param fromX 옆에서 들어올 거리 (+ 오른쪽에서, - 왼쪽에서)
 */
@Composable
private fun Reveal(
    list: LazyListState,
    index: Int,
    delayMs: Int = 0,
    fromY: Dp = 48.dp,
    fromX: Dp = 0.dp,
    scaleFrom: Float = 1f,
    content: @Composable () -> Unit,
) {
    val revealed = rememberRevealed(list, index)
    val p by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(800, delayMillis = delayMs, easing = EaseOut),
        label = "reveal",
    )
    val density = LocalDensity.current
    val dy = with(density) { fromY.toPx() }
    val dx = with(density) { fromX.toPx() }
    Box(
        Modifier.graphicsLayer {
            alpha = p
            translationY = (1f - p) * dy
            translationX = (1f - p) * dx
            val s = scaleFrom + (1f - scaleFrom) * p
            scaleX = s
            scaleY = s
        },
    ) { content() }
}

/** 맨 위 얇은 막대 — 얼마나 내려왔는지 */
@Composable
private fun ScrollProgress(list: LazyListState, itemCount: Int, visibleLine: Boolean) {
    val progress by remember {
        derivedStateOf {
            val info = list.layoutInfo
            val first = info.visibleItemsInfo.firstOrNull()
            if (first == null || itemCount <= 1) 0f
            else {
                val within = if (first.size > 0) (-first.offset).toFloat() / first.size else 0f
                ((list.firstVisibleItemIndex + within) / (itemCount - 1).coerceAtLeast(1)).coerceIn(0f, 1f)
            }
        }
    }
    val shown by animateFloatAsState(progress, tween(120), label = "scrollProgress")
    Box(
        Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(if (visibleLine) Line else Color.Transparent),
    ) {
        Box(Modifier.fillMaxWidth(shown).fillMaxHeight().background(Aqua500))
    }
}

/** 대표 사진 — 페이지보다 천천히 올라가고, 처음엔 살짝 확대된 상태에서 제자리로 */
@Composable
private fun ParallaxPhoto(list: LazyListState) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val intro by animateFloatAsState(if (started) 1f else 0f, tween(1200, easing = EaseOut), label = "heroIntro")
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(20.dp))
            .background(Aqua100),
    ) {
        Image(
            painter = painterResource(R.drawable.travel_main_photo),
            contentDescription = "친구들과 여행하는 사진",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // 스크롤한 만큼의 35% 만 따라 내려가서, 상자 안에서 사진이 천천히 움직이는 것처럼 보인다
                    val scroll = if (list.firstVisibleItemIndex == 0) list.firstVisibleItemScrollOffset.toFloat() else size.height
                    translationY = scroll * 0.35f
                    val s = 1.18f - 0.06f * intro
                    scaleX = s
                    scaleY = s
                    alpha = intro
                },
        )
    }
}

@Composable
private fun SectionDivider() {
    Box(
        Modifier
            .padding(vertical = 32.dp)
            .fillMaxWidth()
            .height(8.dp)
            .background(Background),
    )
}

/** 기능 하나: 번호 · 아이콘 · 제목 · 설명이 옆에서 들어오고, 이어서 미리보기 카드가 올라온다 */
@Composable
private fun FeatureBlock(
    list: LazyListState,
    index: Int,
    fromLeft: Boolean,
    number: String,
    @DrawableRes icon: Int,
    title: String,
    body: String,
    preview: @Composable ColumnScope.(revealed: Boolean) -> Unit,
) {
    val side = if (fromLeft) (-40).dp else 40.dp
    val revealed = rememberRevealed(list, index)
    // 미리보기 속 움직임은 카드가 올라온 뒤에 시작
    var previewOn by remember { mutableStateOf(false) }
    LaunchedEffect(revealed) {
        if (revealed) {
            kotlinx.coroutines.delay(450)
            previewOn = true
        }
    }
    Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        Reveal(list, index, fromY = 16.dp, fromX = side) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Aqua100),
                    contentAlignment = Alignment.Center,
                ) { MBIcon(icon, size = 20.dp, tint = Aqua700) }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(number, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Aqua700)
                    Text(title, style = MaterialTheme.typography.titleMedium, color = Ink)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Reveal(list, index, delayMs = 100, fromY = 16.dp, fromX = side) {
            Text(body, style = MaterialTheme.typography.bodyMedium, color = InkSoft)
        }
        Spacer(Modifier.height(14.dp))
        Reveal(list, index, delayMs = 220, fromY = 56.dp) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Background)
                    .border(Hairline, Line, RoundedCornerShape(16.dp))
                    .padding(16.dp),
            ) { preview(previewOn) }
        }
    }
}

/* ───────────────────────── 미리보기 (나타난 뒤 안쪽이 움직임) ───────────────────────── */

/** 0 → 1 (on 이 되면) */
@Composable
private fun progressOn(on: Boolean, durationMs: Int = 900, delayMs: Int = 0) =
    animateFloatAsState(if (on) 1f else 0f, tween(durationMs, delayMillis = delayMs, easing = EaseOut), label = "preview")

/** 01: J/P 막대가 차오르고 숫자가 올라간다 */
@Composable
private fun DiagnosisPreview(on: Boolean) {
    val p by progressOn(on, 1200)
    val j = (35 * p).toInt()
    Row {
        Text("J · 계획형  $j%", fontSize = 12.sp, color = Aqua700, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text("${(65 * p).toInt()}%  유연형 · P", fontSize = 12.sp, color = Color(0xFF2F7A55), fontWeight = FontWeight.SemiBold)
    }
    Spacer(Modifier.height(8.dp))
    BoxWithConstraints(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)).background(Line)) {
        val w = maxWidth
        Row(Modifier.fillMaxHeight()) {
            Box(Modifier.width(w * 0.35f * p).fillMaxHeight().background(Aqua500))
            Box(Modifier.width(w * 0.65f * p).fillMaxHeight().background(Mint))
        }
    }
    Spacer(Modifier.height(10.dp))
    val t by progressOn(on, 500, delayMs = 900)
    Text("유연한 탐험가", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.graphicsLayer { alpha = t })
}

/** 02: 초대코드가 한 글자씩 */
@Composable
private fun CodePreview(on: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        "aB3x9Q".forEachIndexed { i, ch ->
            val p by progressOn(on, 420, delayMs = i * 110)
            Box(
                Modifier
                    .size(width = 36.dp, height = 44.dp)
                    .graphicsLayer {
                        alpha = p
                        translationY = (1f - p) * 18.dp.toPx()
                    }
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .border(Hairline, if (p > 0.95f) Aqua300 else Line, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) { Text(ch.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Ink) }
        }
    }
    Spacer(Modifier.height(10.dp))
    Text("친구에게 코드를 보내면 같은 방에 들어와요", fontSize = 12.sp, color = Muted)
}

/** 03: 일정 블록이 하나씩 옆에서 */
@Composable
private fun TimelinePreview(on: Boolean) {
    PreviewBlock(on, 0, "10:00", "오이도 빨간등대", "확정", TimelineColors.Fixed)
    Spacer(Modifier.height(6.dp))
    PreviewBlock(on, 1, "12:00", "자유 시간", "자유", TimelineColors.Free)
    Spacer(Modifier.height(6.dp))
    PreviewBlock(on, 2, "14:30", "거북섬 마리나", "확정", TimelineColors.Fixed)
}

@Composable
private fun PreviewBlock(on: Boolean, order: Int, time: String, title: String, tag: String, color: Color) {
    val p by progressOn(on, 600, delayMs = order * 180)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.graphicsLayer {
            alpha = p
            translationX = (1f - p) * 60.dp.toPx()
        },
    ) {
        Text(time, fontSize = 11.sp, color = Muted, modifier = Modifier.width(44.dp))
        Row(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(color)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink, modifier = Modifier.weight(1f))
            Text(tag, fontSize = 11.sp, color = InkSoft)
        }
    }
}

/** 04: 찬성·반대 숫자가 올라간다 */
@Composable
private fun VotePreview(on: Boolean) {
    Text("저녁 장소를 '배곧 한울공원'으로 바꿀까요?", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink)
    Spacer(Modifier.height(10.dp))
    val p by progressOn(on, 1000)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        VoteChip("찬성 ${(3 * p).toInt()}", Mint, p, Modifier.weight(1f))
        VoteChip("반대 ${(1 * p).toInt()}", Color(0xFFFFE1DC), p, Modifier.weight(1f))
    }
}

@Composable
private fun VoteChip(text: String, color: Color, p: Float, modifier: Modifier) {
    Box(
        modifier
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .border(Hairline, Line, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        // 색이 왼쪽부터 차오른다
        Box(Modifier.matchParentSize().graphicsLayer { scaleX = p; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f) }.background(color))
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink)
    }
}

/** 05: 사진 두 장이 확대됐다가 제자리로 */
@Composable
private fun PhotosPreview(on: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(R.drawable.photo1, R.drawable.photo2).forEachIndexed { i, res ->
            val p by progressOn(on, 900, delayMs = i * 160)
            Box(
                Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Aqua100),
            ) {
                Image(
                    painter = painterResource(res),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = p
                            val s = 1.25f - 0.25f * p
                            scaleX = s
                            scaleY = s
                        },
                )
            }
        }
    }
}

/** 이용 순서 한 단계 — 번호가 나타나고, 아래로 이어지는 선이 그어진다 */
@Composable
private fun StepRow(list: LazyListState, index: Int, order: Int, title: String, desc: String, last: Boolean) {
    val revealed = rememberRevealed(list, index)
    val p by animateFloatAsState(if (revealed) 1f else 0f, tween(600, delayMillis = 200 + order * 220, easing = EaseOut), label = "step")
    val line by animateFloatAsState(if (revealed) 1f else 0f, tween(400, delayMillis = 450 + order * 220, easing = EaseOut), label = "stepLine")
    Row(Modifier.graphicsLayer { alpha = p; translationX = (1f - p) * 24.dp.toPx() }) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(28.dp)
                    .graphicsLayer { scaleX = 0.6f + 0.4f * p; scaleY = 0.6f + 0.4f * p }
                    .clip(CircleShape)
                    .background(Ink),
                contentAlignment = Alignment.Center,
            ) { Text("${order + 1}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White) }
            if (!last) {
                Box(Modifier.width(Hairline).height(36.dp)) {
                    Box(
                        Modifier
                            .matchParentSize()
                            .graphicsLayer { scaleY = line; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f) }
                            .background(Line),
                    )
                }
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.padding(top = 3.dp, bottom = if (last) 0.dp else 12.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Text(desc, fontSize = 13.sp, color = Muted)
        }
    }
}
