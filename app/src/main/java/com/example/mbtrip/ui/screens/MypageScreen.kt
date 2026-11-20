package com.example.mbtrip.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.mbtrip.R
import com.example.mbtrip.session.Account
import com.example.mbtrip.session.MyProfile
import com.example.mbtrip.session.MyTrips
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.ui.text.style.TextOverflow
import com.example.mbtrip.session.TestAccess
import com.example.mbtrip.triplog.SampleTripLogs
import com.example.mbtrip.triplog.TripLog
import com.example.mbtrip.triplog.TripLogDependencies
import com.example.mbtrip.triplog.TripLogStore
import com.example.mbtrip.triplog.formatDateRange
import com.example.mbtrip.triplog.photoModel
import com.example.mbtrip.session.UserProfile
import com.example.mbtrip.ui.component.MBTripTopBar
import com.example.mbtrip.ui.design.FillBar
import com.example.mbtrip.ui.design.Hairline
import com.example.mbtrip.ui.design.MBButton
import com.example.mbtrip.ui.design.MBButtonStyle
import com.example.mbtrip.ui.design.MBCard
import com.example.mbtrip.ui.design.MBIcon
import com.example.mbtrip.ui.design.SoftBadge
import com.example.mbtrip.ui.design.MBMonthCalendar
import com.example.mbtrip.ui.design.SectionLabel
import com.example.mbtrip.ui.design.animatedCount
import com.example.mbtrip.ui.design.bouncyClick
import com.example.mbtrip.ui.design.enterFromBelow
import com.example.mbtrip.ui.design.mbDateText
import com.example.mbtrip.ui.design.mbFieldColors
import com.example.mbtrip.ui.theme.Aqua100
import com.example.mbtrip.ui.theme.Aqua500
import com.example.mbtrip.ui.theme.Aqua700
import com.example.mbtrip.ui.theme.Background
import com.example.mbtrip.ui.theme.Danger
import com.example.mbtrip.ui.theme.DangerSoft
import com.example.mbtrip.ui.theme.Ink
import com.example.mbtrip.ui.theme.InkSoft
import com.example.mbtrip.ui.theme.Line
import com.example.mbtrip.ui.theme.Mint
import com.example.mbtrip.ui.theme.Muted
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/**
 * 마이페이지
 *  ① 프로필 (사진 · 이름 · @아이디 · 이메일 · 한 줄 소개) + [프로필 수정]
 *  ② 나의 여행 성향 (진단 점수) + [다시 진단하기]
 *  ③ 내 여행 달력 (내 여행 목록의 날짜가 표시됨)
 *  ④ 바로가기: 내 여행 · 친구 · 알림
 *  ⑤ 계정: 로그아웃 · 회원 탈퇴 · 앱 버전
 * 프로필은 Firestore users/{uid} 에서 읽고 쓴다 (session/Account.kt).
 */
