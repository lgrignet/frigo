package com.mystockmanager.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Réponse commune aux 3 routes de compte (register/login/migrate) du service
 * api.noshi.be — cf. schema.sql (table comptes) et src/server.js.
 */
@Serializable
data class AccountAuthResponse(
    val token: String,
    @SerialName("compte_id") val compteId: String,
    @SerialName("email_verifie") val emailVerifie: Boolean,
    val guid: String? = null
)

/** Erreur renvoyée par l'API (statut HTTP non 2xx). [httpStatus] permet de distinguer 401/409/etc. */
class AccountApiException(message: String, val httpStatus: Int) : Exception(message)

@Serializable
private data class ApiErrorBody(val error: String? = null)

@Serializable
private data class RegisterRequest(
    val email: String,
    val password: String,
    val nom: String? = null,
    val prenom: String? = null,
    val guid: String? = null,
    @SerialName("device_name") val deviceName: String? = null
)

@Serializable
private data class LoginRequest(
    val email: String,
    val password: String,
    val guid: String? = null,
    @SerialName("device_name") val deviceName: String? = null
)

@Serializable
private data class RecoverPasswordRequest(
    val email: String,
    @SerialName("recovery_code") val recoveryCode: String,
    @SerialName("new_password") val newPassword: String,
    val guid: String? = null,
    @SerialName("device_name") val deviceName: String? = null
)

@Serializable
private data class VerifyEmailRequest(val email: String, val code: String)

@Serializable
data class VerifyEmailResponse(@SerialName("email_verifie") val emailVerifie: Boolean)

@Serializable
private data class ResendVerificationRequest(val email: String)

@Serializable
data class OkResponse(val ok: Boolean)

@Serializable
private data class ChangeHouseholdRequest(val guid: String)

@Serializable
data class ChangeHouseholdResponse(val guid: String)

@Serializable
private data class MigrateRequest(
    val email: String,
    val nom: String? = null,
    val prenom: String? = null,
    @SerialName("password_hash") val passwordHash: String,
    @SerialName("password_salt") val passwordSalt: String,
    @SerialName("recovery_code_hash") val recoveryCodeHash: String? = null,
    @SerialName("recovery_code_salt") val recoveryCodeSalt: String? = null,
    val guid: String? = null,
    @SerialName("device_name") val deviceName: String? = null
)

/**
 * Client REST du service de comptes (api.noshi.be) — inscription, connexion et
 * migration d'un compte local pré-existant. Voir API/src/server.js côté serveur.
 */
@Singleton
class AccountApi @Inject constructor() {

    private val baseUrl = "https://api.noshi.be"

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun register(
        email: String,
        password: String,
        nom: String,
        prenom: String,
        guid: String,
        deviceName: String
    ): AccountAuthResponse = post(
        "/account/register",
        RegisterRequest(email = email, password = password, nom = nom, prenom = prenom, guid = guid, deviceName = deviceName)
    )

    suspend fun login(
        email: String,
        password: String,
        guid: String?,
        deviceName: String
    ): AccountAuthResponse = post(
        "/account/login",
        LoginRequest(email = email, password = password, guid = guid, deviceName = deviceName)
    )

    /** Réinitialise le mot de passe via le code de récupération à 20 caractères (pas d'email). */
    suspend fun recoverPassword(
        email: String,
        recoveryCode: String,
        newPassword: String,
        guid: String?,
        deviceName: String
    ): AccountAuthResponse = post(
        "/account/password/recover",
        RecoverPasswordRequest(email = email, recoveryCode = recoveryCode, newPassword = newPassword, guid = guid, deviceName = deviceName)
    )

    suspend fun migrate(
        email: String,
        nom: String,
        prenom: String,
        passwordHash: String,
        passwordSalt: String,
        recoveryCodeHash: String?,
        recoveryCodeSalt: String?,
        guid: String,
        deviceName: String
    ): AccountAuthResponse = post(
        "/account/migrate",
        MigrateRequest(
            email = email, nom = nom, prenom = prenom,
            passwordHash = passwordHash, passwordSalt = passwordSalt,
            recoveryCodeHash = recoveryCodeHash, recoveryCodeSalt = recoveryCodeSalt,
            guid = guid, deviceName = deviceName
        )
    )

    /** Change le foyer (guid) rattaché au compte de cet appareil — Authorization: Bearer requis. */
    suspend fun changeHousehold(deviceToken: String, guid: String): ChangeHouseholdResponse =
        post("/account/household", ChangeHouseholdRequest(guid), bearerToken = deviceToken)

    suspend fun verifyEmail(email: String, code: String): VerifyEmailResponse =
        post("/account/verify-email", VerifyEmailRequest(email, code))

    suspend fun resendVerificationCode(email: String): OkResponse =
        post("/account/verify-email/resend", ResendVerificationRequest(email))

    /** Révoque le device_token courant côté serveur (Authorization: Bearer, pas de corps). */
    suspend fun logout(deviceToken: String): OkResponse {
        val response: HttpResponse = client.post("$baseUrl/account/logout") {
            header(HttpHeaders.Authorization, "Bearer $deviceToken")
        }
        if (response.status.isSuccess()) return response.body()
        val message = try {
            response.body<ApiErrorBody>().error
        } catch (e: Exception) {
            null
        }
        throw AccountApiException(message ?: "HTTP_${response.status.value}", response.status.value)
    }

    private suspend inline fun <reified TReq, reified TRes> post(path: String, body: TReq, bearerToken: String? = null): TRes {
        val response: HttpResponse = client.post("$baseUrl$path") {
            contentType(ContentType.Application.Json)
            setBody(body)
            if (bearerToken != null) header(HttpHeaders.Authorization, "Bearer $bearerToken")
        }
        if (response.status.isSuccess()) {
            return response.body()
        }
        val message = try {
            response.body<ApiErrorBody>().error
        } catch (e: Exception) {
            null
        }
        throw AccountApiException(message ?: "HTTP_${response.status.value}", response.status.value)
    }
}
