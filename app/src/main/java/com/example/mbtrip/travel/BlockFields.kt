package com.example.mbtrip.travel

import com.example.mbtrip.timeline.model.*
import java.time.LocalDate
import java.time.LocalTime

/** Team document schema; old start/end/kind/string-place documents are accepted on read. */
object BlockFields {
    fun encode(b: Block): Map<String, Any?> = mapOf(
        "tripId" to b.tripId, "date" to b.date.toString(), "startTime" to b.start.toString(), "endTime" to b.end.toString(),
        "type" to b.kind.name.lowercase(), "status" to b.status.name.lowercase(),
        "source" to when (b.source) { BlockSource.RECOMMEND -> "siheung"; BlockSource.TIDE, BlockSource.SUNSET -> "auto"; else -> b.source.name.lowercase() },
        "autoKind" to when (b.source) { BlockSource.TIDE -> "tide"; BlockSource.SUNSET -> "sunset"; else -> null },
        "title" to b.title,
        "place" to if (b.place == null && b.lat == null && b.lng == null) null else buildMap<String, Any> {
            put("name", b.place.orEmpty())
            if (b.lat != null && b.lng != null) { put("lat", b.lat); put("lng", b.lng) }
        },
        "locked" to b.locked, "version" to b.version, "tags" to b.tags,
        "memo" to b.memo, "todo" to b.todo, "transport" to b.transport?.name,
        "address" to b.address, "deleteRequested" to b.deleteRequested,
    )
    fun decode(id: String, tripId: String, d: Map<String, Any?>): Block {
        val place = d["place"]
        val map = place as? Map<*, *>
        val source = when ((d["source"] as? String)?.lowercase()) {
            "siheung", "recommend" -> BlockSource.RECOMMEND
            "auto" -> when ((d["autoKind"] as? String)?.lowercase()) {
                "tide" -> BlockSource.TIDE; "sunset" -> BlockSource.SUNSET; else -> BlockSource.AUTO
            }
            else -> BlockSource.valueOf((d["source"] as? String ?: "user").uppercase())
        }
        return Block(
            id, tripId, LocalDate.parse(d["date"] as String),
            LocalTime.parse((d["startTime"] ?: d["start"]) as String),
            LocalTime.parse((d["endTime"] ?: d["end"]) as String),
            BlockKind.valueOf(((d["type"] ?: d["kind"]) as String).uppercase()),
            BlockStatus.valueOf((d["status"] as String).uppercase()), source,
            title = d["title"] as? String ?: "", place = if (map == null) place as? String else map["name"] as? String,
            locked = d["locked"] as? Boolean ?: false, version = (d["version"] as? Number)?.toInt() ?: 1,
            tags = (d["tags"] as? List<*>)?.filterIsInstance<String>().orEmpty(), memo = d["memo"] as? String,
            todo = d["todo"] as? String ?: "", transport = (d["transport"] as? String)?.let { Transport.valueOf(it.uppercase()) },
            lat = ((map?.get("lat") ?: d["lat"]) as? Number)?.toDouble(),
            lng = ((map?.get("lng") ?: d["lng"]) as? Number)?.toDouble(),
            address = d["address"] as? String, deleteRequested = d["deleteRequested"] as? Boolean ?: false,
        )
    }
}
