package com.example.mbtrip

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.mbtrip.recommendation.PlaceRecommender
import com.example.mbtrip.recommendation.SiheungPlaces
import com.example.mbtrip.travel.*
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Source
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

/** Explicitly opt in: -Pandroid.testInstrumentationRunnerArguments.mbtripLiveFirebase=true.
 * Uses isolated Auth sessions and newly created fixtures; never uses the default user's session.
 */
@RunWith(AndroidJUnit4::class)
class FirebaseLiveIntegrationTest {
    @Test fun isolatedLiveRoundTrip() = runBlocking {
        assumeTrue("Live Firebase checks are opt-in", InstrumentationRegistry.getArguments().getString("mbtripLiveFirebase") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val run = UUID.randomUUID().toString()
        val options = FirebaseApp.getInstance().options
        val ownerApp = FirebaseApp.initializeApp(context, options, "mbtrip-check-owner-$run")
        val guestApp = FirebaseApp.initializeApp(context, options, "mbtrip-check-guest-$run")
        val ownerAuth = FirebaseAuth.getInstance(ownerApp)
        val guestAuth = FirebaseAuth.getInstance(guestApp)
        val ownerDb = FirebaseFirestore.getInstance(ownerApp)
        val guestDb = FirebaseFirestore.getInstance(guestApp)
        val settings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build()
        ownerDb.firestoreSettings = settings; guestDb.firestoreSettings = settings
        val ownerPrefs = "mbtrip_check_owner_$run"
        val guestPrefs = "mbtrip_check_guest_$run"
        val owner = TravelRepository(context, ownerDb, ownerAuth, ownerPrefs)
        val guest = TravelRepository(context, guestDb, guestAuth, guestPrefs)
        var fixture: ActiveTrip? = null
        var stage = "initialize"
        var failure: Throwable? = null
        val cleanupErrors = mutableListOf<String>()
        fun step(name: String) { stage = name; Log.i("MBTripFirebaseCheck", "STEP $name") }
        try {
            withTimeout(90_000) {
                step("owner_anonymous_auth")
                val ownerUid = requireNotNull(ownerAuth.signInAnonymously().await().user?.uid)
                step("guest_anonymous_auth")
                val guestUid = requireNotNull(guestAuth.signInAnonymously().await().user?.uid)
                assertNotEquals(ownerUid, guestUid)

                step("save_user_diagnosis")
                owner.saveDiagnosis(20, null)
                assertEquals(20L, ownerDb.collection("users").document(ownerUid).get(Source.SERVER).await().getLong("jpScore"))
                assertEquals(20, owner.cachedScore())

                step("create_test_trip")
                val date = LocalDate.of(2026, 11, 17)
                val active = owner.create(TripInput("자동검증 $run", "시흥", date, date, 2))
                fixture = active
                val tripRef = ownerDb.collection("trips").document(active.id)
                assertEquals(ownerUid, tripRef.get(Source.SERVER).await().getString("ownerId"))
                assertEquals(active, owner.restore())
                withTimeout(15_000) { owner.observeMembers(active).first { list -> list.size == 1 && list.single().jpScore == 20 } }

                step("legacy_invite_query_before_membership")
                val inviteToken = "audit-$run"
                tripRef.update("inviteCode", inviteToken).await()
                val queryError = runCatching {
                    guestDb.collection("trips").whereEqualTo("inviteCode", inviteToken).limit(1).get(Source.SERVER).await()
                }.exceptionOrNull()
                val queryDenied = queryError is FirebaseFirestoreException && queryError.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
                assertTrue("Legacy invite-code collection query must not expose nonmember trips", queryDenied)
                step("uninvited_user_registration_denied")
                val selfAddError = runCatching {
                    guestDb.collection("trips").document(active.id).collection("members").document(guestUid)
                        .set(mapOf("userId" to guestUid, "tripId" to active.id, "jpScore" to 80)).await()
                }.exceptionOrNull()
                val selfAddDenied = selfAddError is FirebaseFirestoreException && selfAddError.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
                assertTrue("Uninvited users must not add themselves", selfAddDenied)
                Log.i("MBTripFirebaseCheck", "INVITE_AUDIT legacyQueryDenied=$queryDenied uninvitedSelfAddDenied=$selfAddDenied")

                step("register_test_companion")
                tripRef.collection("members").document(guestUid).set(mapOf(
                    "userId" to guestUid, "tripId" to active.id, "nickname" to "자동검증 동행자",
                    "handle" to "@validation_guest", "jpScore" to 80,
                )).await()
                val joined = guest.openTrip(active.id)
                assertEquals(active, joined)
                withTimeout(15_000) { guest.observeMembers(joined).first { it.size == 2 } }

                step("save_member_diagnosis")
                guest.saveDiagnosis(80, joined)
                assertEquals(80L, tripRef.collection("members").document(guestUid).get(Source.SERVER).await().getLong("jpScore"))

                step("save_recommended_schedule")
                val result = PlaceRecommender.previewDay(emptyList(), active.id, date,
                    LocalTime.of(9, 0), LocalTime.of(17, 0), listOf(20,80),
                    listOf(SiheungPlaces.all[0], SiheungPlaces.all[20]))
                owner.saveBlocks(active, emptyList(), result)
                val fromGuest = withTimeout(15_000) { guest.observeBlocks(joined).first { it.isNotEmpty() } }
                assertEquals(result.toSet(), fromGuest.toSet())

                step("member_votes_and_comments")
                val blockRef = guestDb.collection("trips").document(active.id).collection("blocks").document(result.first().id)
                guestDb.batch().apply {
                    set(blockRef.collection("votes").document(guestUid), mapOf(
                        "blockId" to result.first().id, "voterId" to guestUid, "value" to "agree"))
                    set(blockRef.collection("comments").document("validation_comment"), mapOf(
                        "blockId" to result.first().id, "authorId" to guestUid, "content" to "검증 의견"))
                }.commit().await()
                assertEquals(1, tripRef.collection("blocks").document(result.first().id)
                    .collection("votes").get(Source.SERVER).await().size())
                assertEquals(1, tripRef.collection("blocks").document(result.first().id)
                    .collection("comments").get(Source.SERVER).await().size())

                step("reconnect_and_reload")
                val reconnected = TravelRepository(context, guestDb, guestAuth, guestPrefs)
                assertEquals(joined, reconnected.restore())
                assertEquals(joined, reconnected.openTrip(joined.id))
                assertEquals(result.toSet(), withTimeout(15_000) { reconnected.observeBlocks(joined).first { it.isNotEmpty() } }.toSet())

                step("member_schedule_write")
                val edited = result.mapIndexed { i, block -> if (i == 0) block.copy(memo = "동행자 저장 검증", version = block.version + 1) else block }
                guest.saveBlocks(joined, result, edited)
                assertEquals(edited.toSet(), withTimeout(15_000) { owner.observeBlocks(active).first { it.toSet() == edited.toSet() } }.toSet())

                step("reject_stale_schedule")
                val stale = runCatching { owner.saveBlocks(active, result, result.map { it.copy(memo = "오래된 화면") }) }.exceptionOrNull()
                assertTrue("Stale schedules must be rejected", stale is IllegalArgumentException)

                step("concurrent_schedule_conflict")
                val versionBeforeConflict = tripRef.get(Source.SERVER).await().getLong("scheduleVersion")!!
                val concurrent = coroutineScope {
                    val a = async { runCatching { owner.saveBlocks(active, edited, edited.mapIndexed { i, b -> if (i == 0) b.copy(memo = "동시 편집 A") else b }) } }
                    val b = async { runCatching { guest.saveBlocks(joined, edited, edited.mapIndexed { i, block -> if (i == 0) block.copy(memo = "동시 편집 B") else block }) } }
                    listOf(a.await(), b.await())
                }
                assertEquals("Exactly one conflicting edit may succeed", 1, concurrent.count { it.isSuccess })
                val conflictError = concurrent.single { it.isFailure }.exceptionOrNull()
                val conflictChain = generateSequence(conflictError) { it.cause }.take(8).toList()
                Log.i("MBTripFirebaseCheck", "CONFLICT_ERROR " + conflictChain.joinToString(" -> ") { code(it) + ":" + it.message })
                assertTrue("Conflicting schedule must be rejected by the stale-data guard", conflictChain.any { it is IllegalArgumentException })

                assertEquals(versionBeforeConflict + 1, tripRef.get(Source.SERVER).await().getLong("scheduleVersion"))
                val winnerMemo = if (concurrent[0].isSuccess) "동시 편집 A" else "동시 편집 B"
                assertEquals(winnerMemo, tripRef.collection("blocks").document(edited.first().id).get(Source.SERVER).await().getString("memo"))
                step("reject_guest_removal")
                val forbidden = runCatching { guest.removeMember(joined, ownerUid) }.exceptionOrNull()
                assertTrue("Only the trip owner can remove companions", forbidden is IllegalArgumentException)

                step("remove_test_companion")
                owner.removeMember(active, guestUid)
                withTimeout(15_000) { owner.observeMembers(active).first { list -> list.size == 1 && list.single().userId == ownerUid } }
                assertFalse(tripRef.collection("members").document(guestUid).get(Source.SERVER).await().exists())

                step("reject_removed_member_reentry")
                val reentry = runCatching { guest.openTrip(active.id) }.exceptionOrNull()
                assertTrue(reentry is IllegalArgumentException ||
                    (reentry is FirebaseFirestoreException && reentry.code == FirebaseFirestoreException.Code.PERMISSION_DENIED))

                step("server_access_rules")
                val guestBlocks = guestDb.collection("trips").document(active.id).collection("blocks")
                val readError = runCatching { guestBlocks.get(Source.SERVER).await() }.exceptionOrNull()
                val probe = result.first().copy(id = "permission_probe_$run")
                val writeError = runCatching { guestBlocks.document(probe.id).set(BlockFields.encode(probe)).await() }.exceptionOrNull()
                val readDenied = readError is FirebaseFirestoreException && readError.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
                val writeDenied = writeError is FirebaseFirestoreException && writeError.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
                val readdError = runCatching {
                    guestDb.collection("trips").document(active.id).collection("members").document(guestUid)
                        .set(mapOf("userId" to guestUid, "tripId" to active.id, "jpScore" to 80)).await()
                }.exceptionOrNull()
                val readdDenied = readdError is FirebaseFirestoreException && readdError.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
                Log.i("MBTripFirebaseCheck", "SERVER_ACCESS removedMemberReadDenied=$readDenied removedMemberWriteDenied=$writeDenied removedMemberReaddDenied=$readdDenied")
                assertTrue("SERVER_RULES_ALLOW_REMOVED_MEMBER: readDenied=$readDenied writeDenied=$writeDenied readdDenied=$readdDenied", readDenied && writeDenied && readdDenied)
                step("complete")
            }
        } catch (e: Throwable) { failure = e }
        finally {
            // Every target below belongs to this run's newly created, isolated Auth users.
            suspend fun clean(name: String, action: suspend () -> Unit) {
                try { withTimeout(12_000) { action() } }
                catch (e: Throwable) { cleanupErrors += "$name: ${code(e)}" }
            }
            val created = fixture
            if (created != null) clean("test_trip") {
                val ref = ownerDb.collection("trips").document(created.id)
                val doc = ref.get(Source.SERVER).await()
                check(doc.getString("ownerId") == ownerAuth.currentUser?.uid)
                check(doc.getString("departure") == "자동검증 $run")
                for (collection in listOf("blocks", "members")) {
                    val docs = ref.collection(collection).get(Source.SERVER).await().documents
                    if (collection == "blocks") for (block in docs) {
                        for (nested in listOf("votes", "comments")) {
                            val childDocs = block.reference.collection(nested).get(Source.SERVER).await().documents
                            if (childDocs.isNotEmpty()) ownerDb.batch().apply { childDocs.forEach { delete(it.reference) } }.commit().await()
                        }
                    }
                    if (docs.isNotEmpty()) ownerDb.batch().apply { docs.forEach { delete(it.reference) } }.commit().await()
                }
                ref.delete().await()
            }
            for ((auth, db, label) in listOf(Triple(ownerAuth, ownerDb, "owner"), Triple(guestAuth, guestDb, "guest"))) {
                val user = auth.currentUser
                if (user != null) {
                    clean("${label}_profile") { db.collection("users").document(user.uid).delete().await() }
                    context.deleteSharedPreferences("${if (label == "owner") ownerPrefs else guestPrefs}_${user.uid}")
                    clean("${label}_auth") { user.delete().await() }
                }
                auth.signOut()
            }
            context.getSharedPreferences(ownerPrefs, 0).edit().clear().commit()
            context.getSharedPreferences(guestPrefs, 0).edit().clear().commit()
            clean("owner_connection") { ownerDb.terminate().await() }
            clean("guest_connection") { guestDb.terminate().await() }
            ownerApp.delete(); guestApp.delete()
            Log.i("MBTripFirebaseCheck", "RESULT stage=$stage failure=${failure?.let(::code)} cleanupErrors=$cleanupErrors")
        }
        val problem = failure
        if (problem != null) throw AssertionError("Firebase validation failed at $stage: ${code(problem)}; cleanup=$cleanupErrors", problem)
        assertTrue("Test-fixture cleanup must succeed: $cleanupErrors", cleanupErrors.isEmpty())
    }

    private fun code(e: Throwable): String = when (e) {
        is FirebaseAuthException -> e.errorCode
        is FirebaseFirestoreException -> e.code.name
        else -> e.javaClass.simpleName
    }
}
