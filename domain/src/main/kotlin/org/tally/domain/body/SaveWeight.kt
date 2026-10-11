package org.tally.domain.body

/** Écriture validée du poids (bornes [WeightValidation]) : saisie, import et restauration passent ici. */
class SaveWeight(private val repository: WeightRepository) {
    /** Retourne l'id, ou null si la valeur est hors bornes (rien n'est écrit). */
    suspend fun add(valueKg: Double, measuredAt: Long, zoneId: String, note: String? = null): Long? {
        if (!WeightValidation.isValid(valueKg)) return null
        return repository.add(valueKg, measuredAt, zoneId, note)
    }

    suspend fun update(entry: WeightEntry): Boolean {
        if (!WeightValidation.isValid(entry.valueKg)) return false
        repository.update(entry)
        return true
    }
}
