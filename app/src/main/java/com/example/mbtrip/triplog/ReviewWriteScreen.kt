package com.example.mbtrip.triplog

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.mbtrip.R
import com.example.mbtrip.session.MyTrip
import com.example.mbtrip.ui.design.Hairline
import com.example.mbtrip.ui.design.MBButton
import com.example.mbtrip.ui.design.MBButtonStyle
import com.example.mbtrip.ui.design.MBCard
import com.example.mbtrip.ui.design.MBIcon
import com.example.mbtrip.ui.design.ScreenHeader
import com.example.mbtrip.ui.design.SectionLabel
import com.example.mbtrip.ui.design.bouncyClick
import com.example.mbtrip.ui.design.enterFromBelow
import com.example.mbtrip.ui.design.mbFieldColors
import com.example.mbtrip.ui.theme.Aqua700
import com.example.mbtrip.ui.theme.Background
import com.example.mbtrip.ui.theme.Faint
import com.example.mbtrip.ui.theme.Ink
import com.example.mbtrip.ui.theme.InkSoft
import com.example.mbtrip.ui.theme.Line
import com.example.mbtrip.ui.theme.Muted
import com.example.mbtrip.ui.theme.Sunny
import kotlinx.coroutines.launch

/**
 * 후기 쓰기 — 여행이 끝나면 오는 알림을 누르면 열린다.
 *  만족도(별 1~5) · 제목 · 내용 · 사진(최대 4장) → [후기 남기기]
 *  저장: trips/{tripId}/reviews/{내 uid} → 여행 로그 · 마이페이지 달력에 바로 보인다
 */
