package org.tally.activity

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import org.tally.coreui.component.ScreenPlaceholder

@Serializable
data object ActivityRoute

fun NavGraphBuilder.activityGraph() {
    composable<ActivityRoute> { ActivityHomeScreen() }
}

@Composable
internal fun ActivityHomeScreen() {
    ScreenPlaceholder(title = stringResource(R.string.activity_title))
}
