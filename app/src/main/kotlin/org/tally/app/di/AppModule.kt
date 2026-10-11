package org.tally.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.tally.data.di.OffUserAgent
import org.tally.data.remote.offUserAgent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    private const val PROJECT_URL = "https://github.com/kerstz/poids"

    /** User-Agent exigé par Open Food Facts : nom + version + URL du dépôt (jamais d'e-mail personnel). */
    @Provides
    @OffUserAgent
    fun provideOffUserAgent(@ApplicationContext context: Context): String {
        val version = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "dev"
        return offUserAgent("Tally", version, PROJECT_URL)
    }
}
