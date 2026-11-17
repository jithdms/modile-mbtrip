package com.example.mbtrip

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.example.mbtrip.auth.FirebaseAccountService
import com.example.mbtrip.travel.*
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Source
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.time.LocalDate
import java.util.UUID

/** Explicit opt-in. Secondary FirebaseApp only; disposable fixtures are cleaned in finally. */
class AccountLiveInstrumentedTest {
    @Test fun anonymousLinkAndSignInKeepTravelAndDiagnosis() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("mbtripLiveAccount") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val token = UUID.randomUUID().toString()
        val app = FirebaseApp.initializeApp(context, FirebaseApp.getInstance().options, "account-check-$token")
        val auth = FirebaseAuth.getInstance(app)
        val db = FirebaseFirestore.getInstance(app)
        db.firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build()
        val prefs = "account_check_$token"
        val repo = TravelRepository(context, db, auth, prefs)
        val service = FirebaseAccountService(auth, db)
        val email = "mbtrip-check-$token@example.com"
        val password = UUID.randomUUID().toString() + "aA9!"
        var uid: String? = null
        var trip: ActiveTrip? = null
        var stage = "anonymous"
        val cleanupErrors = mutableListOf<String>()
        try {
            withTimeout(90000) {
                uid = auth.signInAnonymously().await().user!!.uid
                repo.saveDiagnosis(27, null)
                val date = LocalDate.now()
                trip = repo.create(TripInput("계정 검증 $token", "시흥", date, date, 1))
                stage = "link"
                assertEquals(uid, service.register(email, password))
                assertFalse(auth.currentUser!!.isAnonymous)
                service.completeProfile(uid!!, "검증 여행자")
                stage = "sign_in"
                auth.signOut()
                assertNull(repo.restore())
                assertEquals(uid, service.signIn(email, password))
                service.completeProfile(uid!!, null)
                assertEquals(trip, repo.restore())
                val profile = db.collection("users").document(uid!!).get(Source.SERVER).await()
                assertEquals(27L, profile.getLong("jpScore"))
                assertEquals("검증 여행자", profile.getString("nickname"))
                assertNull(profile.get("password")); assertNull(profile.get("email"))
                val ref = db.collection("trips").document(trip!!.id)
                assertEquals(uid, ref.get(Source.SERVER).await().getString("ownerId"))
                assertEquals(27L, ref.collection("members").document(uid!!).get(Source.SERVER).await().getLong("jpScore"))
                stage = "bad_password"
                assertTrue(runCatching { service.signIn(email, "incorrect-password") }.isFailure)
                assertEquals(uid, auth.currentUser!!.uid)
                stage = "passed"
            }
        } finally {
            suspend fun clean(label: String, action: suspend () -> Unit) {
                try { withTimeout(15000) { action() } }
                catch (e: Exception) { cleanupErrors += "$label:${e.javaClass.simpleName}" }
            }
            if (auth.currentUser == null && uid != null) clean("relogin") { service.signIn(email, password) }
            trip?.let { active -> clean("trip") {
                val ref = db.collection("trips").document(active.id)
                check(ref.get(Source.SERVER).await().getString("departure") == "계정 검증 $token")
                ref.collection("members").document(uid!!).delete().await()
                ref.delete().await()
            } }
            uid?.let { clean("profile") { db.collection("users").document(it).delete().await() } }
            auth.currentUser?.let { user -> clean("auth") { user.delete().await() } }
            auth.signOut()
            uid?.let { context.deleteSharedPreferences("${prefs}_$it") }
            context.getSharedPreferences(prefs, 0).edit().clear().commit()
            clean("connection") { db.terminate().await() }; app.delete()
            Log.i("MBTripAccountCheck", "RESULT stage=$stage cleanupErrors=$cleanupErrors")
            assertTrue("Fixture cleanup failed: $cleanupErrors", cleanupErrors.isEmpty())
        }
    }
}
