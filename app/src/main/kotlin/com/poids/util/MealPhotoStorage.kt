package com.poids.util

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Copie une image choisie (content Uri) dans le stockage privé de l'app
 * (`filesDir/meal_photos`). Retourne le chemin absolu du fichier, stocké en base.
 * 100% local : la photo ne quitte jamais l'appareil.
 */
@Singleton
class MealPhotoStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dir: File
        get() = File(context.filesDir, "meal_photos").apply { mkdirs() }

    suspend fun savePhoto(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(dir, "meal_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            } ?: return@runCatching null
            file.absolutePath
        }.getOrNull()
    }
}
