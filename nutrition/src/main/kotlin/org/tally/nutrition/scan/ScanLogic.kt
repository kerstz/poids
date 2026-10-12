package org.tally.nutrition.scan

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import org.tally.core.barcode.Gtin
import zxingcpp.BarcodeReader

/** Décode une trame ; renvoie le texte du premier code lu, ou null. */
internal fun interface BarcodeDecoder {
    fun decode(image: ImageProxy): String?
}

/** zxing-cpp (Apache-2.0, JNI). Formats retail seulement : UPC-E exclu, `Gtin.isValid` ne le comprend pas. */
internal class ZxingCppDecoder : BarcodeDecoder {
    private val reader = BarcodeReader(
        BarcodeReader.Options(
            formats = setOf(BarcodeReader.Format.EAN_13, BarcodeReader.Format.EAN_8, BarcodeReader.Format.UPC_A),
            tryRotate = true,
            tryInvert = false,
        ),
    )

    override fun decode(image: ImageProxy): String? = reader.read(image).firstOrNull()?.text
}

/**
 * Anti-faux-positif et anti-doublon : un code n'est livré qu'une fois, lu à l'identique sur
 * [required] trames consécutives et valide GTIN (clé de contrôle).
 */
internal class ScanStabilizer(private val required: Int = 2) {
    private var last: String? = null
    private var streak = 0
    private var delivered = false

    /** Retourne le code à livrer, ou null (pas encore stable, invalide ou déjà livré). */
    fun offer(text: String?): String? {
        if (delivered) return null
        if (text == null || !Gtin.isValid(text)) {
            last = null
            streak = 0
            return null
        }
        streak = if (text == last) streak + 1 else 1
        last = text
        if (streak < required) return null
        delivered = true
        return text
    }
}

internal class BarcodeAnalyzer(
    private val decoder: BarcodeDecoder,
    private val stabilizer: ScanStabilizer,
    private val onCode: (String) -> Unit,
) : ImageAnalysis.Analyzer {
    override fun analyze(image: ImageProxy) {
        try {
            val text = runCatching { decoder.decode(image) }.getOrNull()
            stabilizer.offer(text)?.let(onCode)
        } finally {
            image.close()
        }
    }
}
