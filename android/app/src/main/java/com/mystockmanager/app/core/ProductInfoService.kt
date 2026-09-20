package com.mystockmanager.app.core

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class OpenFoodFactsResponse(
    val product: OpenFoodFactsProduct? = null,
    val status: Int? = null
)

@Serializable
data class OpenFoodFactsProduct(
    val product_name: String? = null,
    val product_name_fr: String? = null,
    val brands: String? = null,
    val image_url: String? = null
)

@Singleton
class ProductInfoService @Inject constructor() {
    private val client = HttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getProductName(barcode: String): String? {
        return try {
            val response: String = client.get("https://world.openfoodfacts.org/api/v0/product/$barcode.json").body()
            val parsed = json.decodeFromString<OpenFoodFactsResponse>(response)
            if (parsed.status == 1) {
                parsed.product?.let {
                    it.product_name_fr ?: it.product_name
                }
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
