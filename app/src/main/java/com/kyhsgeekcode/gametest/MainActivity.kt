package com.kyhsgeekcode.gametest

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.kyhsgeekcode.gametest.ui.theme.GameTestTheme
import kotlinx.coroutines.CompletableDeferred
import timber.log.Timber
import timber.log.Timber.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.plant(DebugTree())

        setContent {
            GameTestTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    Greeting {
                        signInExplicitly()
                    }
                }
            }
        }
    }

    init {
        lifecycleScope.launchWhenResumed {
            Timber.i("Signing in")
            val account = signInSilently()
            if (account != null) {
                val name = account.displayName
                Timber.i("Sign in success $name ${account.email} ${account.id}")
            } else {
                Timber.e("Sign in failed")
            }
        }
    }

    val deferred = CompletableDeferred<GoogleSignInAccount?>()
    val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
            Timber.d("Result:$r")
            if (r.resultCode == Activity.RESULT_OK) {
                // There are no request codes
                val data: Intent? = r.data
                val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
                if (result.isSuccess) {
                    // The signed in account is stored in the result.
                    val signedInAccount = result.signInAccount
                    deferred.complete(signedInAccount)
                } else {
                    var message = result.status.statusMessage
                    if (message == null || message.isEmpty()) {
                        message = "Failed sign in"
                    }
                    AlertDialog.Builder(this).setMessage(message)
                        .setNeutralButton(android.R.string.ok, null).show()
                    deferred.complete(null)
                }
            } else {
                Timber.e("Sign in not OK code:${r.resultCode}")
                deferred.complete(null)
            }
        }

    private suspend fun signInSilently(): GoogleSignInAccount? {
        val signInOptions = GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (GoogleSignIn.hasPermissions(account, *signInOptions.scopeArray)) {
            // Already signed in.
            // The signed in account is stored in the 'account' variable.
            Timber.i("Already signed in")
            return account
        } else {
            Timber.i("Not signed in")
            // Haven't been signed-in before. Try the silent sign-in first.
            val signInClient = GoogleSignIn.getClient(this, signInOptions)
            signInClient
                .silentSignIn()
                .addOnCompleteListener(
                    this
                ) { task ->
                    if (task.isSuccessful) {
                        // The signed in account is stored in the task's result.
                        Timber.i("Success signed in")
                        val signedInAccount = task.result
                        deferred.complete(signedInAccount)
                    } else {
                        // Player will need to sign-in explicitly using via UI.
                        // See [sign-in best practices](http://developers.google.com/games/services/checklist) for guidance on how and when to implement Interactive Sign-in,
                        // and [Performing Interactive Sign-in](http://developers.google.com/games/services/android/signin#performing_interactive_sign-in) for details on how to implement
                        // Interactive Sign-in.
                        Timber.i("Explicitly signing in")
                        signInExplicitly()
                    }
                }
            return deferred.await()
        }
    }

    private fun signInExplicitly() {
        val signInClient = GoogleSignIn.getClient(
            this,
            GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN
        )
        val intent = signInClient.signInIntent
        resultLauncher.launch(intent)
        Timber.i("Explicitly signing in launched")

    }
}

@Composable
fun Greeting(onclick: () -> Unit) {
    Button(onClick = {
        onclick()
    }) {
        Text("Sign in")
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    GameTestTheme {
        Greeting {}
    }
}