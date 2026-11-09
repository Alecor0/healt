package com.healmate.data.remote

import com.healmate.data.model.User
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.*
import java.net.URI

internal data class AccountSession(val access: String, val refresh: String, val user: User)
internal class AccountRequestException(val status: Int) : Exception()

internal fun accountEndpoint(rawUrl: String, debug: Boolean): String? {
    val url = runCatching { URI(rawUrl.trim()) }.getOrNull() ?: return null
    if (url.host == null || url.userInfo != null || url.query != null || url.fragment != null) {
        return null
    }
    val localDebug = debug && url.scheme == "http" &&
        url.host in setOf("10.0.2.2", "localhost", "127.0.0.1")
    return url.toString().trimEnd('/').takeIf { url.scheme == "https" || localDebug }
}

internal class AccountApi(
    private val baseUrl: String,
    private val client: HttpClient = HttpClient(Android) {
        followRedirects = false
        install(HttpTimeout) {
            // El plan gratuito de Render duerme la API; la primera petición puede tardar ~1 min.
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 30_000
        }
    }
) {
    suspend fun googleChallenge(): String =
        send(HttpMethod.Post, "/auth/google/desafio", body = buildJsonObject {})
            .getValue("nonce").jsonPrimitive.content

    suspend fun loginWithGoogle(idToken: String, birthDate: String? = null): AccountSession =
        send(HttpMethod.Post, "/auth/google", body = buildJsonObject {
            put("id_token", idToken)
            if (birthDate != null) put("fecha_nacimiento", birthDate.trim())
        }).toSession()

    suspend fun login(email: String, password: String): AccountSession =
        send(HttpMethod.Post, "/auth/login", body = buildJsonObject {
            put("email", email.trim())
            put("password", password)
        }).toSession()

    suspend fun register(name: String, email: String, password: String, birthDate: String): AccountSession =
        send(HttpMethod.Post, "/auth/registro", body = buildJsonObject {
            put("nombre", name.trim())
            put("email", email.trim())
            put("password", password)
            put("fecha_nacimiento", birthDate.trim())
        }).toSession()

    suspend fun refresh(session: AccountSession): AccountSession =
        send(HttpMethod.Post, "/auth/refresh", body = buildJsonObject {
            put("refresh_token", session.refresh)
        }).toSession()

    suspend fun updateProfile(session: AccountSession, user: User): User =
        send(HttpMethod.Put, "/usuarios/me", session.access, buildJsonObject {
            put("nombre", user.nombre.trim())
            put("fecha_nacimiento", user.fechaNacimiento)
            put("peso", user.peso)
            put("altura", user.altura)
            put("alergias", user.alergias)
            put("condiciones_cronicas", user.condicionesCronicas)
            put("contacto_emergencia", user.contactoEmergencia)
        }).toUser()

    fun close() = client.close()

    private suspend fun send(
        httpMethod: HttpMethod,
        path: String,
        token: String? = null,
        body: JsonObject
    ): JsonObject {
        val response = client.request(baseUrl + path) {
            method = httpMethod
            if (token != null) bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(body.toString())
        }
        if (!response.status.isSuccess()) throw AccountRequestException(response.status.value)
        return Json.parseToJsonElement(response.bodyAsText()).jsonObject
    }
}

private fun JsonObject.toSession() = AccountSession(
    access = getValue("access_token").jsonPrimitive.content,
    refresh = getValue("refresh_token").jsonPrimitive.content,
    user = getValue("usuario").jsonObject.toUser()
)

// El id remoto es un UUID y la app no lo usa: la sesión se identifica con los tokens.
private fun JsonObject.toUser() = User(
    nombre = getValue("nombre").jsonPrimitive.content,
    email = getValue("email").jsonPrimitive.content,
    fechaNacimiento = getValue("fecha_nacimiento").jsonPrimitive.contentOrNull.orEmpty(),
    peso = getValue("peso").jsonPrimitive.doubleOrNull,
    altura = getValue("altura").jsonPrimitive.intOrNull,
    alergias = getValue("alergias").jsonPrimitive.content,
    condicionesCronicas = getValue("condiciones_cronicas").jsonPrimitive.content,
    contactoEmergencia = getValue("contacto_emergencia").jsonPrimitive.content
)
