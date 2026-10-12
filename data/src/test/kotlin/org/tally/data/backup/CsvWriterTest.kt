// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.data.backup

import java.io.StringWriter
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tally.data.export.CsvWriter

class CsvWriterTest {
    @Test
    fun escapesCommaQuoteAndNewlinesWithCrlf() {
        val sw = StringWriter()
        CsvWriter(sw).row("a,b", "x\"y", "l1\nl2", "plain", "")
        assertEquals("\"a,b\",\"x\"\"y\",\"l1\nl2\",plain,\r\n", sw.toString())
    }

    @Test
    fun neutralisesFormulaInjectionInTextFieldsOnly() {
        assertEquals("'=SUM(A1)", CsvWriter.text("=SUM(A1)"))
        assertEquals("'+1", CsvWriter.text("+1"))
        assertEquals("'-1", CsvWriter.text("-1"))
        assertEquals("'@cmd", CsvWriter.text("@cmd"))
        assertEquals("'\tx", CsvWriter.text("\tx"))
        assertEquals("pomme", CsvWriter.text("pomme"))
        assertEquals("", CsvWriter.text(null))
    }

    @Test
    fun numbersArePlainAndNullIsEmpty() {
        assertEquals("0.0000001", CsvWriter.number(1e-7))
        assertEquals("72.5", CsvWriter.number(72.5))
        assertEquals("100", CsvWriter.number(100.0))
        assertEquals("0.30000000000000004", CsvWriter.number(0.1 + 0.2))
        assertEquals("", CsvWriter.number(null))
    }
}
