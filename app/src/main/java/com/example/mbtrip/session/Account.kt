package com.example.mbtrip.session

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/** 내 프로필 (Firestore users/{uid}) */
data class MyProfile(
    val uid: String,
    val email: String,
    val nickname: String,
    /** "@아이디" */
    val handle: String,
    val bio: String = "",
    /** 진단 점수 (0 = J, 100 = P). 아직 안 했으면 null */
    val jpScore: Int? = null,
    /** 프로필 사진 (JPEG, Base64) — 저장소 설정 없이 문서 안에 작게 넣는다 */
    val photoData: String? = null,
    /** 테스트 자료를 볼 수 있는 계정인지 (TestAccess 참고) */
    val tester: Boolean = false,
)

/**
 * 회원가입 · 로그인 · 로그아웃 · 회원 탈퇴 · 프로필 저장.
 *
 * Firebase Authentication 의 이메일/비밀번호 로그인을 쓴다.
 *  ⚠ Firebase Console → Authentication → 로그인 방법 → '이메일/비밀번호' 를 켜야 한다.
 *
 * 사용자 문서: users/{uid}
 *   { userId, email, nickname, nicknameLower, handle("@아이디"), handleLower, bio, jpScore, photoData, createdAt, updatedAt }
 *   (handleLower · nicknameLower 는 친구 검색용 — MemberRepository 와 같은 규칙)
 *
 * 로그인 전에 이미 익명으로 여행을 만들었다면, 가입할 때 그 익명 계정에 이메일을 연결해서
 * 만들어 둔 여행·진단 점수가 그대로 이어진다.
 */
object Account {
    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()
    private fun userDoc(uid: String) = db.collection("users").document(uid)

    /** 화면에 바로 반영되도록 Compose 상태 */
    var profile: MyProfile? by mutableStateOf(null); private set

    /** 이메일로 로그인한 상태인지 (익명은 아님) */
    val isSignedIn: Boolean get() = auth.currentUser?.let { !it.isAnonymous } ?: false

    /* ───────────── 가입 · 로그인 ───────────── */

    /** 회원가입. 문제가 있으면 안내 문구, 성공하면 null */
    suspend fun signUp(context: Context, email: String, handle: String, nickname: String, password: String): String? {
        val e = email.trim()
        val h = normalizeHandle(handle)
        val n = nickname.trim().ifBlank { h }
        if (!e.contains('@') || !e.contains('.')) return "이메일 형식을 확인해 주세요."
        if (!HANDLE_REGEX.matches(h)) return "아이디는 영어 소문자·숫자·_ 로 3~20자예요."
        if (password.length < 6) return "비밀번호는 6자 이상이에요."
        return runCatching {
            ensureSession()
            if (isHandleTaken(h, exceptUid = null)) return "이미 사용 중인 아이디예요."
            val current = auth.currentUser
            val created = if (current != null && current.isAnonymous) {
                // 익명으로 만든 여행·진단을 그대로 이어가기
                current.linkWithCredential(EmailAuthProvider.getCredential(e, password)).await().user
            } else {
                auth.createUserWithEmailAndPassword(e, password).await().user
            }
            val user = created ?: return "가입하지 못했어요. 잠시 후 다시 시도해 주세요."
            val jp = UserProfile.jpScore(context)
            userDoc(user.uid).set(
                buildMap {
                    put("userId", user.uid)
                    put("email", e)
                    put("nickname", n)
                    put("nicknameLower", n.lowercase())
                    put("handle", "@$h")
                    put("handleLower", h)
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                    if (jp != null) put("jpScore", jp)
                },
                SetOptions.merge(),
            ).await()
            load(context)
            null
        }.getOrElse { authMessage(it) }
    }

