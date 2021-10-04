package com.kyhsgeekcode.gametest

import android.app.Activity
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.drive.Drive
import kotlinx.coroutines.CompletableDeferred
import timber.log.Timber


// sign in implicitly: Wait until finish
// sign in explicitly: Wait until finish
// sign out explicitly: Wait until finish.

open class GoogleSignInActivity : ComponentActivity() {
    private val signInDeferred = CompletableDeferred<GoogleSignInAccount?>()
    private val signInOptions: GoogleSignInOptions =
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN) // Add the APPFOLDER scope for Snapshot support.
            .requestScopes(Drive.SCOPE_APPFOLDER)
            .requestProfile()
            .build()

    private val mGoogleSignInClient by lazy {
        GoogleSignIn.getClient(applicationContext, signInOptions)
    }


    private val signInRequestLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
            Timber.d("Result:$r")
            if (r.resultCode == Activity.RESULT_OK) {
                // There are no request codes
                val data: Intent? = r.data
                val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
                if (result.isSuccess) {
                    // The signed in account is stored in the result.
                    val signedInAccount = result.signInAccount
                    signInDeferred.complete(signedInAccount)
                } else {
                    var message = result.status.statusMessage
                    if (message == null || message.isEmpty()) {
                        message = "Failed sign in"
                    }
                    AlertDialog.Builder(this).setMessage(message)
                        .setNeutralButton(android.R.string.ok, null).show()
                    signInDeferred.complete(null)
                }
            } else {
                Timber.e("Sign in not OK code:${r.resultCode}")
                signInDeferred.complete(null)
            }
        }

    suspend fun signInExplicitly(): GoogleSignInAccount? {
        val intent = mGoogleSignInClient.signInIntent

        signInRequestLauncher.launch(intent)
        Timber.i("Explicitly signing in launched")
        return signInDeferred.await()
    }


    suspend fun signInSilentlyOrExplicitly(): GoogleSignInAccount? {
        return signInSilently() ?: signInExplicitly()
    }

    suspend fun signInSilently(): GoogleSignInAccount? {
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (GoogleSignIn.hasPermissions(account, *signInOptions.scopeArray)) {
            // Already signed in.
            // The signed in account is stored in the 'account' variable.
            Timber.i("Already signed in")
            return account
        } else {
            Timber.i("Not signed in")
            // Haven't been signed-in before. Try the silent sign-in first.
            mGoogleSignInClient
                .silentSignIn()
                .addOnCompleteListener(
                    this
                ) { task ->
                    if (task.isSuccessful) {
                        // The signed in account is stored in the task's result.
                        Timber.i("Success signed in default")
                        val signedInAccount = task.result
                        signInDeferred.complete(signedInAccount)
                    } else {
                        // Player will need to sign-in explicitly using via UI.
                        // See [sign-in best practices](http://developers.google.com/games/services/checklist) for guidance on how and when to implement Interactive Sign-in,
                        // and [Performing Interactive Sign-in](http://developers.google.com/games/services/android/signin#performing_interactive_sign-in) for details on how to implement
                        // Interactive Sign-in.
                        Timber.i("Explicitly signing in")
                        signInDeferred.complete(null)
                    }
                }

            return signInDeferred.await()
        }
    }

//    fun autoGoogleSignIn() {
//        lifecycleScope.launchWhenResumed {
//            Timber.i("Signing in")
//            val account = signInSilently()
//            if (account != null) {
//                val name = account.displayName
//                Timber.i("Sign in success $name ${account.email} ${account.id}")
//            } else {
//                Timber.e("Sign in failed")
//            }
//        }
//    }

    suspend fun signOut() {
        val d = CompletableDeferred(Unit)
        mGoogleSignInClient.signOut().addOnCompleteListener {
            d.complete(Unit)
        }.addOnFailureListener {
            d.cancel()
        }
        d.await()
    }

    suspend fun revokeAccess() {
        val d = CompletableDeferred(Unit)
        mGoogleSignInClient.revokeAccess()
            .addOnCompleteListener(this) {
                d.complete(Unit)
            }.addOnFailureListener {
                d.cancel()
            }
        d.await()
    }
}

