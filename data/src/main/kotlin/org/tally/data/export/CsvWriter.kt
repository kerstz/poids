package org.tally.data.export

import java.io.Writer
import java.math.BigDecimal

/** Écrivain CSV RFC 4180 : séparateur `,`, fin de ligne CRLF, UTF-8 (l'encodage est celui du [Writer]). */
class CsvWriter(private val out: Writer) {
    fun row(vararg fields: String) {
        fields.forEachIndexed { i, f ->
            if (i > 0) out.write(",")
            out.write(escape(f))
        }
        out.write("\r\n")
    }

    companion object {
        private val FORMULA_PREFIXES = charArrayOf('=', '+', '-', '@', '\t', '\r')

        /** Champ texte libre : neutralise l'injection de formules (préfixe `'`). */
        fun text(value: String?): String {
            if (value.isNullOrEmpty()) return ""
            return if (value[0] in FORMULA_PREFIXES) "'$value" else value
        }

        /** Nombre brut, jamais en notation scientifique ; `null` = inconnu = champ vide. */
        fun number(value: Double?): String =
            value?.let { BigDecimal.valueOf(it).stripTrailingZeros().toPlainString() } ?: ""

        fun escape(field: String): String =
            if (field.any { it == ',' || it == '"' || it == '\r' || it == '\n' }) {
                "\"" + field.replace("\"", "\"\"") + "\""
            } else {
                field
            }
    }
}
