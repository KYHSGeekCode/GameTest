package com.kyhsgeekcode.gametest

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.MainThread
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.games.Games
import com.google.android.gms.games.SnapshotsClient
import com.kyhsgeekcode.gametest.ui.theme.GameTestTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import timber.log.Timber.*
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
        val accountName = viewModel.currentAccount.observeAsState()
        val level = viewModel.realGameData.observeAsState()
        val ilv = level.value?.second?.level
        Surface(color = MaterialTheme.colors.background) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(text = "Game: $ilv")
                Text(text = "Account: ${accountName.value?.displayName}")
                PlayGameButton {
                    val updated = viewModel.levelUp()
                    Timber.d("Updated: $updated")
                }
                SignInButton {
                    lifecycleScope.launch {
                        signInExplicitly()?.run {
                            viewModel.onLogin(this)
                        }
                    }
                }
                SignOutButton {
                    lifecycleScope.launch {
                        signOut()
                        viewModel.onLogout()
                    }
                }
                RevokeButton {
                    lifecycleScope.launch {
                        revokeAccess()
                        signOut()
                        viewModel.onLogout()
                    }
                }
                LoadButton {
//                    showSavedGamesUI()
                    val lastsnapshot = latestSnapshotsClient()
                    if (lastsnapshot == null) {
                        Toast.makeText(this@MainActivity, "Please login first.", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Loading default game",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                        lifecycleScope.launch {
                            viewModel.loadDefaultGame(lastsnapshot)
                        }
                    }
                }
                SaveButton {
                    checkLogin()?.run {
                        lifecycleScope.launch {
                            viewModel.saveSnapshot(this@run)
                            withContext(Dispatchers.Main) {
                                showToast("Saved")
                            }
                        }
                    }
                }
                DebugText(viewModel = viewModel)
            }
        }
    }

    private fun checkLogin(): SnapshotsClient? {
        val latestSnapshotsClient = latestSnapshotsClient()
        if (latestSnapshotsClient == null) {
            Toast.makeText(this@MainActivity, "Please login first", Toast.LENGTH_SHORT)
                .show()
            return null
        }
        return latestSnapshotsClient
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
fun PlayGameButton(onclick: () -> Unit) {
    Button(onClick = {
        onclick()
    }) {
        Text("Play game")
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
fun SignOutButton(onclick: () -> Unit) {
    Button(onClick = {
        onclick()
    }) {
        Text("Sign out")
    }
}

@Composable
fun RevokeButton(onclick: () -> Unit) {
    Button(onClick = {
        onclick()
    }) {
        Text("Revoke")
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
        Text("Load")
    }
}

@Composable
fun DebugText(viewModel: MainViewModel) {
    val t = viewModel.debugMsg.observeAsState()
    Text(t.value ?: "")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    GameTestTheme {
        Surface(color = MaterialTheme.colors.background) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PlayGameButton {}
                SignInButton {}
                LoadButton {}
                SaveButton {}
            }
        }
    }
}

@MainThread
fun Context.showToast(content: String) {
    Toast.makeText(this, content, Toast.LENGTH_SHORT).show()
}