    /** 로그인 — 이메일 또는 아이디. 문제가 있으면 안내 문구, 성공하면 null */
    suspend fun signIn(context: Context, idOrEmail: String, password: String): String? {
        val input = idOrEmail.trim()
        if (input.isBlank() || password.isBlank()) return "이메일(또는 아이디)과 비밀번호를 입력해 주세요."
        return runCatching {
            val email = if (input.contains('@') && input.contains('.')) {
                input
            } else {
                // 아이디로 이메일 찾기 (검색하려면 세션이 있어야 해서 익명으로 먼저)
                ensureSession()
                db.collection("users").whereEqualTo("handleLower", normalizeHandle(input)).limit(1).get().await()
                    .documents.firstOrNull()?.getString("email")
                    ?: return "'$input' 아이디를 찾지 못했어요."
            }
            auth.signInWithEmailAndPassword(email, password).await()
            load(context)
            MyTrips.syncFromServer()
            null
        }.getOrElse { authMessage(it) }
    }

    /** 앱을 켰을 때 / 화면에 들어올 때 프로필 불러오기 (로그인 상태일 때만) */
    suspend fun load(context: Context) {
        val user = auth.currentUser ?: return
        if (user.isAnonymous) return
        val doc = runCatching { userDoc(user.uid).get().await() }.getOrNull() ?: return
        val p = MyProfile(
            uid = user.uid,
            email = doc.getString("email") ?: user.email.orEmpty(),
            nickname = doc.getString("nickname") ?: "여행자",
            handle = doc.getString("handle") ?: "@${user.uid.take(6)}",
            bio = doc.getString("bio").orEmpty(),
            jpScore = doc.getLong("jpScore")?.toInt(),
            photoData = doc.getString("photoData"),
            tester = TestAccess.isTesterEmail(doc.getString("email") ?: user.email) || doc.getBoolean("tester") == true,
        )
        profile = p
        // 진단 결과: 서버 값이 있으면 이 폰에 맞춰 두고, 서버에 없고 이 폰에만 있으면 서버에 올린다
        val server = p.jpScore
        val local = UserProfile.jpScore(context)
        if (server != null) {
            UserProfile.rememberLocally(context, server)
        } else if (local != null) {
            runCatching { userDoc(user.uid).set(mapOf("jpScore" to local), SetOptions.merge()).await() }
            profile = p.copy(jpScore = local)
        }
        // 테스트 계정이면 예시 알림을, 아니면 빈 알림함을
        com.example.mbtrip.notifications.NotificationDependencies.useTestData(p.tester)
        runCatching { com.example.mbtrip.friends.Friends.refresh() }
    }

    /* ───────────── 프로필 수정 ───────────── */

    /**
     * 이름 · 아이디 · 한 줄 소개 · 사진 저장. photo 가 null 이면 사진은 그대로.
     * 문제가 있으면 안내 문구, 성공하면 null
     */
    suspend fun updateProfile(context: Context, nickname: String, handle: String, bio: String, photo: Uri?): String? {
        val user = auth.currentUser ?: return "로그인이 필요해요."
        val h = normalizeHandle(handle)
        val n = nickname.trim()
        if (n.isBlank()) return "이름을 입력해 주세요."
        if (!HANDLE_REGEX.matches(h)) return "아이디는 영어 소문자·숫자·_ 로 3~20자예요."
        return runCatching {
            if (isHandleTaken(h, exceptUid = user.uid)) return "이미 사용 중인 아이디예요."
            val photoData = photo?.let { encodePhoto(context, it) ?: return "사진을 읽지 못했어요." }
            userDoc(user.uid).set(
                buildMap {
                    put("userId", user.uid)
                    put("nickname", n)
                    put("nicknameLower", n.lowercase())
                    put("handle", "@$h")
                    put("handleLower", h)
                    put("bio", bio.trim())
                    if (photoData != null) put("photoData", photoData)
                    put("updatedAt", FieldValue.serverTimestamp())
                },
                SetOptions.merge(),
            ).await()
            load(context)
            null
        }.getOrElse { authMessage(it) }
    }

    /* ───────────── 로그아웃 · 탈퇴 ───────────── */

