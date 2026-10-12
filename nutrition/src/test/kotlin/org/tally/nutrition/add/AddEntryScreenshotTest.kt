// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.nutrition.add

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.tally.domain.model.BaseUnit
import org.tally.domain.nutrition.Food
import org.tally.domain.nutrition.FoodPortion
import org.tally.domain.nutrition.FoodWithPortions
import org.tally.domain.nutrition.Macros
import org.tally.domain.nutrition.Recipe
import org.tally.domain.nutrition.RemoteFood

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "fr-w411dp-h914dp-xxhdpi")
class AddEntryScreenshotTest {
    @get:Rule val rule = createComposeRule()

    private val oats = Food(1, "Flocons d'avoine", "Quaker", per100 = Macros(380.0, 13.0, 66.0, 7.0))
    private val foods = listOf(
        oats,
        Food(2, "Lait demi-écrémé", null, baseUnit = BaseUnit.ML, per100 = Macros(46.0, 3.4, 4.8, 1.6)),
        Food(3, "Pomme", null, per100 = Macros(52.0, 0.3, 14.0, 0.2)),
    )

    private fun screenshot(
        name: String,
        foods: List<Food> = this.foods,
        recipes: List<Recipe> = emptyList(),
        query: String = "",
        tab: Int = 0,
        online: OnlineState = OnlineState.Idle,
        selectedFood: FoodWithPortions? = null,
    ) = rule.captureVariants(name) {
        AddEntryContent(
            foods = foods, recipes = recipes, query = query, tab = tab, online = online,
            selectedFood = selectedFood, recipeToLog = null,
            onQueryChange = {}, onTabChange = {}, onSubmitOnline = {}, onSelectFood = {},
            onChooseRemote = {}, onSelectRecipe = {}, onNewFood = {}, onEditFood = {}, onNewRecipe = {},
            onDismissFood = {}, onDismissRecipe = {}, onAddFood = { _, _, _, _ -> }, onAddRecipe = { _, _ -> },
        )
    }

    @Test fun foods() = screenshot("add_foods")

    @Test fun foodsEmpty() = screenshot("add_foods_empty", foods = emptyList())

    @Test fun noMatch() = screenshot("add_no_match", foods = emptyList(), query = "xyz")

    @Test fun onlineResults() = screenshot(
        "add_online_results", tab = 2, query = "yaourt",
        online = OnlineState.Results(
            listOf(
                RemoteFood("1", "Yaourt nature", "Marque Repère", Macros(59.0, 4.0, 4.7, 3.0), 125.0, null),
                RemoteFood("2", "Yaourt grec", null, Macros(97.0, 9.0, 4.0, 5.0), null, null),
            ),
        ),
    )

    @Test fun onlineUnavailable() = screenshot("add_online_unavailable", tab = 2, query = "yaourt", online = OnlineState.Unavailable)

    @Test fun quantitySheet() = screenshot(
        "add_quantity_sheet",
        selectedFood = FoodWithPortions(oats, listOf(FoodPortion(1, "bol", 40.0, isDefault = true))),
    )
}
