package com.example.mbtrip.auth

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

interface AccountService {
    suspend fun signIn(email: String, password: String): String
    suspend fun register(email: String, password: String): String
    suspend fun completeProfile(uid: String, nickname: String?)
}

object AccountInput {
    fun validate(email: String, password: String, nickname: String? = null, confirmation: String? = null): String? = when {
        !Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email.trim()) -> "올바른 이메일을 입력해주세요."
        password.isEmpty() -> "비밀번호를 입력해주세요."
        nickname != null && nickname.trim().length !in 1..20 -> "닉네임은 1~20자로 입력해주세요."
        nickname != null && password.length < 6 -> "비밀번호는 6자 이상 입력해주세요."
        confirmation != null && password != confirmation -> "비밀번호가 일치하지 않아요."
        else -> null
    }
}

class FirebaseAccountService(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : AccountService {
    override suspend fun signIn(email: String, password: String): String =
        auth.signInWithEmailAndPassword(email.trim(), password).await().user?.uid
            ?: error("사용자를 확인하지 못했어요.")

    override suspend fun register(email: String, password: String): String {
        val current = auth.currentUser
        check(current == null || current.isAnonymous) { "이미 로그인되어 있어요. 가입 전에 계정을 확인해주세요." }
        val credential = EmailAuthProvider.getCredential(email.trim(), password)
        // Linking keeps ownership, diagnosis and travel membership under the anonymous UID.
        val result = if (current != null) current.linkWithCredential(credential).await()
            else auth.createUserWithEmailAndPassword(email.trim(), password).await()
        return result.user?.uid ?: error("사용자를 확인하지 못했어요.")
    }

    override suspend fun completeProfile(uid: String, nickname: String?) {
        check(auth.currentUser?.uid == uid) { "계정이 변경되었어요. 다시 로그인해주세요." }
        val ref = db.collection("users").document(uid)
        val existing = ref.get(Source.SERVER).await()
        val name = nickname?.trim() ?: existing.getString("nickname") ?: "여행자"
        // Password/email stay in Firebase Authentication, never in readable user profiles.
        ref.set(mapOf("userId" to uid, "nickname" to name,
            "nicknameLower" to name.lowercase()), SetOptions.merge()).await()
    }
}

fun accountError(error: Exception): String = when ((error as? FirebaseAuthException)?.errorCode) {
    "ERROR_EMAIL_ALREADY_IN_USE", "ERROR_CREDENTIAL_ALREADY_IN_USE" -> "이미 가입된 이메일이에요. 로그인해주세요."
    "ERROR_OPERATION_NOT_ALLOWED" -> "현재 이메일 로그인이 활성화되지 않았어요. 관리자 설정을 확인해주세요."
    "ERROR_WEAK_PASSWORD" -> "더 안전한 비밀번호를 입력해주세요."
    "ERROR_TOO_MANY_REQUESTS" -> "요청이 많아요. 잠시 후 다시 시도해주세요."
    "ERROR_INVALID_EMAIL" -> "올바른 이메일을 입력해주세요."
    else -> "로그인 정보를 확인하고 다시 시도해주세요. 연결 상태도 확인해주세요."
}