@Composable
fun MyPageScreen(
    onBackClick: () -> Unit = {},
    onReTestClick: () -> Unit = {},
    onOpenMyTrips: () -> Unit = {},
    onOpenFriends: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenLogin: () -> Unit = {},
    /** 로그아웃 · 탈퇴가 끝난 뒤 */
    onSignedOut: () -> Unit = {},
    /** 달력 아래 로그를 눌렀을 때 → 로그 상세 (TripLogStore.selected 에 넣어 둔 로그) */
    onOpenLog: () -> Unit = {},
    /** 로그를 아직 안 남긴 지난 여행 → 후기 쓰기 */
    onWriteReview: (com.example.mbtrip.session.MyTrip) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profile = Account.profile
    val signedIn = Account.isSignedIn
    val jpScore = profile?.jpScore ?: UserProfile.jpScore(context)

    var editing by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    // 내 여행 로그 (테스트 계정은 예시 로그도)
    val tester = TestAccess.isTester
    val logs by produceState(initialValue = TripLogStore.cachedLogs.orEmpty(), key1 = tester) {
        val fromDb = runCatching { TripLogDependencies.repository.loadLogs() }.getOrNull()
        value = when {
            fromDb == null -> if (tester) SampleTripLogs.logs else value
            tester -> (fromDb + SampleTripLogs.logs).distinctBy { it.key }
            else -> fromDb
        }.filterNot { it.key in TripLogStore.deletedKeys } // 지운 로그는 빼고
    }

    // 달력에 표시할 날짜 = 내 여행 기간 + 로그가 있는 여행 기간
    val trips = MyTrips.trips
    val tripDays = remember(trips, logs) {
        fun span(s: LocalDate, e: LocalDate) = generateSequence(s) { d -> d.plusDays(1).takeIf { !it.isAfter(e) } }.toList()
        (trips.flatMap { t -> t.startDate?.let { s -> span(s, t.endDate ?: s) }.orEmpty() } +
            logs.flatMap { span(it.startDate, it.endDate) }).toSet()
    }
    val today = LocalDate.now()
    val nextTrip = trips.filter { (it.endDate ?: it.startDate)?.isBefore(today) == false }
        .minByOrNull { it.startDate ?: today }
    // 처음 보여줄 달: 다가오는 여행 → 가장 최근 로그 → 이번 달
    var month by remember { mutableStateOf(YearMonth.from(nextTrip?.startDate ?: today)) }
    var monthTouched by remember { mutableStateOf(false) }
    LaunchedEffect(logs) {
        if (!monthTouched && nextTrip == null) logs.maxByOrNull { it.startDate }?.let { month = YearMonth.from(it.startDate) }
    }
    // 달력에서 누른 날 → 그날 여행 로그 목록
    var pickedDay by remember { mutableStateOf<LocalDate?>(null) }
    val dayLogs = remember(pickedDay, logs) {
        val d = pickedDay ?: return@remember emptyList<TripLog>()
        logs.filter { !d.isBefore(it.startDate) && !d.isAfter(it.endDate) }
    }
    val dayTrips = remember(pickedDay, trips, dayLogs) {
        val d = pickedDay ?: return@remember emptyList<com.example.mbtrip.session.MyTrip>()
        trips.filter { t ->
            val s = t.startDate ?: return@filter false
            !d.isBefore(s) && !d.isAfter(t.endDate ?: s) && dayLogs.none { it.tripId == t.tripId }
        }
    }

    Column(Modifier.fillMaxSize().background(Background)) {
        MBTripTopBar(title = "마이페이지", onBackClick = onBackClick)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ① 프로필
            MBCard(Modifier.fillMaxWidth().enterFromBelow(0), contentPadding = PaddingValues(18.dp)) {
                if (signedIn && profile != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProfilePhoto(profile.photoData, null, 64.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(profile.nickname, style = MaterialTheme.typography.titleLarge, color = Ink)
                            Text(profile.handle, fontSize = 13.sp, color = Aqua700, fontWeight = FontWeight.SemiBold)
                            Text(profile.email, fontSize = 12.sp, color = Muted)
                        }
                    }
                    if (profile.bio.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(profile.bio, fontSize = 14.sp, color = InkSoft)
                    }
                    Spacer(Modifier.height(14.dp))
                    MBButton(
                        text = "프로필 수정",
                        onClick = { editing = true },
                        style = MBButtonStyle.Outline,
                        height = 44.dp,
                        leadingIcon = R.drawable.ic_mb_edit,
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProfilePhoto(null, null, 56.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("로그인이 필요해요", style = MaterialTheme.typography.titleMedium, color = Ink)
                            Text("로그인하면 프로필과 여행이 계정에 남아요.", fontSize = 13.sp, color = Muted)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    MBButton(text = "로그인 · 회원가입", onClick = onOpenLogin, style = MBButtonStyle.Brand, height = 46.dp)
                }
            }

            // ② 나의 여행 성향
            MBCard(Modifier.fillMaxWidth().enterFromBelow(1), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionLabel("나의 여행 성향", icon = R.drawable.ic_mb_compass)
                if (jpScore != null) {
                    val p = jpScore.coerceIn(0, 100)
                    val shownP by animatedCount(p)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            if (p >= 50) "P" else "J",
                            fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Ink,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            com.example.mbtrip.timeline.ui.jpLabel(p),
                            fontSize = 14.sp, color = InkSoft,
                            modifier = Modifier.padding(bottom = 6.dp).weight(1f),
                        )
                        Text("P $shownP%", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ink, modifier = Modifier.padding(bottom = 6.dp))
                    }
                    Column {
                        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                            Text("J · 계획형 ${100 - p}%", fontSize = 12.sp, color = Aqua700, modifier = Modifier.weight(1f))
                            Text("P · 유연형 $p%", fontSize = 12.sp, color = Color(0xFF2F7A55))
                        }
                        // 왼쪽 = J, 오른쪽 = P
                        FillBar(
                            fraction = (100 - p) / 100f,
                            height = 8.dp,
                            track = Mint,
                            fill = androidx.compose.ui.graphics.SolidColor(Aqua500),
                        )
                    }
                    MBButton(text = "다시 진단하기", onClick = onReTestClick, style = MBButtonStyle.Deep, height = 46.dp, leadingIcon = R.drawable.ic_mb_compass)
                } else {
                    Text("아직 진단하지 않았어요. 10개 질문이면 끝나요.", fontSize = 14.sp, color = InkSoft)
                    MBButton(text = "진단하기", onClick = onReTestClick, style = MBButtonStyle.Deep, height = 46.dp, leadingIcon = R.drawable.ic_mb_compass)
                }
            }

            // ③ 내 여행 달력
            MBCard(Modifier.fillMaxWidth().enterFromBelow(2), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("내 여행 달력", icon = R.drawable.ic_mb_calendar, modifier = Modifier.weight(1f))
                    Text(
                        "전체 보기",
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Aqua700,
                        modifier = Modifier.bouncyClick(RoundedCornerShape(8.dp), onClick = onOpenMyTrips).padding(4.dp),
                    )
                }
                MBMonthCalendar(
                    month = month,
                    onMonthChange = { month = it; monthTouched = true },
                    start = pickedDay,
                    end = null,
                    // 여행 간 날(하늘색)을 누르면 그날 로그가 아래에. 한 번 더 누르면 닫힘
                    onDayClick = { d -> pickedDay = if (pickedDay == d) null else d },
                    marked = tripDays,
                )
                AnimatedContent(
                    targetState = pickedDay,
                    transitionSpec = { (fadeIn() + expandVertically()) togetherWith (fadeOut() + shrinkVertically()) },
                    label = "dayLogs",
                ) { day ->
                    if (day == null) {
                        Text(
                            if (tripDays.isEmpty()) "여행을 다녀오면 그 날짜가 하늘색으로 표시돼요."
                            else "하늘색 날짜를 누르면 그때 남긴 여행 로그가 보여요.",
                            fontSize = 12.sp, color = Muted,
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("${day.mbDateText()} 여행 로그", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                            if (dayLogs.isEmpty() && dayTrips.isEmpty()) {
                                Text("이날은 여행 기록이 없어요.", fontSize = 13.sp, color = Muted)
                            }
                            dayLogs.forEachIndexed { i, log ->
                                DayLogRow(log, Modifier.enterFromBelow(i, distance = 10.dp)) {
                                    TripLogStore.select(log, 0)
                                    onOpenLog()
                                }
                            }
                            dayTrips.forEach { t ->
                                val ended = (t.endDate ?: t.startDate)?.isBefore(today) == true
                                DayTripRow(t, canReview = ended, onClick = { if (ended) onWriteReview(t) else onOpenMyTrips() })
                            }
                        }
                    }
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Background)
                        .border(Hairline, Line, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                ) {
                    if (nextTrip != null) {
                        Column {
                            Text("다음 여행", fontSize = 11.sp, color = Muted)
                            Text(nextTrip.destination, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                            nextTrip.startDate?.let { s ->
                                val e = nextTrip.endDate ?: s
                                Text(if (e == s) s.mbDateText() else "${s.mbDateText()} ~ ${e.mbDateText()}", fontSize = 12.sp, color = InkSoft)
                            }
                        }
                    } else {
                        Text("다가오는 여행이 없어요. 방 만들기에서 새 여행을 만들어 보세요.", fontSize = 13.sp, color = Muted)
                    }
                }
            }

            // ④ 바로가기
            MBCard(Modifier.fillMaxWidth().enterFromBelow(3), contentPadding = PaddingValues(vertical = 6.dp)) {
                MenuRow(R.drawable.ic_mb_luggage, "내 여행", "${trips.size}개", onClick = onOpenMyTrips)
                MenuDivider()
                MenuRow(R.drawable.ic_mb_group, "친구", null, onClick = onOpenFriends)
                MenuDivider()
                MenuRow(R.drawable.ic_mb_bell, "알림", null, onClick = onOpenNotifications)
            }

            // ⑤ 계정
            if (signedIn) {
                MBCard(Modifier.fillMaxWidth().enterFromBelow(4), contentPadding = PaddingValues(vertical = 6.dp)) {
                    MenuRow(R.drawable.ic_mb_logout, "로그아웃", null) { confirmLogout = true }
                    MenuDivider()
                    MenuRow(R.drawable.ic_mb_delete, "회원 탈퇴", null, danger = true) { confirmDelete = true }
                }
            }
            Text(
                "MBTrip ${appVersion(context)}",
                fontSize = 12.sp, color = Muted,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }

    // 프로필 수정
    if (editing && profile != null) {
        EditProfileDialog(
            profile = profile,
            onDismiss = { editing = false },
            onSave = { name, handle, bio, photo, done ->
                scope.launch {
                    val problem = Account.updateProfile(context, name, handle, bio, photo)
                    done(problem)
                    if (problem == null) editing = false
                }
            },
        )
    }

    // 로그아웃 확인
    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = { Text("로그아웃할까요?", fontWeight = FontWeight.Bold, color = Ink) },
            text = { Text("이 폰에 남은 내 여행 목록과 진단 결과도 지워져요. 다시 로그인하면 계정에서 불러와요.", color = InkSoft) },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    Account.signOut(context)
                    onSignedOut()
                }) { Text("로그아웃", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("취소", color = Muted) } },
        )
    }

    // 회원 탈퇴 확인 (비밀번호 다시 입력)
    if (confirmDelete) {
        var password by remember { mutableStateOf("") }
        var busy by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { if (!busy) confirmDelete = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = { Text("회원 탈퇴", fontWeight = FontWeight.Bold, color = Ink) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("프로필과 내 여행 목록이 삭제되고 되돌릴 수 없어요. 함께 만든 여행 방의 일정은 동행자를 위해 남아요.", color = InkSoft, fontSize = 14.sp)
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("비밀번호 확인") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(12.dp),
                        colors = mbFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    AnimatedVisibility(visible = error != null) {
                        Text(
                            error.orEmpty(), fontSize = 13.sp, color = Danger,
                            modifier = Modifier.fillMaxWidth().background(DangerSoft, RoundedCornerShape(8.dp)).padding(10.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !busy && password.isNotBlank(),
                    onClick = {
                        scope.launch {
                            busy = true
                            error = Account.deleteAccount(context, password)
                            busy = false
                            if (error == null) {
                                confirmDelete = false
                                onSignedOut()
                            }
                        }
                    },
                ) { Text(if (busy) "탈퇴하는 중…" else "탈퇴하기", fontWeight = FontWeight.Bold, color = Danger) }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { confirmDelete = false }) { Text("취소", color = Muted) } },
        )
    }
}

