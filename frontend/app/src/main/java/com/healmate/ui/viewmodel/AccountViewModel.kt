package com.healmate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healmate.BuildConfig
import com.healmate.data.model.User
import com.healmate.data.remote.AccountApi
import com.healmate.data.remote.AccountRequestException
import com.healmate.data.remote.AccountSession
import com.healmate.data.remote.GoogleSignInException
import com.healmate.data.remote.HealthApi
import com.healmate.data.repository.RemoteHealthRepository
import com.healmate.data.repository.SessionGate
import com.healmate.data.remote.accountEndpoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AccountUiState(
    val user: User? = null,
    val busy: Boolean = false,
    val error: String? = null,
    val googleProfileRequired: Boolean = false
)

class AccountViewModel : ViewModel() {
    private val api = accountEndpoint(BuildConfig.HEALTHMATE_API_URL, BuildConfig.DEBUG)?.let { AccountApi(it) }
    private var session: AccountSession? = null
    private var pendingGoogleToken: String? = null
    private val _state = MutableStateFlow(AccountUiState())
    val state = _state.asStateFlow()
    val isConfigured = api != null
    val isGoogleConfigured = isConfigured && BuildConfig.HEALTHMATE_GOOGLE_CLIENT_ID.isNotBlank()

    fun login(email: String, password: String) = runRequest {
        session = it.login(email, password)
    }

    fun register(name: String, email: String, password: String, birthDate: String) = runRequest {
        session = it.register(name, email, password, birthDate)
    }

    internal fun loginWithGoogle(selectToken: suspend (String) -> String?) = runRequest(google = true) { api ->
        val token = selectToken(api.googleChallenge()) ?: return@runRequest
        try {
            session = api.loginWithGoogle(token)
        } catch (error: AccountRequestException) {
            if (error.status != 428) throw error
            pendingGoogleToken = token
        }
    }

    fun completeGoogleProfile(birthDate: String) = runRequest(google = true) { api ->
        val token = pendingGoogleToken ?: return@runRequest
        session = api.loginWithGoogle(token, birthDate)
        pendingGoogleToken = null
    }

    fun cancelGoogleProfile() {
        if (_state.value.busy) return
        pendingGoogleToken = null
        publish()
    }

    fun updateProfile(user: User, onSaved: () -> Unit) = runRequest(profile = true) { api ->
        val current = session ?: return@runRequest
        val saved = try {
            api.updateProfile(current, user)
        } catch (error: AccountRequestException) {
            if (error.status != 401) throw error
            val refreshed = api.refresh(current)
            session = refreshed
            api.updateProfile(refreshed, user)
        }
        session = session?.copy(user = saved)
        onSaved()
    }

    /** Repositorio de medicamentos, citas y salud de la cuenta, o null si no hay servidor configurado. */
    internal fun newHealthRepository(): RemoteHealthRepository? =
        api?.let { RemoteHealthRepository(HealthApi(it), sessionGate) }

    private val sessionGate = object : SessionGate {
        override suspend fun <T> withToken(block: suspend (String) -> T): T {
            val configuredApi = api ?: throw AccountRequestException(401)
            val current = session ?: throw AccountRequestException(401)
            return try {
                block(current.access)
            } catch (error: AccountRequestException) {
                if (error.status != 401) throw error
                // El token de acceso venció: se renueva una vez y se reintenta.
                val refreshed = configuredApi.refresh(session ?: throw error)
                session = refreshed
                block(refreshed.access)
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun logout() {
        if (_state.value.busy) return
        session = null
        _state.value = AccountUiState()
    }

    private fun publish(error: String? = null) {
        _state.value = AccountUiState(
            user = session?.user,
            error = error,
            googleProfileRequired = pendingGoogleToken != null
        )
    }

    private fun runRequest(
        profile: Boolean = false,
        google: Boolean = false,
        request: suspend (AccountApi) -> Unit
    ) {
        val configuredApi = api ?: return
        if (_state.value.busy) return
        _state.value = _state.value.copy(busy = true, error = null)
        viewModelScope.launch {
            try {
                request(configuredApi)
                publish()
            } catch (error: CancellationException) {
                throw error
            } catch (error: AccountRequestException) {
                if (profile && error.status == 401) session = null
                if (google && error.status in listOf(401, 409)) pendingGoogleToken = null
                publish(when (error.status) {
                    401 -> when {
                        profile -> "La sesión venció. Inicia sesión de nuevo."
                        google -> "La credencial de Google venció o no es válida. Vuelve a elegir tu cuenta."
                        else -> "Correo o contraseña incorrectos."
                    }
                    409 -> if (google) "Ese correo ya tiene una cuenta. Usa su método de acceso original."
                        else "Ese correo ya está registrado."
                    422 -> when {
                        profile -> "Revisa los datos del perfil."
                        google -> "Ingresa una fecha de nacimiento válida."
                        else -> "Revisa el correo y los datos de acceso."
                    }
                    503 -> if (google) "El acceso con Google no está disponible en este momento."
                        else "El servidor no pudo completar la solicitud."
                    else -> "El servidor no pudo completar la solicitud."
                })
            } catch (error: GoogleSignInException) {
                publish(error.message)
            } catch (_: Exception) {
                publish("No se pudo completar la conexión. Intenta de nuevo.")
            }
        }
    }

    override fun onCleared() {
        api?.close()
    }
}
