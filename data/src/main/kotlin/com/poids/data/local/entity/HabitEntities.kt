package com.poids.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.poids.domain.model.SubstanceKind
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Nuit de sommeil saisie manuellement (aucune API ne détecte le sommeil sans GMS).
 * `date` = jour du réveil.
 */
@Entity(tableName = "sleep_log", indices = [Index("date")])
data class SleepLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val date: LocalDate,
    val bedTime: LocalDateTime,
    val wakeTime: LocalDateTime,
    val durationMin: Int,
    val quality: Int?,
    val note: String?,
)

/**
 * Consommation de substance (alcool, tabac, caféine, ou substance libre).
 * `label` = nom libre quand `kind = OTHER` (suivi de conso / réduction des risques) ;
 * null pour les catégories prédéfinies (on affiche alors le libellé du `kind`).
 */
@Entity(tableName = "substance_log", indices = [Index("dateTime")])
data class SubstanceLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val dateTime: LocalDateTime,
    val kind: SubstanceKind,
    val amount: Double,
    val unit: String,
    val note: String?,
    val label: String? = null,
)
