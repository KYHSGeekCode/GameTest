package com.kyhsgeekcode.gametest

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.games.SnapshotsClient
import com.google.android.gms.games.snapshot.Snapshot
import com.google.android.gms.games.snapshot.SnapshotMetadata
import com.google.android.gms.games.snapshot.SnapshotMetadataChange
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.CompletableDeferred
import timber.log.Timber


sealed class GameDataResult<out T : Any> {
    data class Success<out T : Any>(val data: T) : GameDataResult<T>()
    data class Error<out T : Any>(val exception: Exception) : GameDataResult<T>()
    object NotLoaded : GameDataResult<Nothing>()
    object InProgress : GameDataResult<Nothing>()
}


//class GameData {
//
//    constructor(
//        scope: CoroutineScope,
//        snapshotMetadata: SnapshotMetadata,
//        snapshotsClient: SnapshotsClient
//    ) {
//        name = snapshotMetadata.uniqueName
//        val deferred: CompletableDeferred<Pair<Snapshot?, ByteArray?>> = CompletableDeferred()
//        loadSnapshot(name, snapshotsClient).addOnCsompleteListener {
//            deferred.complete(it.result)
//        }
//        snapshot = scope.async {
//            deferred.await().first
//        }
//        data = scope.async {
//            deferred.await().second
//        }
//        coverImage = snapshotMetadata.coverImageUri
//        desc = snapshotMetadata.description
//    }
//
//    constructor(scope: CoroutineScope, uniqueName: String) {
//        name = uniqueName
//        coverImage = null
//        snapshot = scope.async {
//            null
//        }
//        desc = null
//        data = scope.async {
//            byteArrayOf()
//        }
//    }
//
//    val name: String
//    val snapshot: Deferred<Snapshot?>
//    val coverImage: Uri?
//    val desc: String?
//    val data: Deferred<ByteArray?>
//
//    fun serialize(): ByteArray {
//        return byteArrayOf(0, 1, 2, 3)
//    }
//}


class MainViewModel : ViewModel() {
//    fun loadGame(snapshotMetadata: SnapshotMetadata, snapshotsClient: SnapshotsClient) {
//        _currentGameData.value = GameData(viewModelScope, snapshotMetadata, snapshotsClient)
//    }
//
//    fun createGame(uniqueName: String) {
//        _currentGameData.value = GameData(viewModelScope, uniqueName)
//    }
//
//    private val gameDataa = HashMap<String, GameData>()

//    private val _currentGameData: MutableLiveData<GameData> = MutableLiveData(null)
//    val currentGameData: LiveData<GameData> = _currentGameData

    private val _gameData =
        MutableLiveData<GameDataResult<Pair<Snapshot, GameData>>>(GameDataResult.NotLoaded)
    val gameData: LiveData<GameDataResult<Pair<Snapshot, GameData>>> = _gameData

    val _debugMsg = MutableLiveData<String>()
    val debugMsg: LiveData<String> = _debugMsg

    private val _currentAccount: MutableLiveData<GoogleSignInAccount?> = MutableLiveData()
    val currentAccount = _currentAccount as LiveData<GoogleSignInAccount?>

    fun onLogin(account: GoogleSignInAccount) {
        _currentAccount.value = account
    }

    fun onLogout() {
        _currentAccount.value = null
    }

//    suspend fun saveSnapshot(snapshotsClient: SnapshotsClient) {
//        val deferred = CompletableDeferred(Unit)
//        currentGameData.value?.run {
////            writeSnapshot(
////                snapshotsClient,
////                snapshot.await(),
////                serialize(),
////                coverImage,
////                desc
////            )?.addOnCompleteListener {
////                deferred.complete(Unit)
////            } ?: deferred.complete(Unit)
//            deferred.await()
//        }
//    }

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

    suspend fun loadDefaultGame(lastsnapshot: SnapshotsClient): Boolean {
        val loaded = loadSnapshot("default", lastsnapshot) ?: run {
            _gameData.value = GameDataResult.Error(NullPointerException())
            return@loadDefaultGame false
        }
        val gd = loaded.second ?: createDefaultGameData()
        _gameData.value = GameDataResult.Success(Pair(loaded.first, gd))
        return true
    }

    private fun createDefaultGameData() = GameData(0)

    private suspend fun loadSnapshot(
        uniqueName: String,
        snapshotsClient: SnapshotsClient
    ): Pair<Snapshot, GameData?>? {
        val deferred = CompletableDeferred<Pair<Snapshot, GameData?>?>()
        _gameData.value = GameDataResult.InProgress
        // In the case of a conflict, the most recently modified version of this snapshot will be used.
        val conflictResolutionPolicy = SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED

        // Open the saved game using its name.
        snapshotsClient.open(uniqueName, true, conflictResolutionPolicy)
            .addOnFailureListener { e -> Timber.e("Error while opening Snapshot.", e) }
            .addOnCompleteListener {
                // Opening the snapshot was a success and any conflicts have been resolved.
                val snapshot = it.result.data
                val data = try {
                    snapshot?.run { fromSnapshot(this) }
                } catch (e: Exception) {
                    Timber.e("Failed to decode snapshot", e)
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
        return _gameData.mutation {
            val gd = (it.value as? GameDataResult.Success)?.data?.second ?: return@mutation false
            gd.level++
            true
        }
    }
}

fun <T> MutableLiveData<T>.mutation(actions: (MutableLiveData<T>) -> Boolean): Boolean {
    val isUpdated = actions(this)
    if (isUpdated) {
        val d = this.value
        this.value = null
        this.value = d
    }
    return isUpdated
}

