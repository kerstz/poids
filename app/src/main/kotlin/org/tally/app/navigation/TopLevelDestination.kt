// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.app.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import kotlinx.serialization.Serializable
import org.tally.activity.ActivityRoute
import org.tally.app.R
import org.tally.body.BodyRoute
import org.tally.coreui.icon.TallyIcons
import org.tally.nutrition.NutritionRoute

@Serializable
data object SettingsRoute

@Serializable
data object PrivacyRoute

/**
 * Onglet Activité masqué tant que l'écran n'est qu'un placeholder.
 * À réactiver (true) à la livraison de S3 (docs/SPORT-ARCHI.md). La route reste enregistrée dans le NavHost.
 */
const val SPORT_ENABLED = false

enum class TopLevelDestination(
    val route: Any,
    @DrawableRes val icon: Int,
    @StringRes val labelRes: Int,
) {
    JOURNAL(NutritionRoute, TallyIcons.navJournal, R.string.nav_journal),
    BODY(BodyRoute, TallyIcons.navBody, R.string.nav_body),
    ACTIVITY(ActivityRoute, TallyIcons.navActivity, R.string.nav_activity),
    SETTINGS(SettingsRoute, TallyIcons.navSettings, R.string.nav_settings),
    ;

    companion object {
        /** Destinations affichées dans la barre de navigation. */
        val visible: List<TopLevelDestination>
            get() = entries.filter { it != ACTIVITY || SPORT_ENABLED }
    }
}
