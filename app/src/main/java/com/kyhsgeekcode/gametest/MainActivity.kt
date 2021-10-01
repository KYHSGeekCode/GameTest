package com.kyhsgeekcode.gametest

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.games.Games
import com.google.android.gms.games.SnapshotsClient
import com.google.android.gms.games.snapshot.SnapshotMetadata
import com.kyhsgeekcode.gametest.ui.theme.GameTestTheme
import kotlinx.coroutines.launch
import timber.log.Timber
import timber.log.Timber.*
import java.math.BigInteger
import java.util.*


class MainActivity : GoogleSignInActivity() {
//    init {
//        autoGoogleSignIn()
//    }

    private val viewModel by viewModels<MainViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.plant(DebugTree())

        setContent {
            GameTestTheme {
                val viewModel = MainViewModel()
                MainScreen(viewModel = viewModel)
            }
        }
    }

    @Composable
    fun MainScreen(viewModel: MainViewModel) {
        Surface(color = MaterialTheme.colors.background) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SignInButton {
                    lifecycleScope.launch {
                        signInSilently()
                    }
                }
                SignOutButton {
                    lifecycleScope.launch {
                        signOut()
                    }
                }
                LoadButton {
                    showSavedGamesUI()
                }
                SaveButton {
                    lifecycleScope.launch {
                        viewModel.saveSnapshot(latestSnapshotsClient() ?: return@launch)
                    }
                }
            }
        }
    }

    private val getSavedGameRequestLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
            if (intent != null) {
                val latestSnapshotsClient =
                    latestSnapshotsClient() ?: return@registerForActivityResult
                if (intent.hasExtra(SnapshotsClient.EXTRA_SNAPSHOT_METADATA)) {
                    // Load a snapshot.
                    val snapshotMetadata: SnapshotMetadata? =
                        intent.getParcelableExtra(SnapshotsClient.EXTRA_SNAPSHOT_METADATA)
                    snapshotMetadata?.run {
                        viewModel.loadGame(this, latestSnapshotsClient)
                    }
                } else if (intent.hasExtra(SnapshotsClient.EXTRA_SNAPSHOT_NEW)) {
                    // Create a new snapshot named with a unique string

                    val unique: String = BigInteger(281, Random()).toString(13)
                    viewModel.createGame("snapshotTemp-$unique")
                }
            }
        }

    private fun showSavedGamesUI() {
        val snapshotsClient = latestSnapshotsClient() ?: return
        val maxNumberOfSavedGamesToShow = 3
        val intentTask = snapshotsClient.getSelectSnapshotIntent(
            "See My Saves", true, true, maxNumberOfSavedGamesToShow
        )
        intentTask.addOnSuccessListener { intent -> getSavedGameRequestLauncher.launch(intent) }
    }

    private fun latestSnapshotsClient(): SnapshotsClient? {
        val lastAccount = GoogleSignIn.getLastSignedInAccount(this) ?: run {
            Timber.e("No recent sign in")
            return null
        }
        return Games.getSnapshotsClient(this, lastAccount)
    }
}


@Composable
fun SignInButton(onclick: () -> Unit) {
    Button(onClick = {
        onclick()
    }) {
        Text("Sign in")
    }
}

@Composable
fun SignOutButton(onclick: () -> Unit){
    Button(onClick = {
        onclick()
    }) {
        Text("Sign out")
    }
}

@Composable
fun SaveButton(onclick: () -> Unit) {
    Button(onClick = {
        onclick()
    }) {
        Text("Save")
    }
}

@Composable
fun LoadButton(onclick: () -> Unit) {
    Button(onClick = {
        onclick()
    }) {
        Text("Save")
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    GameTestTheme {
        Surface(color = MaterialTheme.colors.background) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SignInButton {}
                LoadButton {}
                SaveButton {}
            }
        }
    }
}