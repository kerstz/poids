package org.tally.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.tally.data.di.OffUserAgent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    /**
     * User-Agent exigé par Open Food Facts : nom + version. Pas d'URL de dépôt tant que le
     * projet n'est pas publié, et jamais d'adresse e-mail personnelle.
     */
    @Provides
    @OffUserAgent
    fun provideOffUserAgent(@ApplicationContext context: Context): String {
        val version = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "dev"
        return "Tally/$version (Android)"
    }
}