@Composable
fun ReviewWriteScreen(
    trip: MyTrip?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    /** 고칠 후기 (상세 화면의 수정 아이콘). null = 새로 쓰기 */
    editing: TripLog? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // 수정이면 원래 내용으로 채워 둔다
    var satisfaction by remember { mutableIntStateOf(editing?.satisfaction ?: 0) }
    var title by remember { mutableStateOf(editing?.title.orEmpty()) }
    var content by remember { mutableStateOf(editing?.content.orEmpty()) }
    // 사진: 새로 고른 것(Uri) · 이미 올린 것(주소 String)
    var photos by remember { mutableStateOf<List<Any>>(editing?.photos.orEmpty().take(ReviewReminders.MAX_PHOTOS)) }
    var saving by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(ReviewReminders.MAX_PHOTOS),
    ) { uris -> if (uris.isNotEmpty()) photos = (photos + uris).distinct().take(ReviewReminders.MAX_PHOTOS) }

    // 와이어프레임 그대로: 가운데 제목 · 별 5개 · 사진 3칸 · 제목 칸 · 내용 칸 · [제출하기]
    val fieldShape = RoundedCornerShape(12.dp)
    val fieldBg = Color(0xFFF4F4F4)
    val fieldLine = Color(0xFFBDBDBD)
    Column(Modifier.fillMaxSize().background(Color.White)) {
        // 뒤로 가기만 (제목 줄 없이)
        Box(Modifier.fillMaxWidth().padding(start = 8.dp, top = 6.dp)) {
            Box(
                Modifier.size(44.dp).bouncyClick(CircleShape, onClick = onBack),
                contentAlignment = Alignment.Center,
            ) { MBIcon(R.drawable.ic_mb_chevron_left, size = 24.dp, tint = Ink, contentDescription = "뒤로 가기") }
        }
        if (trip == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("후기를 쓸 여행을 찾지 못했어요.", fontSize = 14.sp, color = Muted)
            }
            return@Column
        }
        // 화면 가운데에 오도록 (키보드가 올라오면 스크롤)
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().imePadding()) {
        val screenH = maxHeight
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = screenH)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.height(24.dp))
            // 어느 여행인지 작게
            val s = trip.startDate
            Text(
                listOfNotNull(
                    trip.destination.ifBlank { null }?.let { "$it 여행" },
                    s?.let { formatDateRange(it, trip.endDate ?: it) },
                ).joinToString(" · "),
                fontSize = 13.sp, color = Muted,
                modifier = Modifier.enterFromBelow(0),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (editing != null) "후기를 고쳐 볼까요?" else "여행은 어떠셨나요?",
                fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink,
                modifier = Modifier.enterFromBelow(0),
            )
            Spacer(Modifier.height(14.dp))

            // 별 5개 — 누른 곳까지 검게 채워진다
            Row(Modifier.enterFromBelow(1), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                (1..5).forEach { n ->
                    Text(
                        if (n <= satisfaction) "★" else "☆",
                        fontSize = 40.sp,
                        color = Ink,
                        modifier = Modifier
                            .bouncyClick(CircleShape, scaleDown = 0.85f) { satisfaction = n }
                            .padding(horizontal = 2.dp),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))

            // 사진 3칸 — 빈 첫 칸은 +, 고른 사진은 누르면 빠진다
            Row(Modifier.fillMaxWidth().enterFromBelow(2), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(ReviewReminders.MAX_PHOTOS) { i ->
                    val uri = photos.getOrNull(i)
                    val isAddSlot = uri == null && i == photos.size
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(fieldShape)
                            .background(fieldBg)
                            .border(Hairline, fieldLine, fieldShape)
                            .then(
                                when {
                                    uri != null -> Modifier.clickable { photos = photos - uri }
                                    isAddSlot -> Modifier.clickable {
                                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    }
                                    else -> Modifier
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (uri != null) {
                            AsyncImage(
                                model = if (uri is String) photoModel(context, uri) else uri,
                                contentDescription = "고른 사진 (누르면 빠져요)",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                            Box(
                                Modifier.align(Alignment.TopEnd).padding(4.dp).size(20.dp)
                                    .background(Color.Black.copy(alpha = 0.55f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) { MBIcon(R.drawable.ic_mb_close, size = 12.dp, tint = Color.White) }
                        } else if (isAddSlot) {
                            Text("+", fontSize = 24.sp, color = Muted)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // 제목
            BasicField(
                value = title,
                onChange = { title = it.take(40) },
                placeholder = "제목을 입력해주세요.",
                singleLine = true,
                modifier = Modifier.fillMaxWidth().enterFromBelow(3),
                shape = fieldShape, bg = fieldBg, line = fieldLine,
            )
            Spacer(Modifier.height(8.dp))
            // 내용
            BasicField(
                value = content,
                onChange = { content = it.take(1000) },
                placeholder = "가장 기억에 남는 순간을 적어 주세요.",
                singleLine = false,
                modifier = Modifier.fillMaxWidth().height(140.dp).enterFromBelow(4),
                shape = fieldShape, bg = fieldBg, line = fieldLine,
            )
            Spacer(Modifier.height(20.dp))

            // 제출하기 — 검은 둥근 버튼
            val canSend = !saving && satisfaction > 0 && content.isNotBlank()
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .enterFromBelow(5)
                    .bouncyClick(RoundedCornerShape(50), enabled = canSend) {
                        scope.launch {
                            saving = true
                            val problem = ReviewReminders.save(context, trip, satisfaction, title, content, photos, editing)
                            saving = false
                            if (problem != null) {
                                Toast.makeText(context, problem, Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    if (editing != null) "후기를 고쳤어요." else "후기를 남겼어요. 여행 로그에서 볼 수 있어요.",
                                    Toast.LENGTH_SHORT,
                                ).show()
                                onSaved()
                            }
                        }
                    }
                    .background(if (canSend || saving) Ink else Color(0xFF9E9E9E), RoundedCornerShape(50)),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (saving) "저장하는 중…" else if (editing != null) "수정 완료" else "제출하기", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.White)
            }
            if (!canSend && !saving) {
                Spacer(Modifier.height(8.dp))
                Text(
                    if (satisfaction == 0) "별을 눌러 만족도를 골라 주세요." else "내용을 적어 주세요.",
                    fontSize = 12.sp, color = Muted,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
        }
    }
}

/** 회색 바탕 + 얇은 테두리 입력칸 (와이어프레임 모양) */
@Composable
private fun BasicField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean,
    modifier: Modifier,
    shape: RoundedCornerShape,
    bg: Color,
    line: Color,
) {
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = singleLine,
        textStyle = TextStyle(fontFamily = com.example.mbtrip.ui.theme.Pretendard, fontSize = 14.sp, color = Ink),
        cursorBrush = SolidColor(Ink),
        modifier = modifier
            .clip(shape)
            .background(bg)
            .border(Hairline, line, shape),
        decorationBox = { inner ->
            Box(
                Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
            ) {
                if (value.isEmpty()) Text(placeholder, fontSize = 14.sp, color = Color(0xFF9E9E9E))
                inner()
            }
        },
    )
}
