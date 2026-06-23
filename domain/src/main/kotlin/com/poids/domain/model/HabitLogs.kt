package com.poids.domain.model

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/** Nuit de sommeil. */
data class SleepLog(
    val id: Long = 0L,
    val date: LocalDate,
    val bedTime: LocalDateTime,
    val wakeTime: LocalDateTime,
    val quality: Int? = null,
    val note: String? = null,
) {
    val durationMin: Int get() = Duration.between(bedTime, wakeTime).toMinutes().toInt()
}

/** Consommation de substance. `label` = nom libre (suivi de conso) si renseigné. */
data class SubstanceLog(
    val id: Long = 0L,
    val dateTime: LocalDateTime,
    val kind: SubstanceKind,
    val amount: Double,
    val unit: String,
    val note: String? = null,
    val label: String? = null,
)
