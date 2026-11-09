package com.example.myapplication

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. 뒤로 가기 버튼 기능
        val ivBack = findViewById<TextView>(R.id.iv_back)
        ivBack.setOnClickListener {
            finish()
        }

        // 2. 사진 슬라이더 (ViewPager2) 설정
        val viewPager = findViewById<ViewPager2>(R.id.view_pager_photos)
        val imageList = listOf(
            R.drawable.travel_main_photo,
            R.drawable.user1,
            R.drawable.user2,
            R.drawable.user3
        )
        viewPager.adapter = PhotoPagerAdapter(imageList)

        // 3. 하단 프로필 사진 클릭 시 정보 모달 띄우기
        findViewById<androidx.cardview.widget.CardView>(R.id.cv_profile1).setOnClickListener {
            showProfileDialog("사용자 1", "여행 기획 및 일정 조율 담당")
        }

        findViewById<androidx.cardview.widget.CardView>(R.id.cv_profile2).setOnClickListener {
            showProfileDialog("사용자 2", "사진 촬영 및 기록 담당")
        }

        findViewById<androidx.cardview.widget.CardView>(R.id.cv_profile3).setOnClickListener {
            showProfileDialog("사용자 3", "길찾기 및 네비게이션 담당")
        }
    }

    private fun showProfileDialog(name: String, role: String) {
        AlertDialog.Builder(this)
            .setTitle("$name 프로필 정보")
            .setMessage("이름: $name\n역할: $role")
            .setPositiveButton("확인") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}