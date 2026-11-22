package zastrow.tom.stepmanager

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope

class GoogleSheetsAuth(
    private val activity: Activity,
    private val authorizationLauncher: ActivityResultLauncher<IntentSenderRequest>,
    private val onTokenReceived: (String) -> Unit
) {
    companion object {
        private const val TAG = "GoogleSheetsAuth"
        private const val SHEETS_SCOPE = "https://www.googleapis.com/auth/spreadsheets"
    }

    // Request permission to access Google Sheets
    fun authorize() {
        Log.d(TAG, "Starting Google authorization")

        val requestedScopes = listOf(
            Scope(SHEETS_SCOPE)
        )

        val request = AuthorizationRequest.builder()
            .setRequestedScopes(requestedScopes)
            .build()

        Identity.getAuthorizationClient(activity)
            .authorize(request)
            .addOnSuccessListener { result ->

                Log.d(TAG, "Authorization request succeeded")
                Log.d(TAG, "Has resolution: ${result.hasResolution()}")
                Log.d(TAG, "Has access token: ${result.accessToken != null}")

                if (result.hasResolution()) {
                    // User needs to approve permissions
                    val pendingIntent = result.pendingIntent

                    if (pendingIntent == null) {
                        Log.e(TAG, "Resolution required, but pendingIntent is null")
                        return@addOnSuccessListener
                    }

                    Log.d(TAG, "Launching Google authorization screen")

                    val intentRequest =
                        IntentSenderRequest.Builder(
                            pendingIntent.intentSender
                        ).build()

                    authorizationLauncher.launch(intentRequest)

                } else {
                    // Permission was already granted
                    onAuthorizationSuccess(result)
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Initial authorization request failed", exception)
            }
    }

    fun onAuthorizationResult(data: Intent?) {
        Log.d(TAG, "Processing authorization result")

        if (data == null) {
            Log.e(TAG, "Authorization returned null Intent")
            return
        }

        try {
            val result = Identity.getAuthorizationClient(activity)
                .getAuthorizationResultFromIntent(data)

            Log.d(TAG, "Authorization result retrieved")
            Log.d(TAG, "Access token received: ${result.accessToken != null}")

            onAuthorizationSuccess(result)

        } catch (exception: Exception) {
            Log.e(TAG, "Could not retrieve authorization result", exception)
        }
    }

    private fun onAuthorizationSuccess(result: AuthorizationResult) {
        val accessToken = result.accessToken

        Log.d(TAG, "Google Sheets authorization successful")

        if (accessToken != null) {
            onTokenReceived(accessToken)
        }
    }
}
