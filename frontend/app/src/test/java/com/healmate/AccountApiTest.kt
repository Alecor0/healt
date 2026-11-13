package com.healmate

import com.healmate.data.remote.AccountApi
import com.healmate.data.remote.AccountRequestException
import com.healmate.data.remote.accountEndpoint
import com.healmate.data.util.isBirthDateFormat
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.*
import org.junit.Test

class AccountApiTest {
    private val profile = """
        {"id":"6f1c2a52-5d8e-4b8a-9a43-2f0d6f7c1b11","nombre":"Ana","email":"ana@example.com","fecha_nacimiento":"1992-05-14",
        "peso":null,"altura":null,"alergias":"","condiciones_cronicas":"","contacto_emergencia":""}
    """.trimIndent()

    @Test
    fun endpointRequiresHttpsExceptForDebugLoopback() {
        assertEquals("https://example.com/api", accountEndpoint("https://example.com/api/", false))
        assertNull(accountEndpoint("http://example.com", true))
        assertNull(accountEndpoint("http://10.0.2.2:8000", false))
        assertEquals("http://10.0.2.2:8000", accountEndpoint("http://10.0.2.2:8000", true))
        assertNull(accountEndpoint("https://username:password@example.com", false))
    }

    @Test
    fun birthDateRequiresAnIsoDateInput() {
        assertTrue(isBirthDateFormat(" 1992-05-14 "))
        assertFalse(isBirthDateFormat(""))
        assertFalse(isBirthDateFormat("14/05/1992"))
    }

    @Test
    fun accountAndProfileUseServerDataAndBearerToken() = runBlocking {
        val engine = MockEngine { request ->
            val body = request.body as OutgoingContent.ByteArrayContent
            val payload = Json.parseToJsonElement(body.bytes().decodeToString()).jsonObject
            when (request.url.encodedPath) {
                "/auth/login" -> {
                    assertEquals(HttpMethod.Post, request.method)
                    assertEquals("\"ana@example.com\"", payload.getValue("email").toString())
                    assertNull(request.headers[HttpHeaders.Authorization])
                    respond("""{"access_token":"access","refresh_token":"refresh","usuario":$profile}""")
                }
                "/auth/registro" -> {
                    assertEquals("\"1992-05-14\"", payload.getValue("fecha_nacimiento").toString())
                    respond("""{"access_token":"access","refresh_token":"refresh","usuario":$profile}""")
                }
                "/usuarios/me" -> {
                    assertEquals(HttpMethod.Put, request.method)
                    assertEquals("Bearer access", request.headers[HttpHeaders.Authorization])
                    assertEquals(JsonNull, payload.getValue("peso"))
                    respond(profile.replace("\"peso\":null", "\"peso\":\"64.50\""))
                }
                else -> error("Solicitud inesperada")
            }
        }
        val api = AccountApi("https://example.com", HttpClient(engine))
        try {
            val session = api.login(" ana@example.com ", "password")
            assertEquals("Ana", session.user.nombre)
            assertEquals("1992-05-14", session.user.fechaNacimiento)
            assertEquals(session, api.register("Ana", "ana@example.com", "password", "1992-05-14"))
            assertEquals(session.user.copy(peso = 64.5), api.updateProfile(session, session.user))
        } finally {
            api.close()
        }
    }

    @Test
    fun failedLoginDoesNotParseOrExposeTheResponseBody() = runBlocking {
        val api = AccountApi("https://example.com", HttpClient(MockEngine {
            respond("Error", HttpStatusCode.Unauthorized)
        }))
        try {
            val result = runCatching { api.login("ana@example.com", "incorrecta") }
            assertEquals(401, (result.exceptionOrNull() as AccountRequestException).status)
        } finally {
            api.close()
        }
    }

    @Test
    fun googleUsesServerChallengeAndRequestsBirthDateOnlyForNewAccounts() = runBlocking {
        val nonce = "9c4c8f70-ad5c-4edb-83be-46958d9d3654"
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Post, request.method)
            val body = request.body as OutgoingContent.ByteArrayContent
            val payload = Json.parseToJsonElement(body.bytes().decodeToString()).jsonObject
            when (request.url.encodedPath) {
                "/auth/google/desafio" -> {
                    assertTrue(payload.isEmpty())
                    respond("""{"nonce":"$nonce"}""", HttpStatusCode.Created)
                }
                "/auth/google" -> {
                    assertEquals("\"test-id-token\"", payload.getValue("id_token").toString())
                    if ("fecha_nacimiento" !in payload) {
                        respond("Perfil requerido", HttpStatusCode(428, "Precondition Required"))
                    } else {
                        assertEquals("\"1992-05-14\"", payload.getValue("fecha_nacimiento").toString())
                        respond("""{"access_token":"access","refresh_token":"refresh","usuario":$profile}""")
                    }
                }
                else -> error("Solicitud inesperada")
            }
        }
        val api = AccountApi("https://example.com", HttpClient(engine))
        try {
            assertEquals(nonce, api.googleChallenge())
            val failure = runCatching { api.loginWithGoogle("test-id-token") }.exceptionOrNull()
            assertEquals(428, (failure as AccountRequestException).status)
            val session = api.loginWithGoogle("test-id-token", "1992-05-14")
            assertEquals("1992-05-14", session.user.fechaNacimiento)
            assertEquals("access", session.access)
        } finally {
            api.close()
        }
    }
}
