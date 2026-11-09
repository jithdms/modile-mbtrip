package com.example.myapplication

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // 시스템 바(상단/하단) 여백 설정 (activity_main.xml의 id @+id/main과 연동)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 1. 왼쪽 상단 뒤로 가기 버튼 기능 구현
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish() // 현재 액티비티를 종료하고 이전 화면으로 이동
        }

        // 2. 검색창 오른쪽 달력 아이콘 클릭 시 기간 선택 다이얼로그 띄우기
        val ivCalendar = findViewById<ImageView>(R.id.ivCalendar)
        ivCalendar.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            // DatePickerDialog 팝업 생성
            val datePickerDialog = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    // 사용자가 날짜를 선택하고 확인을 눌렀을 때 실행되는 곳
                    val selectedDate = "$selectedYear-${selectedMonth + 1}-$selectedDay"
                    // TODO: 선택된 날짜(selectedDate)를 이용해 로그를 필터링하는 로직을 추후 여기에 추가하세요.
                },
                year,
                month,
                day
            )
            datePickerDialog.show()
        }
    }
}