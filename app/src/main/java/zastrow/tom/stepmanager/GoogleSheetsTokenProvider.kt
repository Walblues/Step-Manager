package zastrow.tom.stepmanager

import android.content.Context
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.tasks.await

class GoogleSheetsTokenProvider(
    private val context: Context
) {
    suspend fun getAccessToken(): String? {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(
                listOf(
                    Scope("https://www.googleapis.com/auth/spreadsheets")
                )
            )
            .build()

        val result = Identity
            .getAuthorizationClient(context)
            .authorize(request)
            .await()

        // A resolution means user interaction is required.
        if (result.hasResolution()) {
            return null
        }

        return result.accessToken
    }
}