/* ───────────────────────── 부품 ───────────────────────── */

/** 프로필 사진: 새로 고른 사진(uri) > 계정 사진(Base64) > 기본 사진 */
@Composable
private fun ProfilePhoto(photoData: String?, picked: Uri?, size: androidx.compose.ui.unit.Dp) {
    val bitmap = remember(photoData) { Account.decodePhoto(photoData)?.asImageBitmap() }
    val mod = Modifier
        .size(size)
        .clip(CircleShape)
        .border(Hairline, Line, CircleShape)
    when {
        picked != null -> AsyncImage(model = picked, contentDescription = "프로필 사진", modifier = mod, contentScale = ContentScale.Crop)
        bitmap != null -> Image(bitmap = bitmap, contentDescription = "프로필 사진", modifier = mod, contentScale = ContentScale.Crop)
        else -> Image(painter = painterResource(R.drawable.default_avatar), contentDescription = "기본 프로필 사진", modifier = mod, contentScale = ContentScale.Crop)
    }
}

@Composable
private fun MenuRow(@DrawableRes icon: Int, title: String, trailing: String?, danger: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .bouncyClick(RoundedCornerShape(0.dp), onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MBIcon(icon, size = 20.dp, tint = if (danger) Danger else Ink)
        Spacer(Modifier.width(14.dp))
        Text(title, fontSize = 15.sp, color = if (danger) Danger else Ink, modifier = Modifier.weight(1f))
        if (trailing != null) {
            Text(trailing, fontSize = 13.sp, color = Muted)
            Spacer(Modifier.width(6.dp))
        }
        MBIcon(R.drawable.ic_mb_chevron_right, size = 18.dp, tint = Muted)
    }
}

