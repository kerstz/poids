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

enum class TopLevelDestination(
    val route: Any,
    @DrawableRes val icon: Int,
    @StringRes val labelRes: Int,
) {
    JOURNAL(NutritionRoute, TallyIcons.navJournal, R.string.nav_journal),
    BODY(BodyRoute, TallyIcons.navBody, R.string.nav_body),
    ACTIVITY(ActivityRoute, TallyIcons.navActivity, R.string.nav_activity),
    SETTINGS(SettingsRoute, TallyIcons.navSettings, R.string.nav_settings),
}
