package com.example.mbtrip.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.mbtrip.ui.component.MBTripTopBar
// 정의하신 테마 컬러 팔레트 Import
import com.example.mbtrip.ui.theme.Background
import com.example.mbtrip.ui.theme.Primary
import com.example.mbtrip.ui.theme.Secondary
import com.example.mbtrip.ui.theme.Surface
import com.example.mbtrip.ui.theme.TextPrimary
import com.example.mbtrip.ui.theme.TextSecondary
import java.util.Calendar

@Composable
fun MyPageScreen(
    onBackClick: () -> Unit = {},
    onReTestClick: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    // 프로필 편집 상태 관리 (이모지 대신 이미지 Uri 지원)
    var name by remember { mutableStateOf("누군기") }
    var userId by remember { mutableStateOf("@jithdms") }
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var isEditingProfile by remember { mutableStateOf(false) }

    // 프로필 수정 팝업 임시 상태
    var tempName by remember { mutableStateOf(name) }
    var tempUserId by remember { mutableStateOf(userId) }
    var tempImageUri by remember { mutableStateOf<Uri?>(profileImageUri) }

    // 갤러리에서 이미지 선택 런처
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            tempImageUri = uri
        }
    }

    // 달력 연/월 상태 관리
    var currentYear by remember { mutableStateOf(2026) }
    var currentMonth by remember { mutableStateOf(10) } // 0~11 (10 = 11월)

    val calendar = remember(currentYear, currentMonth) {
        Calendar.getInstance().apply {
            set(currentYear, currentMonth, 1)
        }
    }
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val maxDaysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    Scaffold(
        containerColor = Background,
        topBar = {
            MBTripTopBar(
                title = "마이페이지",
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. 프로필 영역
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Primary.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (profileImageUri != null) {
                            AsyncImage(
                                model = profileImageUri,
                                contentDescription = "프로필 사진",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(text = "🐶", fontSize = 32.sp)
                        }
                    }

                    Column {
                        Text(
                            text = name,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = userId,
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }
                }

                IconButton(onClick = {
                    tempName = name
                    tempUserId = userId
                    tempImageUri = profileImageUri
                    isEditingProfile = true
                }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "프로필 수정",
                        tint = TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // 2. MBTI "P" 85% 카드 영역
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "“P”",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                        Text(
                            text = "85%",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "J", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                            Text(text = "P", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(TextSecondary.copy(alpha = 0.4f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(0.15f)
                                    .background(TextPrimary)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(0.85f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = onReTestClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Secondary,
                            contentColor = Surface
                        )
                    ) {
                        Text(
                            text = "재진단하기",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 3. 전체 월 달력 일정 카드 영역
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "달력 일정",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (currentMonth == 0) {
                                        currentMonth = 11
                                        currentYear -= 1
                                    } else {
                                        currentMonth -= 1
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowLeft,
                                    contentDescription = "이전 달",
                                    tint = TextPrimary
                                )
                            }

                            Text(
                                text = "$currentYear. ${String.format("%02d", currentMonth + 1)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            IconButton(
                                onClick = {
                                    if (currentMonth == 11) {
                                        currentMonth = 0
                                        currentYear += 1
                                    } else {
                                        currentMonth += 1
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowRight,
                                    contentDescription = "다음 달",
                                    tint = TextPrimary
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Background, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                            listOf("일", "월", "화", "수", "목", "금", "토").forEach { day ->
                                Text(
                                    text = day,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    modifier = Modifier.width(32.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        val totalCells = mutableListOf<Int?>()
                        for (i in 1 until firstDayOfWeek) {
                            totalCells.add(null)
                        }
                        for (day in 1..maxDaysInMonth) {
                            totalCells.add(day)
                        }

                        totalCells.chunked(7).forEach { weekDays ->
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    for (i in 0 until 7) {
                                        val day = if (i < weekDays.size) weekDays[i] else null
                                        Box(
                                            modifier = Modifier.size(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (day != null) {
                                                Text(
                                                    text = "$day",
                                                    fontSize = 12.sp,
                                                    color = TextPrimary,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }

                                if (currentYear == 2026 && currentMonth == 10) {
                                    val hasTripThisWeek = weekDays.contains(3) && weekDays.contains(5)
                                    if (hasTripThisWeek) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceAround
                                        ) {
                                            val startIdx = weekDays.indexOf(3)
                                            val endIdx = weekDays.indexOf(5)
                                            if (startIdx >= 0 && endIdx >= 0) {
                                                Spacer(modifier = Modifier.weight(startIdx.toFloat().coerceAtLeast(0.1f)))
                                                Box(
                                                    modifier = Modifier
                                                        .weight((endIdx - startIdx + 1).toFloat())
                                                        .height(32.dp)
                                                        .clip(RoundedCornerShape(16.dp))
                                                        .background(Primary),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "제주도 여행",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Surface
                                                    )
                                                }
                                                Spacer(modifier = Modifier.weight((6 - endIdx).toFloat().coerceAtLeast(0.1f)))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // 프로필 편집 다이얼로그 (사진 변경 기능 포함)
    if (isEditingProfile) {
        AlertDialog(
            onDismissRequest = { isEditingProfile = false },
            title = { Text(text = "프로필 수정", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 팝업 내부에서 보여지는 프로필 사진 미리보기 및 변경 버튼
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Primary.copy(alpha = 0.3f))
                            .clickable {
                                imagePickerLauncher.launch("image/*")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (tempImageUri != null) {
                            AsyncImage(
                                model = tempImageUri,
                                contentDescription = "프로필 사진 미리보기",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(text = "📷", fontSize = 28.sp)
                        }
                    }

                    TextButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Text("갤러리에서 사진 변경", fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("이름") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tempUserId,
                        onValueChange = { tempUserId = it },
                        label = { Text("아이디 (중복 방지)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (tempName.isNotBlank() && tempUserId.isNotBlank()) {
                            name = tempName
                            userId = if (tempUserId.startsWith("@")) tempUserId else "@$tempUserId"
                            profileImageUri = tempImageUri
                            isEditingProfile = false
                        }
                    }
                ) {
                    Text("저장", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isEditingProfile = false }) {
                    Text("취소")
                }
            }
        )
    }
}