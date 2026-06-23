package com.poids.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import javax.inject.Inject

/** Client minimal pour l'API Open Food Facts (lecture produit par code-barres). */
class OpenFoodFactsApi @Inject constructor(
    private val client: HttpClient,
) {
    suspend fun fetchProduct(barcode: String): OffResponse =
        client.get("$BASE_URL/api/v2/product/$barcode.json") {
            // On limite les champs récupérés (bande passante + parsing).
            parameter(
                "fields",
                "code,product_name,brands,nutriscore_grade,nova_group,nutriments",
            )
        }.body()

    private companion object {
        const val BASE_URL = "https://world.openfoodfacts.org"
    }
}