/** 달력 아래 로그 한 줄: 사진 · 제목 · 장소 · 기간 · 만족도 */
@Composable
private fun DayLogRow(log: TripLog, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier
            .fillMaxWidth()
            .bouncyClick(shape, onClick = onClick)
            .background(Color.White, shape)
            .border(Hairline, Line, shape)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val model = remember(log.photos) { photoModel(context, log.photos.firstOrNull()) }
        AsyncImage(
            model = model ?: R.drawable.travel_main_photo,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Background)
                .border(Hairline, Line, RoundedCornerShape(10.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(log.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${log.destination} · ${formatDateRange(log.startDate, log.endDate)}",
                fontSize = 12.sp, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            if (log.satisfaction > 0) {
                Text("만족도 ${"★".repeat(log.satisfaction)}${"☆".repeat(5 - log.satisfaction)}", fontSize = 11.sp, color = Aqua700)
            }
        }
        if (log.photos.size > 1) SoftBadge("사진 ${log.photos.size}")
        Spacer(Modifier.width(4.dp))
        MBIcon(R.drawable.ic_mb_chevron_right, size = 18.dp, tint = Muted)
    }
}

/** 로그는 아직 없는 여행 */
@Composable
private fun DayTripRow(trip: com.example.mbtrip.session.MyTrip, canReview: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .bouncyClick(shape, onClick = onClick)
            .background(Background, shape)
            .border(Hairline, Line, shape)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MBIcon(R.drawable.ic_mb_luggage, size = 20.dp, tint = Aqua700)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("${trip.destination} 여행", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Text(if (canReview) "아직 후기가 없어요 · 눌러서 후기 쓰기" else "아직 남긴 로그가 없어요", fontSize = 12.sp, color = if (canReview) Aqua700 else Muted)
        }
        MBIcon(R.drawable.ic_mb_chevron_right, size = 18.dp, tint = Muted)
    }
}