    /**
     * 로그아웃 — 이 폰의 내 여행 목록·지금 여행만 비운다.
     * 진단 결과는 계정(users/{uid}.jpScore)과 이 폰(계정별)에 남아서, 다시 로그인하면 그대로 보인다.
     */
    fun signOut(context: Context) {
        auth.signOut()
        clearLocal(context)
    }

    /**
     * 회원 탈퇴 — 내 사용자 문서 · 내 여행 목록 문서를 지우고 계정을 삭제한다.
     * (함께 만든 여행 방의 일정은 동행자들이 쓰고 있어서 남겨 둔다)
     * 문제가 있으면 안내 문구, 성공하면 null
     */
    suspend fun deleteAccount(context: Context, password: String): String? {
        val user = auth.currentUser ?: return "로그인이 필요해요."
        return runCatching {
            // 오래전에 로그인했으면 탈퇴 전에 비밀번호를 다시 확인해야 한다
            val email = user.email
            if (email != null && password.isNotBlank()) {
                user.reauthenticate(EmailAuthProvider.getCredential(email, password)).await()
            }
            val mine = userDoc(user.uid).collection("myTrips").get().await()
            mine.documents.forEach { it.reference.delete().await() }
            userDoc(user.uid).delete().await()
            user.delete().await()
            clearLocal(context)
            null
        }.getOrElse { authMessage(it) }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun clearLocal(context: Context) {
        profile = null
        MyTrips.clearLocal()
        TripSession.clear()
        com.example.mbtrip.notifications.NotificationDependencies.useTestData(false)
        com.example.mbtrip.friends.Friends.clearLocal()
    }

    /* ───────────── 도우미 ───────────── */

    /** 로그인하지 않았으면 익명 세션을 만든다 (검색·저장 권한용) */
    private suspend fun ensureSession() {
        if (auth.currentUser == null) auth.signInAnonymously().await()
    }

    private suspend fun isHandleTaken(handleLower: String, exceptUid: String?): Boolean {
        val docs = db.collection("users").whereEqualTo("handleLower", handleLower).limit(2).get().await().documents
        return docs.any { it.id != exceptUid }
    }

    /** "@Min_Ji " → "min_ji" */
    fun normalizeHandle(h: String): String = h.trim().removePrefix("@").lowercase()

    private val HANDLE_REGEX = Regex("^[a-z0-9_]{3,20}$")

    /** 사진을 가로세로 최대 256px JPEG 로 줄여 Base64 로 (문서 1MB 제한 안에 넉넉히) */
    private suspend fun encodePhoto(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= 256 && bounds.outHeight / (sample * 2) >= 256) sample *= 2
            val bmp = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: return@runCatching null
            val scale = 256f / maxOf(bmp.width, bmp.height)
            val small = if (scale < 1f) Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true) else bmp
            val out = ByteArrayOutputStream()
            small.compress(Bitmap.CompressFormat.JPEG, 82, out)
            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        }.getOrNull()
    }

    /** Base64 사진 → Bitmap (화면에 그릴 때) */
    fun decodePhoto(data: String?): Bitmap? = data?.let {
        runCatching {
            val bytes = Base64.decode(it, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    /** Firebase 오류 → 알아듣기 쉬운 말 */
    private fun authMessage(e: Throwable): String = when (e) {
        is FirebaseAuthUserCollisionException -> "이미 가입된 이메일이에요."
        is FirebaseAuthWeakPasswordException -> "비밀번호가 너무 쉬워요. 6자 이상으로 정해 주세요."
        is FirebaseAuthInvalidUserException -> "가입되지 않은 계정이에요."
        is FirebaseAuthInvalidCredentialsException -> "이메일 또는 비밀번호가 맞지 않아요."
        is FirebaseAuthRecentLoginRequiredException -> "보안을 위해 비밀번호를 다시 입력해 주세요."
        else -> "잠시 후 다시 시도해 주세요. (${e.localizedMessage ?: "연결 오류"})"
    }
}
