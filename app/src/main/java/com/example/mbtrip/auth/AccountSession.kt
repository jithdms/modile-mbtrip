package com.example.mbtrip.auth

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

data class AccountSession(val uid: String, val anonymous: Boolean, val label: String)

interface SessionAccess {
    fun current(): AccountSession?
    fun observe(): Flow<AccountSession?>
    fun signOut()
}

class FirebaseSessionAccess(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) : SessionAccess {
    override fun current(): AccountSession? = auth.currentUser?.let {
        AccountSession(it.uid, it.isAnonymous, it.email ?: it.displayName ?: "익명 계정")
    }
    override fun observe(): Flow<AccountSession?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(current()) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()
    override fun signOut() {
        // Anonymous accounts must be linked before sign-out to avoid orphaning their trips.
        check(auth.currentUser?.isAnonymous != true) { "먼저 이메일 계정을 연결해주세요." }
        auth.signOut()
    }
}
