package com.example.mbtrip

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.example.mbtrip.travel.*
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.time.LocalDate
import java.util.Date
import java.util.UUID

class InviteLiveInstrumentedTest {
    @Test fun singleUseInviteEnforcesMembershipAndRevocation() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("mbtripLiveInvites") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val run = UUID.randomUUID().toString()
        val apps = (0..2).map { FirebaseApp.initializeApp(context, FirebaseApp.getInstance().options, "invite-$run-$it") }
        val auths = apps.map { FirebaseAuth.getInstance(it) }
        val dbs = apps.map { FirebaseFirestore.getInstance(it).apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build()
        } }
        val repos = (0..2).map { TravelRepository(context, dbs[it], auths[it], "invite_$run-$it") }
        val invites = (0..2).map { InviteRepository(dbs[it], auths[it]) }
        var active: ActiveTrip? = null
        var stage = "initialize"
        val tokens = mutableListOf<String>()
        val cleanup = mutableListOf<String>()
        suspend fun denied(action: suspend () -> Unit) {
            val error = runCatching { action() }.exceptionOrNull()
            assertTrue("Expected PERMISSION_DENIED at $stage, got ${error?.javaClass?.simpleName}",
                error is FirebaseFirestoreException && error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED)
        }
        try {
            withTimeout(120000) {
                val uids = auths.map { it.signInAnonymously().await().user!!.uid }
                repos[1].saveDiagnosis(65, null)
                dbs[1].collection("users").document(uids[1]).set(mapOf("nickname" to "초대 손님"), SetOptions.merge()).await()
                val date = LocalDate.now()
                val trip = repos[0].create(TripInput("초대 검증 $run", "시흥", date, date, 3)).also { active = it }
                val tripRef = dbs[0].collection("trips").document(trip.id)
                suspend fun issue(): String = invites[0].issue(trip).also { tokens += it }
                suspend fun rawClaim(index: Int, code: String) {
                    val db = dbs[index]
                    db.batch().apply {
                        update(db.collection("tripInvites").document(code), mapOf("usedBy" to uids[index], "usedAt" to FieldValue.serverTimestamp()))
                        set(db.collection("trips").document(trip.id).collection("members").document(uids[index]), mapOf(
                            "userId" to uids[index], "tripId" to trip.id, "nickname" to "검증", "handle" to "@test", "inviteCode" to code))
                    }.commit().await()
                }
                stage = "deny_unauthorized_registration"
                denied { dbs[1].collection("trips").document(trip.id).collection("members").document(uids[1])
                    .set(mapOf("userId" to uids[1], "tripId" to trip.id)).await() }
                stage = "issue_and_prevent_directory_and_partial_claim"
                val first = issue()
                denied { dbs[1].collection("tripInvites").get(Source.SERVER).await() }
                denied { invites[1].issue(trip) }
                assertTrue(runCatching { invites[1].join("F".repeat(32)) }.isFailure)
                denied { dbs[1].collection("tripInvites").document(first).update(mapOf("usedBy" to uids[1], "usedAt" to FieldValue.serverTimestamp())).await() }
                stage = "join_and_restore_profile"
                assertEquals(trip.id, invites[1].join(first))
                val opened = repos[1].openTrip(trip.id)
                assertEquals(trip.id, opened.id)
                val member = tripRef.collection("members").document(uids[1]).get(Source.SERVER).await()
                assertEquals(65L, member.getLong("jpScore")); assertEquals("초대 손님", member.getString("nickname"))
                assertEquals(trip.id, invites[1].join(first))
                assertTrue(runCatching { invites[2].join(first) }.isFailure)
                stage = "removed_user_cannot_replay"
                repos[0].removeMember(trip, uids[1])
                denied { rawClaim(1, first) }
                assertTrue(runCatching { invites[1].join(first) }.isFailure)
                stage = "rotation_and_revocation"
                val old = issue(); val newer = issue()
                assertTrue(runCatching { invites[1].join(old) }.isFailure)
                invites[0].revoke(trip)
                assertTrue(runCatching { invites[1].join(newer) }.isFailure)
                stage = "expiry_enforced_by_server"
                val expiring = InviteCode.generate().also { tokens += it }
                dbs[0].batch().apply {
                    set(dbs[0].collection("tripInvites").document(expiring), mapOf("tripId" to trip.id,
                        "ownerId" to uids[0], "expiresAt" to Timestamp(Date(System.currentTimeMillis() + 5000)), "usedBy" to ""))
                    update(tripRef, "inviteCode", expiring)
                }.commit().await()
                delay(5500)
                denied { rawClaim(1, expiring) }
                stage = "concurrent_claim_exactly_one_winner"
                val racing = issue()
                val results = coroutineScope { listOf(1, 2).map { index -> async { runCatching { invites[index].join(racing) } } }.awaitAll() }
                assertEquals(1, results.count { it.isSuccess })
                assertEquals(2, tripRef.collection("members").get(Source.SERVER).await().size())
                stage = "passed"
            }
        } finally {
            suspend fun clean(label: String, action: suspend () -> Unit) {
                try { withTimeout(12000) { action() } } catch (e: Exception) { cleanup += "$label:${e.javaClass.simpleName}" }
            }
            for (code in tokens) clean("token") {
                val ref = dbs[0].collection("tripInvites").document(code)
                if (ref.get(Source.SERVER).await().exists()) ref.delete().await()
            }
            active?.let { trip -> clean("trip") {
                val ref = dbs[0].collection("trips").document(trip.id)
                check(ref.get(Source.SERVER).await().getString("departure") == "초대 검증 $run")
                val members = ref.collection("members").get(Source.SERVER).await()
                dbs[0].batch().apply { members.documents.forEach { delete(it.reference) }; delete(ref) }.commit().await()
            } }
            for (index in 0..2) {
                auths[index].currentUser?.let { user ->
                    clean("profile") { dbs[index].collection("users").document(user.uid).delete().await() }
                    context.deleteSharedPreferences("invite_$run-${index}_${user.uid}")
                    clean("account") { user.delete().await() }
                }
                auths[index].signOut()
                context.getSharedPreferences("invite_$run-$index", 0).edit().clear().commit()
                clean("connection") { dbs[index].terminate().await() }; apps[index].delete()
            }
            Log.i("MBTripInviteCheck", "RESULT stage=$stage cleanupErrors=$cleanup")
            assertTrue("Cleanup failed: $cleanup", cleanup.isEmpty())
        }
    }
}
