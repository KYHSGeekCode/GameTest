package com.kyhsgeekcode.gametest

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.games.SnapshotsClient
import com.google.android.gms.games.snapshot.Snapshot
import com.google.android.gms.games.snapshot.SnapshotMetadata
import com.google.android.gms.games.snapshot.SnapshotMetadataChange
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import timber.log.Timber
import java.io.IOException


class GameData {

    constructor(
        scope: CoroutineScope,
        snapshotMetadata: SnapshotMetadata,
        snapshotsClient: SnapshotsClient
    ) {
        name = snapshotMetadata.uniqueName
        val deferred: CompletableDeferred<Pair<Snapshot?, ByteArray?>> = CompletableDeferred()
        loadSnapshot(name, snapshotsClient).addOnCompleteListener {
            deferred.complete(it.result)
        }
        snapshot = scope.async {
            deferred.await().first
        }
        data = scope.async {
            deferred.await().second
        }
        coverImage = snapshotMetadata.coverImageUri
        desc = snapshotMetadata.description
    }

    constructor(scope: CoroutineScope, uniqueName: String) {
        name = uniqueName
        coverImage = null
        snapshot = scope.async {
            null
        }
        desc = null
        data = scope.async {
            byteArrayOf()
        }
    }

    val name: String
    val snapshot: Deferred<Snapshot?>
    val coverImage: Uri?
    val desc: String?
    val data: Deferred<ByteArray?>

    fun serialize(): ByteArray {
        return byteArrayOf(0, 1, 2, 3)
    }
}


class MainViewModel : ViewModel() {
    fun loadGame(snapshotMetadata: SnapshotMetadata, snapshotsClient: SnapshotsClient) {
        _currentGameData.value = GameData(viewModelScope, snapshotMetadata, snapshotsClient)
    }

    fun createGame(uniqueName: String) {
        _currentGameData.value = GameData(viewModelScope, uniqueName)
    }

    private val gameDataa = HashMap<String, GameData>()

    private val _currentGameData: MutableLiveData<GameData> = MutableLiveData(null)
    val currentGameData: LiveData<GameData> = _currentGameData

    val _debugMsg = MutableLiveData<String>()
    val debugMsg: LiveData<String> = _debugMsg

    suspend fun saveSnapshot(snapshotsClient: SnapshotsClient) {
        val deferred = CompletableDeferred(Unit)
        currentGameData.value?.run {
//            writeSnapshot(
//                snapshotsClient,
//                snapshot.await(),
//                serialize(),
//                coverImage,
//                desc
//            )?.addOnCompleteListener {
//                deferred.complete(Unit)
//            } ?: deferred.complete(Unit)
            deferred.await()
        }
    }

    private fun writeSnapshot(
        snapshotsClient: SnapshotsClient,
        snapshot: Snapshot,
        data: ByteArray, coverImage: Bitmap, desc: String
    ): Task<SnapshotMetadata?>? {
        // Set the data payload for the snapshot
        snapshot.snapshotContents.writeBytes(data)

        // Create the change operation
        val metadataChange = SnapshotMetadataChange.Builder()
            .setCoverImage(coverImage)
            .setDescription(desc)
            .build()
        // Commit the operation
        return snapshotsClient.commitAndClose(snapshot, metadataChange)
    }

}

fun loadSnapshot(
    uniqueName: String,
    snapshotsClient: SnapshotsClient
): Task<Pair<Snapshot?, ByteArray?>> {
    // Display a progress dialog
    // ...

    // In the case of a conflict, the most recently modified version of this snapshot will be used.
    val conflictResolutionPolicy = SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED

    // Open the saved game using its name.
    return snapshotsClient.open(uniqueName, true, conflictResolutionPolicy)
        .addOnFailureListener { e -> Timber.e("Error while opening Snapshot.", e) }
        .continueWith {
            val snapshot = it.result.data
            // Opening the snapshot was a success and any conflicts have been resolved.
            Pair(
                snapshot, try {
                    // Extract the raw data from the snapshot.
                    snapshot.snapshotContents.readFully()
                } catch (e: IOException) {
                    Timber.e("Error while reading Snapshot.", e)
                    null
                }
            )
        }
}