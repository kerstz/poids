package org.tally.app.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable
import org.tally.activity.ActivityRoute
import org.tally.app.R
import org.tally.body.BodyRoute
import org.tally.nutrition.NutritionRoute

@Serializable
data object SettingsRoute

enum class TopLevelDestination(
    val route: Any,
    val icon: ImageVector,
    @StringRes val labelRes: Int,
) {
    JOURNAL(NutritionRoute, Icons.Filled.DateRange, R.string.nav_journal),
    BODY(BodyRoute, Icons.Filled.Person, R.string.nav_body),
    ACTIVITY(ActivityRoute, Icons.Filled.LocationOn, R.string.nav_activity),
    SETTINGS(SettingsRoute, Icons.Filled.Settings, R.string.nav_settings),
}
