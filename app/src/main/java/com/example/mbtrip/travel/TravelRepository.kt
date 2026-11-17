package com.example.mbtrip.travel

import android.content.Context
import com.example.mbtrip.timeline.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.LocalTime

/** Current-trip integration; no fake user IDs or sample trip IDs. */
class TravelRepository(
    context: Context,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    preferencesName: String = "mbtrip_active_travel",
) {
    private val appContext = context.applicationContext
    private val preferencesKey = preferencesName
    private val prefs = appContext.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    private fun tripPrefs(userId: String) = appContext.getSharedPreferences("${preferencesKey}_$userId", Context.MODE_PRIVATE)

    private suspend fun uid(): String = auth.currentUser?.uid ?: auth.signInAnonymously().await().user?.uid
        ?: error("사용자를 확인하지 못했어요. 다시 시도해주세요.")
    private fun trip(id: String) = db.collection("trips").document(id.also { require(it.isNotBlank() && '/' !in it) })

    fun restore(): ActiveTrip? = runCatching {
        val uid = auth.currentUser?.uid ?: return null
        val saved = tripPrefs(uid).takeIf { it.contains("tripId") } ?: prefs.takeIf { it.getString("uid", null) == uid }
            ?: return null
        val input = TripInput(
            saved.getString("departure", null)!!, saved.getString("destination", null)!!,
            LocalDate.parse(saved.getString("start", null)), LocalDate.parse(saved.getString("end", null)),
            saved.getInt("count", 1),
        )
        ActiveTrip(saved.getString("tripId", null)!!, saved.getString("ownerId", null), input)
    }.getOrNull()

    fun currentUid(): String? = auth.currentUser?.uid
    fun cachedScore(): Int? = auth.currentUser?.uid?.let { prefs.getInt("jp_$it", -1).takeIf { score -> score in 0..100 } }

    suspend fun create(input: TripInput): ActiveTrip {
        val userId = uid()
        val profile = db.collection("users").document(userId).get(Source.SERVER).await()
        val profileScore = profile.getLong("jpScore")?.toInt()?.takeIf { it in 0..100 }
        val ref = db.collection("trips").document()
        val active = ActiveTrip(ref.id, userId, input)
        val member = mutableMapOf<String, Any>("userId" to userId, "tripId" to ref.id,
            "nickname" to (profile.getString("nickname") ?: auth.currentUser?.displayName ?: "나"), "handle" to "@${userId.takeLast(8)}")
        (profileScore ?: cachedScore())?.let { member["jpScore"] = it }
        db.batch().apply {
            set(ref, mapOf("tripId" to ref.id, "ownerId" to userId, "departure" to input.departure,
                "destination" to input.destination, "startDate" to input.start.toString(),
                "endDate" to input.end.toString(), "headcount" to input.headcount, "scheduleVersion" to 0))
            set(ref.collection("members").document(userId), member)
        }.commit().await()
        tripPrefs(userId).edit().putString("uid", userId).putString("tripId", active.id).putString("ownerId", userId)
            .putString("departure", input.departure).putString("destination", input.destination)
            .putString("start", input.start.toString()).putString("end", input.end.toString())
            .putInt("count", input.headcount).apply()
        return active
    }

    /** Open an existing trip only if the authenticated user is already a member. */
    suspend fun openTrip(id: String): ActiveTrip {
        val userId = uid()
        val ref = trip(id.trim())
        require(ref.collection("members").document(userId).get(Source.SERVER).await().exists()) {
            "이 여행의 동행자가 아니에요. 초대받은 여행인지 확인해주세요."
        }
        val document = ref.get(Source.SERVER).await()
        require(document.exists()) { "여행을 찾을 수 없어요." }
        val input = TripInput(
            document.getString("departure")?.ifBlank { "출발지" } ?: "출발지",
            document.getString("destination") ?: error("여행 목적지가 없어요."),
            LocalDate.parse(document.getString("startDate")), LocalDate.parse(document.getString("endDate")),
            document.getLong("headcount")?.toInt() ?: 1,
        )
        // Older team documents may not have an owner field; do not guess the owner.
        val active = ActiveTrip(ref.id, document.getString("ownerId"), input)
        tripPrefs(userId).edit().putString("uid", userId).putString("tripId", active.id).putString("ownerId", active.ownerId)
            .putString("departure", input.departure).putString("destination", input.destination)
            .putString("start", input.start.toString()).putString("end", input.end.toString())
            .putInt("count", input.headcount).apply()
        return active
    }

    suspend fun saveDiagnosis(score: Int, active: ActiveTrip?) {
        require(score in 0..100)
        val userId = uid()
        val userRef = db.collection("users").document(userId)
        // A retest updates existing membership only; it must never re-add a removed user.
        db.runTransaction { tx ->
            val memberRef = active?.let { trip(it.id).collection("members").document(userId) }
            if (memberRef != null) require(tx.get(memberRef).exists()) { "현재 여행의 동행자가 아니에요." }
            tx.set(userRef, mapOf("userId" to userId, "jpScore" to score), SetOptions.merge())
            if (memberRef != null) tx.set(memberRef,
                mapOf("userId" to userId, "tripId" to active?.id, "jpScore" to score), SetOptions.merge())
        }.await()
        // Mark a diagnosis saved only after Firebase confirms it.
        prefs.edit().putInt("jp_$userId", score).apply()
    }

    fun observeMembers(active: ActiveTrip): Flow<List<Member>> = callbackFlow {
        val listener = trip(active.id).collection("members").addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            trySend(snapshot?.documents.orEmpty().map { MemberFields.decode(it.id, it.data.orEmpty(), active.isTraveling()) }
                .sortedWith(compareBy<Member> { it.userId != active.ownerId }.thenBy { it.nickname }))
        }
        awaitClose { listener.remove() }
    }

    suspend fun removeMember(active: ActiveTrip, userId: String) {
        require(userId.isNotBlank() && '/' !in userId)
        val viewer = uid()
        db.runTransaction { tx ->
            val ref = trip(active.id)
            val ownerId = tx.get(ref).getString("ownerId")
            require(ownerId == viewer) { "여행을 만든 사람만 동행자를 내보낼 수 있어요." }
            require(userId != ownerId) { "여행을 만든 사람은 내보낼 수 없어요." }
            tx.delete(ref.collection("members").document(userId))
        }.await()
    }

    fun observeBlocks(active: ActiveTrip): Flow<List<Block>> = callbackFlow {
        val listener = trip(active.id).collection("blocks").addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            try {
                trySend(snapshot?.documents.orEmpty().map { BlockFields.decode(it.id, active.id, it.data.orEmpty()) }
                    .sortedWith(compareBy<Block> { it.date }.thenBy { it.start }))
            } catch (e: Exception) { close(e) }
        }
        awaitClose { listener.remove() }
    }

    /** Save a diff atomically; stale or offline schedules must be reloaded first. */
    suspend fun saveBlocks(active: ActiveTrip, before: List<Block>, after: List<Block>) {
        require(after.all { it.tripId == active.id && it.date in active.input.start..active.input.end })
        require(after.map { it.id }.distinct().size == after.size)
        after.groupBy { it.date }.values.forEach { day ->
            require(day.sortedBy { it.start }.zipWithNext().all { (a, b) -> a.end <= b.start }) { "일정 시간이 겹쳐요." }
        }
        val userId = uid()
        val ref = trip(active.id)
        val version = ref.get(Source.SERVER).await().getLong("scheduleVersion") ?: 0L
        val serverBlocks = ref.collection("blocks").get(Source.SERVER).await().documents
            .map { BlockFields.decode(it.id, active.id, it.data.orEmpty()) }
        require(serverBlocks.toSet() == before.toSet()) { "다른 사람이 일정을 바꿨어요. 최신 일정을 확인한 뒤 다시 시도해주세요." }
        val previous = before.associateBy { it.id }
        val next = after.associateBy { it.id }
        val changed = (previous.keys + next.keys).filter { previous[it] != next[it] }
        if (changed.isEmpty()) return
        require(before.size + changed.size < 450) { "일정이 너무 많아요. 날짜별로 나눠 저장해주세요." }
        db.runTransaction { tx ->
            val current = tx.get(ref)
            require((current.getLong("scheduleVersion") ?: 0L) == version) { "다른 사람이 일정을 바꿨어요. 다시 시도해주세요." }
            require(tx.get(ref.collection("members").document(userId)).exists()) { "현재 여행의 동행자가 아니에요." }
            // Read before writing, including unchanged reservations used by the allocator.
            (previous.keys + changed).toSet().forEach { id ->
                val snapshot = tx.get(ref.collection("blocks").document(id))
                val remote = if (snapshot.exists()) BlockFields.decode(id, active.id, snapshot.data.orEmpty()) else null
                require(remote == previous[id]) { "일정이 변경됐어요. 최신 일정을 확인해주세요." }
            }
            changed.forEach { id ->
                val block = next[id]
                val blockRef = ref.collection("blocks").document(id)
                if (block == null) tx.delete(blockRef) else tx.set(blockRef, BlockFields.encode(block), SetOptions.merge())
            }
            tx.update(ref, "scheduleVersion", version + 1)
        }.await()
    }
}
