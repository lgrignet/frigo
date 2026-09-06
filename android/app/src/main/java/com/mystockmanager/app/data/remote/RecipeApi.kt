package com.mystockmanager.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
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

@Serializable
data class RecipeIngredientDto(
    val name: String,
    val quantity: Double? = null,
    val unit: String? = null
)

@Serializable
data class RecipeDto(
    val id: String,
    val title: String,
    val cuisineType: String,
    val servings: Int? = null,
    val ingredients: List<RecipeIngredientDto>,
    val steps: List<String>,
    val imageEmoji: String? = null
)

@Serializable
data class RecipeSearchResponse(
    val recipes: List<RecipeDto>,
    val degraded: Boolean
)

/** Erreur renvoyée par l'API recettes (statut HTTP non 2xx). */
class RecipeApiException(message: String, val httpStatus: Int) : Exception(message)

// ApiErrorBody est définie dans AccountApi.kt (même package data.remote), réutilisée telle quelle.

@Serializable
private data class RecipeSearchRequest(
    val ingredients: List<String>,
    val priorityIngredients: List<String>,
    val cuisineTypes: List<String>,
    val language: String,
    val count: Int = 5
)

/**
 * Client REST des recettes générées par IA (api.noshi.be/recipes) — même
 * pattern que AccountApi.kt (Ktor + ContentNegotiation, Bearer device_token).
 */
@Singleton
class RecipeApi @Inject constructor() {

    private val baseUrl = "https://api.noshi.be"

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun search(
        deviceToken: String,
        ingredients: List<String>,
        priorityIngredients: List<String>,
        cuisineTypes: List<String>,
        language: String,
        count: Int = 5
    ): RecipeSearchResponse = post(
        "/recipes/search",
        RecipeSearchRequest(ingredients, priorityIngredients, cuisineTypes, language, count),
        bearerToken = deviceToken
    )

    suspend fun choose(deviceToken: String, recipeId: String): OkResponse {
        val response: HttpResponse = client.post("$baseUrl/recipes/$recipeId/choose") {
            header(HttpHeaders.Authorization, "Bearer $deviceToken")
        }
        if (response.status.isSuccess()) return response.body()
        throw errorFrom(response)
    }

    suspend fun getRecipe(deviceToken: String, recipeId: String, language: String): RecipeDto {
        val response: HttpResponse = client.get("$baseUrl/recipes/$recipeId") {
            header(HttpHeaders.Authorization, "Bearer $deviceToken")
            parameter("language", language)
        }
        if (response.status.isSuccess()) return response.body()
        throw errorFrom(response)
    }

    private suspend inline fun <reified TReq, reified TRes> post(path: String, body: TReq, bearerToken: String? = null): TRes {
        val response: HttpResponse = client.post("$baseUrl$path") {
            contentType(ContentType.Application.Json)
            setBody(body)
            if (bearerToken != null) header(HttpHeaders.Authorization, "Bearer $bearerToken")
        }
        if (response.status.isSuccess()) return response.body()
        throw errorFrom(response)
    }

    private suspend fun errorFrom(response: HttpResponse): RecipeApiException {
        val message = try {
            response.body<ApiErrorBody>().error
        } catch (e: Exception) {
            null
        }
        return RecipeApiException(message ?: "HTTP_${response.status.value}", response.status.value)
    }
}