@Composable
private fun MenuDivider() {
    HorizontalDivider(Modifier.padding(start = 52.dp), color = Line, thickness = Hairline)
}

/** 프로필 수정 모달 — 사진 · 이름 · 아이디 · 한 줄 소개 */
@Composable
private fun EditProfileDialog(
    profile: MyProfile,
    onDismiss: () -> Unit,
    onSave: (name: String, handle: String, bio: String, photo: Uri?, done: (String?) -> Unit) -> Unit,
) {
    var name by remember { mutableStateOf(profile.nickname) }
    var handle by remember { mutableStateOf(profile.handle.removePrefix("@")) }
    var bio by remember { mutableStateOf(profile.bio) }
    var photo by remember { mutableStateOf<Uri?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> if (uri != null) photo = uri }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = { Text("프로필 수정", fontWeight = FontWeight.Bold, color = Ink) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(Modifier.bouncyClick(CircleShape) { picker.launch("image/*") }) {
                    ProfilePhoto(profile.photoData, photo, 84.dp)
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .size(28.dp)
                            .background(Color.White, CircleShape)
                            .border(Hairline, Line, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { MBIcon(R.drawable.ic_mb_camera, size = 16.dp, tint = Ink, contentDescription = "사진 바꾸기") }
                }
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("이름") }, singleLine = true,
                    shape = RoundedCornerShape(12.dp), colors = mbFieldColors(), modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = handle, onValueChange = { handle = it },
                    label = { Text("아이디") }, prefix = { Text("@", color = Muted) }, singleLine = true,
                    shape = RoundedCornerShape(12.dp), colors = mbFieldColors(), modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = bio, onValueChange = { if (it.length <= 40) bio = it },
                    label = { Text("한 줄 소개 (40자)") }, singleLine = true,
                    shape = RoundedCornerShape(12.dp), colors = mbFieldColors(), modifier = Modifier.fillMaxWidth(),
                )
                AnimatedVisibility(visible = error != null) {
                    Text(
                        error.orEmpty(), fontSize = 13.sp, color = Danger,
                        modifier = Modifier.fillMaxWidth().background(DangerSoft, RoundedCornerShape(8.dp)).padding(10.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy,
                onClick = {
                    busy = true
                    error = null
                    onSave(name, handle, bio, photo) { problem ->
                        busy = false
                        error = problem
                    }
                },
            ) { Text(if (busy) "바꾸는 중…" else "완료", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("취소", color = Muted) } },
    )
}

private fun appVersion(context: android.content.Context): String =
    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()?.let { "v$it" } ?: ""
