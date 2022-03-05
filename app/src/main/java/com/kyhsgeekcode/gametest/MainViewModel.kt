package com.kyhsgeekcode.gametest

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.games.SnapshotsClient
import com.google.android.gms.games.snapshot.Snapshot
import com.google.android.gms.games.snapshot.SnapshotMetadataChange
import kotlinx.coroutines.CompletableDeferred
import timber.log.Timber


sealed class GameDataResult<out T> {
    data class Success<T>(val data: T) : GameDataResult<T>()
    data class Error<T>(val exception: Exception) : GameDataResult<T>()
    object NotLoaded : GameDataResult<Nothing>()
    object InProgress : GameDataResult<Nothing>()
}

class MainViewModel : ViewModel() {
    var gameData by mutableStateOf(createDefaultGameData())
        private set

    var snapshot by mutableStateOf<GameDataResult<Snapshot>>(GameDataResult.NotLoaded)
        private set

    var debugMsg by mutableStateOf("")
        private set

    var currentAccount by mutableStateOf<GoogleSignInAccount?>(null)

    fun onLogin(account: GoogleSignInAccount) {
        currentAccount = account
    }

    fun onLogout() {
        currentAccount = null
    }

    private suspend fun writeSnapshot(
        snapshotsClient: SnapshotsClient,
        snapshot: Snapshot,
        data: ByteArray, coverImage: Bitmap?, desc: String
    ) {
        // Set the data payload for the snapshot
        snapshot.snapshotContents.writeBytes(data)

        // Create the change operation
        val metadataChange = SnapshotMetadataChange.Builder()
//            .setCoverImage(coverImage)
            .setDescription(desc)
            .build()
        // Commit the operation
        val deferred = CompletableDeferred(Unit)
        snapshotsClient.commitAndClose(snapshot, metadataChange).addOnCompleteListener {
            deferred.complete(Unit)
        }
        Timber.d("waiting writing")
        deferred.await()
    }

    suspend fun loadDefaultGame(lastsnapshot: SnapshotsClient): Boolean {
        val loaded = loadSnapshot("default", lastsnapshot) ?: run {
            snapshot = GameDataResult.Error(NullPointerException())
            return@loadDefaultGame false
        }
        gameData = loaded.second ?: createDefaultGameData()
        return true
    }

    private fun createDefaultGameData(): GameData {
        Timber.w("Creating default data")
        return GameData(0)
    }

    private suspend fun loadSnapshot(
        uniqueName: String,
        snapshotsClient: SnapshotsClient
    ): Pair<Snapshot, GameData?>? {
        val deferred = CompletableDeferred<Pair<Snapshot, GameData?>?>()
        snapshot = GameDataResult.InProgress
        // In the case of a conflict, the most recently modified version of this snapshot will be used.
        val conflictResolutionPolicy = SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED

        // Open the saved game using its name.
        snapshotsClient.open(uniqueName, true, conflictResolutionPolicy)
            .addOnFailureListener { e ->
                Timber.e(e, "Error while opening Snapshot.")
            }
            .addOnSuccessListener {
                // Opening the snapshot was a success and any conflicts have been resolved.
                val snapshot = it.data
                val data = try {
                    snapshot?.run { fromSnapshot(this) }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to decode snapshot")
                    null
                }
                deferred.complete(
                    if (snapshot != null) {
                        if (data != null) {
                            Pair(snapshot, data)
                        } else {
                            Pair(snapshot, null)
                        }
                    } else {
                        null
                    }
                )
            }
        return deferred.await()
    }

    fun levelUp(): Boolean {
        gameData = gameData.copy(level = gameData.level + 1)
        return true
    }

    suspend fun saveSnapshot(snapshotsClient: SnapshotsClient) {
        val snapshot = (snapshot as? GameDataResult.Success)?.data ?: return
        writeSnapshot(snapshotsClient, snapshot, gameData.toByteArray(), null, "def")
    }
}
