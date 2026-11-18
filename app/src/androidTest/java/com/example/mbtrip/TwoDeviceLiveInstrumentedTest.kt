package com.example.mbtrip

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.mbtrip.friends.FriendListScreen
import com.example.mbtrip.timeline.model.*
import com.example.mbtrip.timeline.ui.TimelineScreen
import com.example.mbtrip.travel.*
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.LocalDate

/** Run concurrently on TWO devices with the same mbtripTwoDeviceRun and owner/guest roles.
 * Transfer only the newly issued test invitation via the handoff file; never default Auth.
 */
class TwoDeviceLiveInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun invitationDiagnosisRecommendationAndBidirectionalSync() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("mbtripTwoDevice") == "true")
        val run = requireNotNull(args.getString("mbtripTwoDeviceRun"))
        require(Regex("[a-f0-9-]{36}").matches(run))
        val role = requireNotNull(args.getString("mbtripTwoDeviceRole"))
        require(role in setOf("owner", "guest"))
        val context = instrumentation.targetContext
        val app = FirebaseApp.initializeApp(context, FirebaseApp.getInstance().options, "two-device-$run-$role")
        val auth = FirebaseAuth.getInstance(app)
        val db = FirebaseFirestore.getInstance(app).apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build()
        }
        val prefs = "two_device_${run}_$role"
        val repo = TravelRepository(context, db, auth, prefs)
        val invites = InviteRepository(db, auth)
        val handoff = File(context.getExternalFilesDir(null), "two-device-$run.json")
        var trip: ActiveTrip? = null
        var code: String? = null
        var stage = "initialize"
        val cleanup = mutableListOf<String>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            withTimeout(180_000) {
                val uid = auth.signInAnonymously().await().user!!.uid
                repo.saveDiagnosis(if (role == "owner") 20 else 80, null)
                db.collection("users").document(uid).set(mapOf("nickname" to if (role == "owner") "기기A" else "기기B"), SetOptions.merge()).await()
                val date = LocalDate.now()
                stage = "invitation"
                val active = if (role == "owner") {
                    repo.create(TripInput("2기기검증 $run", "시흥", date, date, 2)).also {
                        trip = it
                        code = invites.issue(it)
                        handoff.writeText(JSONObject().put("run", run).put("tripId", it.id).put("code", code).toString())
                    }
                } else {
                    while (!handoff.exists()) delay(500)
                    val data = JSONObject(handoff.readText())
                    require(data.getString("run") == run)
                    assertEquals(data.getString("tripId"), invites.join(data.getString("code")))
                    repo.openTrip(data.getString("tripId")).also { repo.saveDiagnosis(80, it) }
                }
                trip = active
                val initialMembers = withTimeout(90_000) { repo.observeMembers(active).first { it.size == 2 && it.all { m -> m.hasDiagnosis } } }
                assertEquals(setOf(20, 80), initialMembers.map { it.jpScore }.toSet())
                assertEquals(2, initialMembers.map { it.userId }.toSet().size)
                assertEquals(role == "owner", uid == active.ownerId)
                val members = mutableStateOf(initialMembers)
                val blocks = mutableStateOf<List<Block>>(emptyList())
                val timeline = mutableStateOf(false)
                scope.launch { repo.observeMembers(active).collect { value -> withContext(Dispatchers.Main) { members.value = value } } }
                scope.launch { repo.observeBlocks(active).collect { value -> withContext(Dispatchers.Main) { blocks.value = value } } }
                compose.setContent {
                    MaterialTheme {
                        if (!timeline.value) FriendListScreen(members.value, onBack = {})
                        else TimelineScreen(initialBlocks = blocks.value, members = members.value, tripId = active.id,
                            tripStart = active.input.start, tripEnd = active.input.end, markers = emptyList(),
                            onBlocksChange = { before, after -> repo.saveBlocks(active, before, after) })
                    }
                }
                stage = "members_ui"
                compose.onNodeWithText("기기A").assertExists()
                compose.onNodeWithText("기기B").assertExists()
                compose.runOnIdle { timeline.value = true }
                if (role == "owner") {
                    stage = "recommendation_ui"
                    compose.onNodeWithText("맛집·카페 추천").performClick()
                    compose.onAllNodes(isToggleable())[0].performClick()
                    compose.onNodeWithText("배분 미리보기").performClick()
                    compose.onNodeWithText("일정에 반영").performClick()
                }
                stage = "recommendation_received"
                compose.waitUntil(90_000) { blocks.value.any { it.source == BlockSource.RECOMMEND } }
                val recommendation = blocks.value.first { it.source == BlockSource.RECOMMEND }
                assertEquals("경기도 시흥시 동서로811번길 33", recommendation.address)
                compose.onAllNodesWithText(recommendation.title, substring = true).onFirst().assertIsDisplayed()
                compose.waitForIdle()
                instrumentation.waitForIdleSync()
                // SurfaceFlinger may display the prior frame briefly on a resource-limited AVD.
                Thread.sleep(750)
                val windows = android.os.ParcelFileDescriptor.AutoCloseInputStream(
                    instrumentation.uiAutomation.executeShellCommand("dumpsys window displays")).bufferedReader().use { it.readText() }
                val focus = windows.lineSequence().firstOrNull { it.contains("mCurrentFocus=") }.orEmpty()
                assertTrue("App window is obscured: $focus", focus.contains("com.example.mbtrip"))
                instrumentation.uiAutomation.takeScreenshot()?.let { bitmap ->
                    File(context.getExternalFilesDir(null), "two-device-$run-$role.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    bitmap.recycle()
                }
                if (role == "guest") {
                    stage = "guest_edit"
                    val before = blocks.value
                    repo.saveBlocks(active, before, before.map { if (it.id == recommendation.id) it.copy(memo = "2기기 응답 $run") else it })
                    withTimeout(60_000) { repo.observeMembers(active).first { it.any { m -> m.userId == active.ownerId && m.jpScore == 21 } } }
                } else {
                    stage = "owner_receives_guest_edit"
                    compose.waitUntil(90_000) { blocks.value.any { it.memo == "2기기 응답 $run" } }
                    repo.saveDiagnosis(21, active)
                    val guestUid = initialMembers.single { it.userId != uid }.userId
                    // Guest deletes its own temporary profile after receiving our diagnosis update.
                    while (db.collection("users").document(guestUid).get(Source.SERVER).await().exists()) delay(500)
                }
                stage = "passed"
            }
        } finally {
            scope.cancel()
            suspend fun clean(label: String, action: suspend () -> Unit) {
                try { withTimeout(15_000) { action() } } catch (e: Exception) { cleanup += "$label:${e.javaClass.simpleName}" }
            }
            if (role == "owner") {
                code?.let { token -> clean("invite") { db.collection("tripInvites").document(token).delete().await() } }
                trip?.let { active -> clean("trip") {
                    val ref = db.collection("trips").document(active.id)
                    val doc = ref.get(Source.SERVER).await()
                    check(doc.getString("ownerId") == auth.currentUser?.uid && doc.getString("departure") == "2기기검증 $run")
                    for (collection in listOf("blocks", "members")) {
                        val documents = ref.collection(collection).get(Source.SERVER).await().documents
                        if (documents.isNotEmpty()) db.batch().apply { documents.forEach { delete(it.reference) } }.commit().await()
                    }
                    ref.delete().await()
                } }
            }
            auth.currentUser?.let { user ->
                clean("profile") { db.collection("users").document(user.uid).delete().await() }
                context.deleteSharedPreferences("${prefs}_${user.uid}")
                clean("account") { user.delete().await() }
            }
            auth.signOut()
            context.deleteSharedPreferences(prefs)
            handoff.delete()
            clean("connection") { db.terminate().await() }
            app.delete()
            Log.i("MBTripTwoDevice", "RESULT role=$role stage=$stage cleanupErrors=$cleanup")
            assertTrue("Temporary fixture cleanup failed: $cleanup", cleanup.isEmpty())
        }
    }
}
