package com.example.mbtrip

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.example.mbtrip.auth.*
import com.example.mbtrip.travel.*
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.time.LocalDate
import java.util.UUID

class SessionLiveInstrumentedTest {
    @Test fun accountSwitchRestoresOnlyItsOwnTripAndDiagnosis() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("mbtripLiveSession") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val run = UUID.randomUUID().toString()
        val app = FirebaseApp.initializeApp(context, FirebaseApp.getInstance().options, "session-check-$run")
        val auth = FirebaseAuth.getInstance(app)
        val db = FirebaseFirestore.getInstance(app)
        db.firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build()
        val accounts = FirebaseAccountService(auth, db)
        val session = FirebaseSessionAccess(auth)
        val prefs = "session_check_$run"
        val repo = TravelRepository(context, db, auth, prefs)
        val emails = (0..1).map { "mbtrip-session-$run-$it@example.com" }
        val password = UUID.randomUUID().toString() + "Aa9!"
        val uids = mutableMapOf<Int, String>()
        val trips = mutableMapOf<Int, ActiveTrip>()
        val cleanup = mutableListOf<String>()
        var stage = "anonymous_guard"
        try {
            withTimeout(90000) {
                val anonymous = auth.signInAnonymously().await().user!!.uid
                assertTrue(runCatching { session.signOut() }.isFailure)
                assertEquals(anonymous, auth.currentUser!!.uid)
                for (index in 0..1) {
                    stage = "create_account_$index"
                    val uid = accounts.register(emails[index], password).also { uids[index] = it }
                    if (index == 0) assertEquals(anonymous, uid)
                    accounts.completeProfile(uid, "계정 $index")
                    if (index == 1) { assertNull(repo.restore()); assertNull(repo.cachedScore()) }
                    repo.saveDiagnosis(if (index == 0) 21 else 79, null)
                    val date = LocalDate.now()
                    trips[index] = repo.create(TripInput("세션 검증 $run-$index", "시흥", date, date, 1))
                    if (index == 0) {
                        val trip = trips.getValue(0)
                        context.getSharedPreferences("${prefs}_legacy", 0).edit()
                            .putString("uid", uid).putString("tripId", trip.id).putString("ownerId", uid)
                            .putString("departure", trip.input.departure).putString("destination", trip.input.destination)
                            .putString("start", date.toString()).putString("end", date.toString()).putInt("count", 1).commit()
                        assertEquals(trip, TravelRepository(context, db, auth, "${prefs}_legacy").restore())
                    } else assertNull(TravelRepository(context, db, auth, "${prefs}_legacy").restore())
                    session.signOut()
                    assertNull(session.current()); assertNull(repo.restore()); assertNull(repo.cachedScore())
                }
                for (index in listOf(0, 1, 0)) {
                    stage = "restore_account_$index"
                    assertEquals(uids[index], accounts.signIn(emails[index], password))
                    assertEquals(uids[index], session.current()!!.uid)
                    val reopenedRepository = TravelRepository(context, db, auth, prefs)
                    assertEquals(trips[index], reopenedRepository.restore())
                    assertEquals(if (index == 0) 21 else 79, reopenedRepository.cachedScore())
                    assertEquals(trips[index], reopenedRepository.openTrip(trips[index]!!.id))
                    val other = trips[1 - index]!!
                    assertTrue(runCatching { reopenedRepository.openTrip(other.id) }.isFailure)
                    assertEquals(trips[index], reopenedRepository.restore())
                    session.signOut()
                }
                stage = "passed"
            }
        } finally {
            suspend fun clean(label: String, action: suspend () -> Unit) {
                try { withTimeout(15000) { action() } } catch (e: Exception) { cleanup += "$label:${e.javaClass.simpleName}" }
            }
            for ((index, uid) in uids) clean("account_$index") {
                accounts.signIn(emails[index], password)
                trips[index]?.let { trip ->
                    val ref = db.collection("trips").document(trip.id)
                    check(ref.get(Source.SERVER).await().getString("departure") == "세션 검증 $run-$index")
                    ref.collection("members").document(uid).delete().await(); ref.delete().await()
                }
                db.collection("users").document(uid).delete().await()
                auth.currentUser!!.delete().await()
                context.deleteSharedPreferences("${prefs}_$uid")
            }
            // If anonymous setup failed before registration, it still belongs only to this test.
            auth.currentUser?.let { clean("remaining_auth") { it.delete().await() } }
            auth.signOut(); context.deleteSharedPreferences(prefs); context.deleteSharedPreferences("${prefs}_legacy")
            clean("connection") { db.terminate().await() }; app.delete()
            Log.i("MBTripSessionCheck", "RESULT stage=$stage cleanupErrors=$cleanup")
            assertTrue("Cleanup failed: $cleanup", cleanup.isEmpty())
        }
    }
}
