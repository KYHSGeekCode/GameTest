package com.kyhsgeekcode.gametest

import com.google.android.gms.games.snapshot.Snapshot
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class GameData(var level: Int = 0) {
    fun toByteArray(): ByteArray {
        return Json.encodeToString(this).toByteArray(Charsets.UTF_8)
    }
}

fun fromSnapshot(snapshot: Snapshot): GameData =
    Json.decodeFromString(String(snapshot.snapshotContents.readFully()))
