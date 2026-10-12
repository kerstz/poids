// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.domain.data

import java.io.InputStream
import java.io.OutputStream

/** Sauvegarde / restauration ZIP JSONL versionnée (docs/AUDIT.md §3.4, jalon 7). */
interface BackupRepository {
    /** Écrit l'archive dans [out] (non fermé). Lignes par table dans le résumé. */
    suspend fun export(out: OutputStream): BackupSummary

    /** Valide puis remplace toutes les données ; en cas de rejet, rien n'est modifié. */
    suspend fun restore(input: InputStream): RestoreResult

    /** Revient à l'état précédant la dernière restauration réussie (sauvegarde de sécurité). */
    suspend fun undoLastRestore(): RestoreResult
}

data class BackupSummary(val rows: Map<String, Int>)

sealed interface RestoreResult {
    data class Success(val rows: Map<String, Int>) : RestoreResult
    data class Rejected(val error: RestoreError, val table: String? = null, val line: Int? = null) : RestoreResult
}

enum class RestoreError {
    NOT_A_BACKUP,
    FUTURE_FORMAT,
    CHECKSUM,
    COUNT_MISMATCH,
    INVALID_ROW,
    MISSING_BUILTIN,
    TOO_LARGE,
    NOTHING_TO_UNDO,
    IO,
}

/** Exports CSV interopérables (RFC 4180, UTF-8, unités SI) ; retournent le nombre de lignes de données. */
interface CsvExporter {
    suspend fun exportWeights(out: OutputStream): Int
    suspend fun exportJournal(out: OutputStream): Int
}
