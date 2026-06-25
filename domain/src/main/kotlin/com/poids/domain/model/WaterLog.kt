package com.poids.domain.model

import java.time.LocalDateTime

/** Une prise d'eau (un verre, une bouteille…), pour le suivi d'hydratation. */
data class WaterLog(
    val id: Long = 0L,
    val dateTime: LocalDateTime,
    val amountMl: Int,
)
