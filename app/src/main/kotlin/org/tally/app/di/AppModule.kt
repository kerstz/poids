package org.tally.app.di

import android.content.Context
import android.os.Build
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.tally.data.backup.AppVersion
import org.tally.data.di.OffUserAgent
import org.tally.data.remote.offUserAgent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    private const val PROJECT_URL = "https://github.com/kerstz/poids"

    /** User-Agent exigé par Open Food Facts : nom + version + URL du dépôt (jamais d'e-mail personnel). */
    @Provides
    fun provideAppVersion(@ApplicationContext context: Context): AppVersion {
        val info = runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull()
        val code = when {
            info == null -> 0L
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> info.longVersionCode
            else -> @Suppress("DEPRECATION") info.versionCode.toLong()
        }
        return AppVersion(info?.versionName ?: "dev", code)
    }

    @Provides
    @OffUserAgent
    fun provideOffUserAgent(@ApplicationContext context: Context): String {
        val version = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "dev"
        return offUserAgent("Tally", version, PROJECT_URL)
    }
}
