package com.healmate.data.remote

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

internal class GoogleSignInException(message: String) : Exception(message)

internal suspend fun selectGoogleIdToken(
    activity: Activity,
    clientId: String,
    nonce: String
): String? {
    val option = GetSignInWithGoogleOption.Builder(clientId).setNonce(nonce).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
    val credential = try {
        CredentialManager.create(activity).getCredential(activity, request).credential
    } catch (_: GetCredentialCancellationException) {
        return null
    } catch (_: NoCredentialException) {
        throw GoogleSignInException("Agrega una cuenta de Google al dispositivo y vuelve a intentarlo.")
    } catch (_: GetCredentialException) {
        throw GoogleSignInException("No se pudo abrir el acceso con Google. Intenta de nuevo.")
    }

    if (credential is CustomCredential &&
        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    ) {
        try {
            return GoogleIdTokenCredential.createFrom(credential.data).idToken
        } catch (_: GoogleIdTokenParsingException) {
            throw GoogleSignInException("No se pudo leer la credencial de Google.")
        }
    }
    throw GoogleSignInException("El dispositivo no devolvió una credencial de Google.")
}
