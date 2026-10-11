package org.tally.coreui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.tally.coreui.component.EmptyState
import org.tally.coreui.component.ErrorBanner
import org.tally.coreui.component.FoodRow
import org.tally.coreui.component.MacroBar
import org.tally.coreui.component.MacroLegend
import org.tally.coreui.component.NumberField
import org.tally.coreui.component.ProgressGauge
import org.tally.coreui.component.SectionHeader
import org.tally.coreui.component.TallyCard
import org.tally.coreui.icon.TallyIcons
import org.tally.coreui.theme.TallyTheme

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h914dp-xxhdpi")
class ComponentGalleryTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun surfaces() = rule.captureVariants("gallery_surfaces") {
        Page {
            SectionHeader(title = "Petit-déjeuner", trailing = "420 kcal")
            TallyCard {
                Text("Carte", color = TallyTheme.colors.onSurface)
                ProgressGauge(value = 1400f, target = 2000f, label = "1400 sur 2000 kcal")
                MacroBar(
                    proteinG = 82.0,
                    carbsG = 190.0,
                    fatG = 54.0,
                    legend = MacroLegend("P 82 g", "G 190 g", "L 54 g"),
                )
            }
            FoodRow(name = "Flocons d'avoine", detail = "60 g", kcal = "228 kcal", secondary = "P 8 g · G 40 g · L 4 g", onDelete = {}, deleteLabel = "Supprimer")
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                listOf(TallyIcons.add, TallyIcons.delete, TallyIcons.chevronLeft, TallyIcons.chevronRight, TallyIcons.restaurant).forEach {
                    androidx.compose.material3.Icon(painterResource(it), contentDescription = null, tint = TallyTheme.colors.onSurface)
                }
            }
            FoodRow(name = "Pomme", detail = "1,0 portion", kcal = "78 kcal")
        }
    }

    @Test
    fun inputs() = rule.captureVariants("gallery_inputs") {
        Page {
            NumberField(value = "150,5", onValueChange = {}, label = "Quantité", unit = "g")
            NumberField(value = "abc", onValueChange = {}, label = "Quantité", unit = "g", error = "Valeur invalide")
        }
    }

    @Test
    fun states() = rule.captureVariants("gallery_states") {
        Page {
            EmptyState(
                icon = painterResource(TallyIcons.restaurant),
                title = "Rien de noté",
                body = "Ajoute ce que tu manges pour suivre ta journée.",
                actionLabel = "Ajouter",
                onAction = {},
            )
            ErrorBanner(message = "Open Food Facts est injoignable.", retryLabel = "Réessayer", onRetry = {})
        }
    }
}

@androidx.compose.runtime.Composable
internal fun Page(content: @androidx.compose.runtime.Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(TallyTheme.colors.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}
