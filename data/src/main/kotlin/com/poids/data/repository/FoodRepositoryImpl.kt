package com.poids.data.repository

import com.poids.data.local.dao.FoodItemDao
import com.poids.data.remote.OpenFoodFactsApi
import com.poids.data.remote.toEntity
import com.poids.domain.model.FoodItem
import com.poids.domain.repository.FoodLookupResult
import com.poids.domain.repository.FoodRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FoodRepositoryImpl @Inject constructor(
    private val dao: FoodItemDao,
    private val api: OpenFoodFactsApi,
) : FoodRepository {

    override suspend fun lookupByBarcode(barcode: String): FoodLookupResult {
        // 1. Cache local (offline, rapide)
        dao.getByBarcode(barcode)?.let { return FoodLookupResult.Found(it.toDomain()) }

        // 2. API Open Food Facts, puis write-through en cache
        return try {
            val response = api.fetchProduct(barcode)
            val product = response.product
            if (response.status != 1 || product == null) {
                FoodLookupResult.NotFound
            } else {
                val entity = product.toEntity(barcode)
                val id = dao.upsert(entity)
                FoodLookupResult.Found(entity.copy(id = id).toDomain())
            }
        } catch (e: Exception) {
            FoodLookupResult.Error(e.message ?: "Erreur réseau")
        }
    }

    override suspend fun search(query: String, limit: Int): List<FoodItem> =
        dao.search(query.trim(), limit).map { it.toDomain() }

    override suspend fun saveManual(food: FoodItem): Long = dao.upsert(food.toEntity())

    override fun observeById(id: Long): Flow<FoodItem?> =
        dao.observeById(id).map { it?.toDomain() }
}
