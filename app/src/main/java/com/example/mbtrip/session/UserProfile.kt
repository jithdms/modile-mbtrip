package com.example.mbtrip.session

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * 나(이 폰의 사용자) 정보 — 처음 J/P 진단을 했는지, 점수가 몇인지.
 *  - 폰 안(SharedPreferences): 다음에 앱을 켜도 진단을 다시 안 하게
 *  - Firestore users/{uid}: { userId, jpScore }  (데이터 구조 문서에 users 추가 필요)
 * jpScore 는 데이터 구조 문서 기준 0 = J, 100 = P.
 */
object UserProfile {
    private const val PREFS = "mbtrip_user"
    private const val KEY_JP = "jpScore"

    fun jpScore(context: Context): Int? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_JP, -1).takeIf { it in 0..100 }

    fun hasDiagnosis(context: Context): Boolean = jpScore(context) != null

    /** 진단 결과 저장. 폰에는 바로, Firestore 는 익명 로그인 뒤 */
    suspend fun saveJpScore(context: Context, jpScore: Int) {
        require(jpScore in 0..100)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY_JP, jpScore).apply()
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: auth.signInAnonymously().await().user?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .set(mapOf("userId" to uid, "jpScore" to jpScore), SetOptions.merge())
            .await()
    }

    /** 개발용: 진단을 처음부터 다시 하게 */
    fun resetDiagnosis(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_JP).apply()
    }
}
