package com.kyhsgeekcode.gametest

//fun downloadSavedGameData(snapshotsClient: SnapshotsClient, name: String) {
//    snapshotsClient.open(
//        name,
//        true,
//        SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED
//    ).addOnFailureListener { e ->
//        Timber.e("Error while opening snapshot: ", e)
//    }.continueWith {
//        val snapshot = it.result.data
//        // Opening the snapshot was a success and any conflicts have been resolved.
//        try {
//            // Extract the raw data from the snapshot.
//            snapshot?.snapshotContents?.readFully()
//        } catch (e: IOException) {
//            Timber.e("Error while reading snapshot: ", e)
//        } catch (e: NullPointerException) {
//            Timber.e("Error while reading snapshot: ", e)
//        }
//        null
//    }.addOnCompleteListener { task ->
//        if (task.isSuccessful) {
//            val data = task.result
//
//        } else {
//            val ex = task.exception
//            Timber.d(
//                "Failed to load saved game data: " + if (ex != null) ex.message else "UNKNOWN"
//            )
//        }
//    }
//}