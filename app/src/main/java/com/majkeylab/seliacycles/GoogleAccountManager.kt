package com.majkeylab.seliacycles

import android.app.Activity
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

data class CloudAccount(val uid: String, val displayName: String?, val email: String?)

class GoogleAccountManager(private val activity: Activity) {
    private val credentialManager = CredentialManager.create(activity)

    val isConfigured: Boolean
        get() = FirebaseApp.getApps(activity).isNotEmpty() && activity.getString(R.string.google_web_client_id).isNotBlank()

    fun currentAccount(): CloudAccount? {
        if (!isConfigured) return null
        return FirebaseAuth.getInstance().currentUser?.let { CloudAccount(it.uid, it.displayName, it.email) }
    }

    suspend fun signIn(): CloudAccount {
        check(isConfigured) { "Google sync is not configured" }
        val googleOption = GetGoogleIdOption.Builder()
            .setServerClientId(activity.getString(R.string.google_web_client_id))
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()
        val credential = credentialManager.getCredential(
            context = activity,
            request = GetCredentialRequest.Builder().addCredentialOption(googleOption).build(),
        ).credential
        check(credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) { "Unexpected Google credential" }
        val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val result = FirebaseAuth.getInstance()
            .signInWithCredential(GoogleAuthProvider.getCredential(token, null))
            .awaitResult()
        val user = requireNotNull(result.user) { "Google sign-in returned no user" }
        return CloudAccount(user.uid, user.displayName, user.email)
    }

    suspend fun signOut() {
        if (!isConfigured) return
        FirebaseAuth.getInstance().signOut()
        runCatching { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
    }
}
